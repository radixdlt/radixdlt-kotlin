package com.radixdlt.client.application

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.application.translate.ConsumableDataSource
import com.radixdlt.client.application.translate.DataStoreTranslator
import com.radixdlt.client.application.translate.TokenTransferTranslator
import com.radixdlt.client.application.translate.TransactionAtoms
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observables.ConnectableObservable
import io.reactivex.rxkotlin.Observables

class RadixApplicationAPI private constructor(
    val identity: RadixIdentity,
    private val universe: RadixUniverse,
    private val atomBuilderSupplier: () -> AtomBuilder
) {

    private val dataStoreTranslator: DataStoreTranslator = DataStoreTranslator.instance

    val ledger: RadixLedger = universe.ledger

    private val consumableDataSource = ConsumableDataSource(ledger)
    private val tokenTransferTranslator = TokenTransferTranslator(universe, consumableDataSource)

    val address: RadixAddress
        get() = ledger.getAddressFromPublicKey(identity.getPublicKey())

    class Result internal constructor(private val updates: Observable<AtomSubmissionUpdate>) {
        private val completable: Completable

        init {

            this.completable = updates.filter { it.isComplete }
                .firstOrError()
                .flatMapCompletable { update ->
                    if (update.getState() === AtomSubmissionState.STORED) {
                        return@flatMapCompletable Completable.complete()
                    } else {
                        return@flatMapCompletable Completable.error(RuntimeException(update.message))
                    }
                }
        }

        fun toObservable(): Observable<AtomSubmissionUpdate> {
            return updates
        }

        fun toCompletable(): Completable {
            return completable
        }
    }

    fun getReadableData(address: RadixAddress): Observable<UnencryptedData> {
        return ledger.getAllAtoms(address.getUID())
            .map { dataStoreTranslator.fromAtom(it) }
            .flatMapMaybe { data -> identity.decrypt(data).toMaybe().onErrorComplete() }
    }

    fun storeData(data: Data, address: RadixAddress): Result {
        val dataStore = DataStore(data, address)

        val atomBuilder = atomBuilderSupplier()
        val updates: ConnectableObservable<AtomSubmissionUpdate> = dataStoreTranslator.translate(dataStore, atomBuilder)
            .andThen(Single.fromCallable { atomBuilder.buildWithPOWFee(ledger.magic, address.publicKey) })
            .flatMap(identity::sign)
            .flatMapObservable(ledger::submitAtom)
            .replay()

        updates.connect()

        return Result(updates)
    }

    fun storeData(data: Data, address0: RadixAddress, address1: RadixAddress): Result {
        val dataStore = DataStore(data, address0, address1)

        val atomBuilder = atomBuilderSupplier()
        val updates: ConnectableObservable<AtomSubmissionUpdate> = dataStoreTranslator.translate(dataStore, atomBuilder)
            .andThen(Single.fromCallable { atomBuilder.buildWithPOWFee(ledger.magic, address0.publicKey) })
            .flatMap(identity::sign)
            .flatMapObservable(ledger::submitAtom)
            .replay()

        updates.connect()

        return Result(updates)
    }

    fun getTokenTransfers(address: RadixAddress, tokenClass: Asset): Observable<TokenTransfer> {
        return Observables.combineLatest<TransactionAtoms, Atom, Observable<Atom>>(
            Observable.fromCallable { TransactionAtoms(address, tokenClass.id) },
            ledger.getAllAtoms(address.getUID())
        ) { transactionAtoms, atom ->
            transactionAtoms.accept(atom).newValidTransactions
        }
            .flatMap { atoms -> atoms.map { tokenTransferTranslator.fromAtom(it) } }
    }

    fun getSubUnitBalance(address: RadixAddress, tokenClass: Asset): Observable<Long> {
        return this.consumableDataSource.getConsumables(address)
            .map { it.asSequence() }
            .map { sequence ->
                sequence.filter { consumable ->
                    consumable.assetId == tokenClass.id
                }
                    .map(Consumable::quantity)
                    .sum()
            }
            .share()
    }

    fun transferTokens(
        from: RadixAddress,
        to: RadixAddress,
        tokenClass: Asset,
        subUnitAmount: Long,
        attachment: Data?
    ): Result {
        val tokenTransfer = TokenTransfer.create(from, to, tokenClass, subUnitAmount, attachment)
        val atomBuilder = atomBuilderSupplier()

        val updates = tokenTransferTranslator.translate(tokenTransfer, atomBuilder)
            .andThen(Single.fromCallable<UnsignedAtom> { atomBuilder.buildWithPOWFee(ledger.magic, from.publicKey) })
            .flatMap(identity::sign)
            .flatMapObservable<AtomSubmissionUpdate>(ledger::submitAtom)
            .replay()

        updates.connect()

        return Result(updates)
    }

    fun transferTokens(from: RadixAddress, to: RadixAddress, tokenClass: Asset, subUnitAmount: Long): Result {
        val tokenTransfer = TokenTransfer.create(from, to, tokenClass, subUnitAmount)
        val atomBuilder = atomBuilderSupplier()

        val updates = tokenTransferTranslator.translate(tokenTransfer, atomBuilder)
            .andThen(Single.fromCallable<UnsignedAtom> { atomBuilder.buildWithPOWFee(ledger.magic, from.publicKey) })
            .flatMap { identity.sign(it) }
            .flatMapObservable<AtomSubmissionUpdate>(ledger::submitAtom)
            .replay()

        updates.connect()

        return Result(updates)
    }

    companion object {

        @JvmStatic
        fun create(identity: RadixIdentity): RadixApplicationAPI {
            return RadixApplicationAPI(identity, RadixUniverse.instance, ::AtomBuilder)
        }

        @JvmStatic
        fun create(
            identity: RadixIdentity,
            universe: RadixUniverse,
            atomBuilderSupplier: () -> AtomBuilder
        ): RadixApplicationAPI {
            return RadixApplicationAPI(identity, universe, atomBuilderSupplier)
        }
    }
}
