package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.TransferTokensAction
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.TokenTransfer
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.TransactionAtom
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.ledger.computeIfAbsentSynchronisedFunction
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

class TokenTransferTranslator(
    private val universe: RadixUniverse,
    private val particleStore: ParticleStore
) {

    private val cache = ConcurrentHashMap<RadixAddress, AddressTokenReducer>()

    fun fromAtom(transactionAtom: TransactionAtom, identity: RadixIdentity): Single<TokenTransfer> {
        val summary = transactionAtom.summary().entries.asSequence()
            .filter { entry -> entry.value.containsKey(Asset.TEST.id) }
            .map { entry ->
                SimpleImmutableEntry<ECPublicKey, Long>(
                    entry.key.iterator().next(),
                    entry.value[Asset.TEST.id]
                )
            }
            .toList()

        if (summary.size > 2) {
            throw IllegalStateException("More than two participants in token transfer. Unable to handle: $summary")
        }

        val from: RadixAddress?
        val to: RadixAddress?
        if (summary.size == 1) {
            from = if (summary[0].value <= 0L) universe.getAddressFrom(summary[0].key) else null
            to = if (summary[0].value < 0L) null else universe.getAddressFrom(summary[0].key)
        } else {
            if (summary[0].value > 0) {
                from = universe.getAddressFrom(summary[1].key)
                to = universe.getAddressFrom(summary[0].key)
            } else {
                from = universe.getAddressFrom(summary[0].key)
                to = universe.getAddressFrom(summary[1].key)
            }
        }

        val subUnitAmount = Math.abs(summary[0].value)

        val attachment: Data?
        if (transactionAtom.encrypted != null) {
            val protectors: List<EncryptedPrivateKey>
            if (transactionAtom.encryptor?.protectors != null) {
                protectors = transactionAtom.encryptor.protectors
            } else {
                protectors = emptyList()
            }
            val metaData = HashMap<String, Any>()
            metaData["encrypted"] = !protectors.isEmpty()
            attachment = Data.raw(transactionAtom.encrypted.bytes, metaData, protectors)

            val timestamp = transactionAtom.timestamp
            return Single.just(attachment)
                .flatMap(identity::decrypt)
                .map { unencrypted -> TokenTransfer(from!!, to!!, Asset.TEST, subUnitAmount, unencrypted, timestamp) }
                .onErrorResumeNext { e ->
                    if (e is CryptoException) {
                        return@onErrorResumeNext Single.just(
                            TokenTransfer(from!!, to!!, Asset.TEST, subUnitAmount, null, timestamp)
                        )
                    } else {
                        return@onErrorResumeNext Single.error(e)
                    }
                }
        } else {
            return Single.just(
                TokenTransfer(from!!, to!!, Asset.TEST, subUnitAmount, null, transactionAtom.timestamp)
            )
        }
    }

    fun getTokenState(address: RadixAddress?): Observable<AddressTokenState> {
        return cache.computeIfAbsentSynchronisedFunction(address!!) { addr ->
            AddressTokenReducer(addr, particleStore)
        }.state
    }

    fun translate(transferTokensAction: TransferTokensAction, atomBuilder: AtomBuilder): Completable {
        atomBuilder.type(TransactionAtom::class.java)

        return this.getTokenState(transferTokensAction.from)
            .map(AddressTokenState::unconsumedConsumables)
            .firstOrError()
            .flatMapCompletable { unconsumedConsumables ->

                if (transferTokensAction.attachment != null) {
                    atomBuilder.payload(transferTokensAction.attachment.bytes!!)
                    if (!transferTokensAction.attachment.protectors.isEmpty()) {
                        atomBuilder.protectors(transferTokensAction.attachment.protectors)
                    }
                }

                var consumerTotal: Long = 0
                val iterator = unconsumedConsumables.iterator()
                val consumerQuantities = HashMap<Set<ECKeyPair>, Long>()

                // HACK for now
                // TODO: remove this, create a ConsumersCreator
                // TODO: randomize this to decrease probability of collision
                while (consumerTotal < transferTokensAction.subUnitAmount && iterator.hasNext()) {
                    val left = transferTokensAction.subUnitAmount - consumerTotal

                    val newConsumer = iterator.next().toConsumer()
                    consumerTotal += newConsumer.quantity

                    val amount = Math.min(left, newConsumer.quantity)
                    newConsumer.addConsumerQuantities(
                        amount, setOf(transferTokensAction.to!!.toECKeyPair()),
                        consumerQuantities
                    )

                    atomBuilder.addParticle(newConsumer)
                }

                if (consumerTotal < transferTokensAction.subUnitAmount) {
                    return@flatMapCompletable Completable.error(
                        InsufficientFundsException(
                            transferTokensAction.tokenClass, consumerTotal, transferTokensAction.subUnitAmount
                        )
                    )
                }

                val consumables = consumerQuantities.entries.asSequence()
                    .map { entry -> Consumable(entry.value, entry.key, System.nanoTime(), Asset.TEST.id) }
                    .toList()
                atomBuilder.addParticles(consumables)

                return@flatMapCompletable Completable.complete()
            }
    }
}
