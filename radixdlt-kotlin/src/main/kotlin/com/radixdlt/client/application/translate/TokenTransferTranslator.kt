package com.radixdlt.client.application.translate

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.atoms.TokenReference
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.DataParticle
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.crypto.Encryptor
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.serialization.RadixJson
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import io.reactivex.Completable
import io.reactivex.Observable
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

class TokenTransferTranslator(
    private val universe: RadixUniverse,
    private val particleStore: ParticleStore
) {

    private val cache = ConcurrentHashMap<RadixAddress, AddressTokenReducer>()

    fun fromAtom(atom: Atom): List<TokenTransfer> {
        return atom.tokenSummary().entries.asSequence()
            .map { e ->
                val summary = ArrayList<Map.Entry<ECPublicKey, Long>>(e.value.entries)

                if (summary.isEmpty()) {
                    throw IllegalStateException("Invalid atom: ${RadixJson.gson.toJson(atom)}")
                }

                if (summary.size > 2) {
                    throw IllegalStateException(
                        "More than two participants in token transfer. Unable to handle: $summary"
                    )
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

                val bytesParticle: DataParticle? = atom.getDataParticles()!!.asSequence()
                    .filter { p -> "encryptor" != p.getMetaData("application") }
                    .firstOrNull()

                // Construct attachment from atom
                val attachment: Data?
                if (bytesParticle != null) {
                    val metaData = HashMap<String, Any>()

                    val encryptorParticle: DataParticle? = atom.getDataParticles()!!.asSequence()
                        .filter { p -> "encryptor" == p.getMetaData("application") }
                        .firstOrNull()

                    metaData["encrypted"] = encryptorParticle != null

                    val encryptor: Encryptor? = if (encryptorParticle != null) {
                        val encryptorBytes = encryptorParticle.bytes!!.toUtf8String()
                        val protectorsJson = JSON_PARSER.parse(encryptorBytes).asJsonArray
                        val protectors = ArrayList<EncryptedPrivateKey>()
                        protectorsJson.forEach { protectorJson ->
                            protectors.add(EncryptedPrivateKey.fromBase64(protectorJson.asString))
                        }
                        Encryptor(protectors)
                    } else {
                        null
                    }
                    attachment = Data.raw(encryptorParticle?.bytes?.bytes, metaData, encryptor)
                } else {
                    attachment = null
                }

                val amount = TokenReference.subUnitsToDecimal(Math.abs(summary[0].value))
                return@map TokenTransfer.create(from, to, amount, e.key, attachment, atom.timestamp)
            }
            .toList()
    }

    fun getTokenState(address: RadixAddress?): Observable<AddressTokenState> {
        return cache.computeIfAbsentSynchronisedFunction(address!!) { addr ->
            AddressTokenReducer(addr, particleStore)
        }.state
    }

    fun translate(transfer: TokenTransfer, atomBuilder: AtomBuilder): Completable {
        return this.getTokenState(transfer.from)
            .firstOrError()
            .flatMapCompletable { state ->
                val allUnconsumedConsumables = state.unconsumedConsumables
                val unconsumedConsumables = if (allUnconsumedConsumables.containsKey(transfer.tokenReference)) {
                    allUnconsumedConsumables[transfer.tokenReference]
                } else {
                    emptyList()
                }

                // Translate attachment to corresponding atom structure
                val attachment = transfer.attachment
                if (attachment != null) {
                    atomBuilder.addParticle(
                        DataParticle.DataParticleBuilder()
                            .payload(Payload(attachment.bytes))
                            .account(transfer.from!!)
                            .account(transfer.to!!)
                            .build())
                    val encryptor = attachment.encryptor
                    if (encryptor != null) {
                        val protectorsJson = JsonArray()
                        encryptor.protectors.asSequence().map(EncryptedPrivateKey::base64).forEach(protectorsJson::add)

                        val encryptorPayload = Payload(protectorsJson.toString().toByteArray(StandardCharsets.UTF_8))
                        val encryptorParticle = DataParticle.DataParticleBuilder()
                            .payload(encryptorPayload)
                            .setMetaData("application", "encryptor")
                            .setMetaData("contentType", "application/json")
                            .account(transfer.from)
                            .account(transfer.to)
                            .build()
                        atomBuilder.addParticle(encryptorParticle)
                    }
                }

                var consumerTotal: Long = 0
                val subUnitAmount = transfer.amount.multiply(TokenReference.getSubUnits()).longValueExact()
                val iterator = unconsumedConsumables!!.iterator()
                val consumerQuantities = HashMap<ECKeyPair, Long>()

                // HACK for now
                // TODO: remove this, create a ConsumersCreator
                // TODO: randomize this to decrease probability of collision
                while (consumerTotal < subUnitAmount && iterator.hasNext()) {
                    val left = subUnitAmount - consumerTotal

                    val down = iterator.next().spinDown()
                    consumerTotal += down.amount

                    val amount = Math.min(left, down.amount)
                    down.addConsumerQuantities(amount, transfer.to!!.toECKeyPair(), consumerQuantities)

                    atomBuilder.addParticle(down)
                }

                if (consumerTotal < subUnitAmount) {
                    return@flatMapCompletable Completable.error(
                        InsufficientFundsException(
                            transfer.tokenReference, TokenReference.subUnitsToDecimal(consumerTotal), transfer.amount
                        )
                    )
                }

                val consumables = consumerQuantities.entries.asSequence()
                    .map { entry ->
                        Consumable(
                            entry.value,
                            AccountReference(entry.key.getPublicKey()),
                            System.nanoTime(),
                            transfer.tokenReference,
                            System.currentTimeMillis() / 60000L + 60000L,
                            Spin.UP
                        )
                    }
                    .toList()
                atomBuilder.addParticles(consumables)

                return@flatMapCompletable Completable.complete()
            }
    }

    companion object {
        private val JSON_PARSER = JsonParser()
    }
}
