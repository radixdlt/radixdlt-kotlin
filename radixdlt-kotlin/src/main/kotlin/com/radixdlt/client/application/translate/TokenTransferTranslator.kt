package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import io.reactivex.Completable
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.HashMap

class TokenTransferTranslator(
    private val universe: RadixUniverse,
    private val consumableDataSource: ConsumableDataSource
) {

    fun fromAtom(atom: Atom): TokenTransfer {
        val summary = atom.summary().entries.asSequence()
            .filter { entry -> entry.value.containsKey(Asset.XRD.id) }
            .map { entry ->
                SimpleImmutableEntry<ECPublicKey, Long>(
                    entry.key.iterator().next(),
                    entry.value[Asset.XRD.id]
                )
            }
            .toList()

        if (summary.isEmpty()) {
            throw IllegalStateException("Invalid atom: $atom")
        }

        if (summary.size > 2) {
            throw IllegalStateException("More than two participants in token transfer. Unable to handle: $summary")
        }

        val from: RadixAddress?
        val to: RadixAddress?
        if (summary.size == 1) {
            from = if (summary[0].value <= 0L) universe.getAddressFrom(summary[0].key) else null
            to = if (summary[0].value <= 0L) null else universe.getAddressFrom(summary[0].key)
        } else {
            if (summary[0].value > 0) {
                from = universe.getAddressFrom(summary[1].key)
                to = universe.getAddressFrom(summary[0].key)
            } else {
                from = universe.getAddressFrom(summary[0].key)
                to = universe.getAddressFrom(summary[1].key)
            }
        }

        val attachment: Data?
        if (atom.payload != null) {
            val protectors: List<EncryptedPrivateKey> = if (atom.encryptor?.protectors != null) {
                atom.encryptor.protectors
            } else {
                emptyList()
            }
            val metaData = HashMap<String, Any>()
            metaData["encrypted"] = !protectors.isEmpty()
            attachment = Data.raw(atom.payload.bytes, metaData, protectors)
        } else {
            attachment = null
        }

        return TokenTransfer.create(
            from, to, Asset.XRD, Math.abs(summary[0].value), attachment, atom.timestamp
        )
    }

    fun translate(tokenTransfer: TokenTransfer, atomBuilder: AtomBuilder): Completable {
        return this.consumableDataSource.getConsumables(tokenTransfer.from!!)
            .firstOrError()
            .flatMapCompletable { unconsumedConsumables ->

                if (tokenTransfer.attachment != null) {
                    atomBuilder.payload(tokenTransfer.attachment.bytes!!)
                    if (!tokenTransfer.attachment.protectors.isEmpty()) {
                        atomBuilder.protectors(tokenTransfer.attachment.protectors)
                    }
                }

                var consumerTotal: Long = 0
                val iterator = unconsumedConsumables.iterator()
                val consumerQuantities = HashMap<Set<ECKeyPair>, Long>()

                // HACK for now
                // TODO: remove this, create a ConsumersCreator
                // TODO: randomize this to decrease probability of collision
                while (consumerTotal < tokenTransfer.subUnitAmount && iterator.hasNext()) {
                    val left = tokenTransfer.subUnitAmount - consumerTotal

                    val newConsumer = iterator.next().toConsumer()
                    consumerTotal += newConsumer.quantity

                    val amount = Math.min(left, newConsumer.quantity)
                    newConsumer.addConsumerQuantities(
                        amount, setOf(tokenTransfer.to!!.toECKeyPair()),
                        consumerQuantities
                    )

                    atomBuilder.addParticle(newConsumer)
                }

                if (consumerTotal < tokenTransfer.subUnitAmount) {
                    return@flatMapCompletable Completable.error(
                        InsufficientFundsException(
                            tokenTransfer.tokenClass, consumerTotal, tokenTransfer.subUnitAmount
                        )
                    )
                }

                val consumables = consumerQuantities.entries.asSequence()
                    .map { entry -> Consumable(entry.value, entry.key, System.nanoTime(), Asset.XRD.id) }
                    .toList()
                atomBuilder.addParticles(consumables)

                return@flatMapCompletable Completable.complete()

                /*
                if (withPOWFee) {
                    // TODO: Replace this with public key of processing node runner
                    return atomBuilder.buildWithPOWFee(ledger.getMagic(), fromAddress.getPublicKey());
                } else {
                    return atomBuilder.build();
                }
                */
            }
    }
}
