package com.radixdlt.client.wallet

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.*
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.exceptions.Exceptions
import io.reactivex.observables.GroupedObservable
import io.reactivex.rxkotlin.Observables
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


class RadixWallet internal constructor(private val universe: RadixUniverse) {

    private val ledger: RadixLedger = universe.ledger

    private val cache = ConcurrentHashMap<RadixAddress, Observable<Collection<Consumable>>>()

    private fun getUnconsumedConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
        // TODO: use https://github.com/JakeWharton/RxReplayingShare to disconnect when unsubscribed
        return cache.computeIfAbsentSynchronisedFunction(address) {
            Observable.just<Collection<Consumable>>(emptySet()).concatWith(
                    Observables.combineLatest(
                            Observable.fromCallable { TransactionAtoms(address, Asset.XRD.id) },
                            ledger.getAllAtoms(address.getUID(), TransactionAtom::class.java)) { transactionAtoms: TransactionAtoms, atom: TransactionAtom ->
                        transactionAtoms.accept(atom).getUnconsumedConsumables()
                    }.flatMapMaybe { unconsumedMaybe: Maybe<Collection<Consumable>> ->
                        unconsumedMaybe
                    }

            ).debounce(1000, TimeUnit.MILLISECONDS)
                    .replay(1).autoConnect()
        }
    }

    fun getSubUnitBalance(address: RadixAddress, assetId: EUID): Observable<Long> {
        return this.getUnconsumedConsumables(address)
                .map { it.asSequence() }
                .map { sequence ->
                    sequence.filter { consumable ->
                        consumable.assetId == assetId
                    }.map {
                        it.quantity
                    }.sum()
                }.share()
    }

    fun getSubUnitBalance(address: RadixAddress, asset: Asset): Observable<Long> {
        return this.getSubUnitBalance(address, asset.id)
    }

    fun getXRDSubUnitBalance(address: RadixAddress): Observable<Long> {
        return this.getSubUnitBalance(address, Asset.XRD.id)
    }

    fun getXRDTransactions(address: RadixAddress): Observable<WalletTransaction> {
        return Observables.combineLatest(
                Observable.fromCallable { TransactionAtoms(address, Asset.XRD.id) },
                ledger.getAllAtoms(address.getUID(), TransactionAtom::class.java)) { transactionAtoms: TransactionAtoms, atom: TransactionAtom ->
            transactionAtoms.accept(atom).newValidTransactions
        }.flatMap { atom: Observable<TransactionAtom> ->
            atom
        }.map { atom: TransactionAtom ->
            WalletTransaction(address, atom)
        }
    }

    fun getXRDTransactionsGroupedByParticipants(address: RadixAddress): Observable<GroupedObservable<Set<ECPublicKey>, WalletTransaction>> {
        return this.getXRDTransactions(address)
                .groupBy { transaction ->
                    transaction.transactionAtom
                            .particles!!.asSequence()
                            .map { it.ownersPublicKeys }
                            .flatMap { it.asSequence() }
                            .filter { publicKey -> address.publicKey != publicKey }
                            .distinct()
                            .toSet()
                }
    }

    /**
     * Creates a new transaction atom from available consumables on subscription.
     * Mainly for internal use. Use transferXRD and transferXRDWhenAvailable for
     * the simplest interface.
     *
     * @param amountInSubUnits amount to transfer
     * @param fromAddress address which consumables will originate from
     * @param toAddress address to send XRD to
     * @param payload message or data one can attach to the atom
     * @param withPOWFee whether or not to calculate and add a POW fee consumable
     * @return a new unsigned atom to be created on subscription
     */
    fun createXRDTransaction(
            amountInSubUnits: Long,
            fromAddress: RadixAddress,
            toAddress: RadixAddress,
            payload: ByteArray?,
            withPOWFee: Boolean,
            extraParticle: Particle?
    ): Single<UnsignedAtom> {
        return this.getUnconsumedConsumables(fromAddress)
                .firstOrError()
                .map { unconsumedConsumables ->
                    val atomBuilder = AtomBuilder()
                    atomBuilder.type(TransactionAtom::class.java)

                    if (payload != null) {
                        atomBuilder.payload(payload)
                    }

                    var consumerTotal: Long = 0
                    val iterator = unconsumedConsumables.iterator()
                    val consumerQuantities = HashMap<Set<ECKeyPair>, Long>()

                    if (extraParticle != null) {
                        atomBuilder.addParticle(extraParticle)
                    }

                    // HACK for now
                    // TODO: remove this, create a ConsumersCreator
                    // TODO: randomize this to decrease probability of collision
                    while (consumerTotal < amountInSubUnits && iterator.hasNext()) {
                        val left = amountInSubUnits - consumerTotal

                        val newConsumer = iterator.next().toConsumer()
                        consumerTotal += newConsumer.quantity

                        val amount = Math.min(left, newConsumer.quantity)
                        newConsumer.addConsumerQuantities(amount, setOf(toAddress.toECKeyPair()),
                                consumerQuantities)

                        atomBuilder.addParticle(newConsumer)
                    }

                    if (consumerTotal < amountInSubUnits) {
                        Exceptions.propagate(InsufficientFundsException(Asset.XRD, consumerTotal, amountInSubUnits))
                    }

                    val consumables = consumerQuantities.entries.asSequence()
                            .map { entry ->
                                Consumable(entry.value, entry.key, System.nanoTime(), Asset.XRD.id)
                            }.toList()
                    atomBuilder.addParticles(consumables)

                    return@map if (withPOWFee) {
                        // TODO: Replace this with public key of processing node runner
                        atomBuilder.buildWithPOWFee(ledger.magic, fromAddress.publicKey)
                    } else {
                        atomBuilder.build()
                    }
                }
    }

    fun transferXRD(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            extraParticle: Particle
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRD(amountInSubUnits, fromIdentity, toAddress, null, extraParticle)
    }

    fun transferXRD(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRD(amountInSubUnits, fromIdentity, toAddress, null)
    }

    fun transferXRD(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            payload: String
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRD(amountInSubUnits, fromIdentity, toAddress, payload.toByteArray())
    }

    fun transferXRD(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            payload: ByteArray?
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRD(amountInSubUnits, fromIdentity, toAddress, payload, null)
    }

    fun transferXRD(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            payload: ByteArray?,
            extraParticle: Particle?
    ): Observable<AtomSubmissionUpdate> {
        if (amountInSubUnits <= 0) {
            throw IllegalArgumentException("Cannot send negative or 0 XRD.")
        }
        Objects.requireNonNull(fromIdentity)
        Objects.requireNonNull(toAddress)

        val fromAddress = universe.getAddressFrom(fromIdentity.getPublicKey())
        val statusObservable = this.createXRDTransaction(amountInSubUnits, fromAddress, toAddress, payload, true, extraParticle)
                .flatMap { fromIdentity.sign(it) }
                .flatMapObservable { ledger.submitAtom(it) }
                .replay()
        statusObservable.connect()
        return statusObservable
    }

    fun transferXRDWhenAvailable(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            extraParticle: Particle
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRDWhenAvailable(amountInSubUnits, fromIdentity, toAddress, null, extraParticle)
    }


    fun transferXRDWhenAvailable(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRDWhenAvailable(amountInSubUnits, fromIdentity, toAddress, null)
    }

    fun transferXRDWhenAvailable(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            payload: String
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRDWhenAvailable(amountInSubUnits, fromIdentity, toAddress, payload.toByteArray())
    }

    fun transferXRDWhenAvailable(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            payload: ByteArray?
    ): Observable<AtomSubmissionUpdate> {
        return this.transferXRDWhenAvailable(amountInSubUnits, fromIdentity, toAddress, payload, null)
    }

    fun transferXRDWhenAvailable(
            amountInSubUnits: Long,
            fromIdentity: RadixIdentity,
            toAddress: RadixAddress,
            payload: ByteArray?,
            extraParticle: Particle?
    ): Observable<AtomSubmissionUpdate> {
        if (amountInSubUnits <= 0) {
            throw IllegalArgumentException("Cannot send negative or 0 XRD.")
        }

        val fromAddress = universe.getAddressFrom(fromIdentity.getPublicKey())
        val status = this.getXRDSubUnitBalance(fromAddress)
                .filter { balance -> balance > amountInSubUnits }
                .firstOrError()
                .ignoreElement()
                .andThen(
                        Single.fromCallable { this.transferXRD(amountInSubUnits, fromIdentity, toAddress, payload, extraParticle) }.flatMapObservable { t -> t })
                .replay()

        status.connect()
        return status
    }

    companion object {

        /**
         * Lock to protect default wallet instance
         */
        private val lock = Any()
        private var radixWallet: RadixWallet? = null

        @JvmStatic
        val instance: RadixWallet
            get() = synchronized(lock) {
                if (radixWallet == null) {
                    radixWallet = RadixWallet(RadixUniverse.instance)
                }
                return radixWallet as RadixWallet
            }
    }
}

/**
 * Implementation that doesn't block when map already
 * contains the value
 */
fun <K, V> ConcurrentHashMap<K, V>.computeIfAbsentFunction(key: K, mappingFunction: (t: K) -> V): V {
    return this[key] ?: run {
        val value = mappingFunction(key)
        this[key] = value
        return@run value
    }
}


/**
 * Synchronised that doesn't block when map already
 * contains the value
 */
fun <K, V> ConcurrentHashMap<K, V>.computeIfAbsentSynchronisedFunction(key: K, mappingFunction: (t: K) -> V): V {
    return this[key] ?: synchronized(this) {
        var valueSynchronized = get(key)
        if (valueSynchronized == null) {
            valueSynchronized = mappingFunction(key)
            this[key] = valueSynchronized
        }
        return@synchronized valueSynchronized!!
    }
}
