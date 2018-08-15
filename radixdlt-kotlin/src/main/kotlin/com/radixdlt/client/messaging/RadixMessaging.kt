package com.radixdlt.client.messaging

import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.IdParticle
import com.radixdlt.client.core.identity.RadixIdentities
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Observable
import io.reactivex.observables.GroupedObservable
import org.slf4j.LoggerFactory
import java.util.*

class RadixMessaging internal constructor(private val universe: RadixUniverse) {
    private val ledger: RadixLedger = universe.ledger


    fun getAllMessagesEncrypted(euid: EUID): Observable<EncryptedMessage> {
        return ledger.getAllAtoms(euid, ApplicationPayloadAtom::class.java)
                .filter { atom -> atom.applicationId == APPLICATION_ID } // Only get messaging atoms
                .map { EncryptedMessage.fromAtom(it) }
    }

    fun getAllMessagesEncrypted(address: RadixAddress): Observable<EncryptedMessage> {
        return this.getAllMessagesEncrypted(address.getUID())
    }

    fun getAllMessagesDecrypted(identity: RadixIdentity): Observable<RadixMessage> {
        return this.getAllMessagesEncrypted(identity.getPublicKey().getUID())
                .flatMapMaybe { decryptable ->
                    RadixIdentities.decrypt(identity, decryptable)
                            .toMaybe()
                            .doOnError { error -> LOGGER.error(error.toString()) }
                            .onErrorComplete()
                }
    }

    fun getAllMessagesDecryptedAndGroupedByParticipants(identity: RadixIdentity): Observable<GroupedObservable<RadixAddress, RadixMessage>> {
        return this.getAllMessagesDecrypted(identity)
                .groupBy { msg -> if (msg.from.publicKey == identity.getPublicKey()) msg.to else msg.from }
    }

    fun sendMessage(content: RadixMessageContent, fromIdentity: RadixIdentity, uniqueId: EUID?): Observable<AtomSubmissionUpdate> {
        Objects.requireNonNull(content)
        if (content.content.length > MAX_MESSAGE_LENGTH) {
            throw IllegalArgumentException(
                    "Message must be under " + MAX_MESSAGE_LENGTH + " characters but was " + content.content.length
            )
        }

        val atomBuilder = AtomBuilder()
                .type(ApplicationPayloadAtom::class.java)
                .applicationId(RadixMessaging.APPLICATION_ID)
                .payload(content.toJson())
                .addDestination(content.to!!)
                .addDestination(content.from!!)
                .addProtector(content.to.publicKey)
                .addProtector(content.from.publicKey)

        if (uniqueId != null) {
            atomBuilder.addParticle(IdParticle.create("jwt", uniqueId, fromIdentity.getPublicKey()))
        }

        val unsignedAtom = atomBuilder.buildWithPOWFee(ledger.magic, fromIdentity.getPublicKey())

        return fromIdentity.sign(unsignedAtom)
                .flatMapObservable { ledger.submitAtom(it) }
    }

    fun sendMessage(content: RadixMessageContent, fromIdentity: RadixIdentity): Observable<AtomSubmissionUpdate> {
        return this.sendMessage(content, fromIdentity, null)
    }

    fun sendMessage(message: String, fromIdentity: RadixIdentity, toAddress: RadixAddress, uniqueId: EUID?): Observable<AtomSubmissionUpdate> {
        Objects.requireNonNull(message)
        Objects.requireNonNull(fromIdentity)
        Objects.requireNonNull(toAddress)

        val fromAddress = universe.getAddressFrom(fromIdentity.getPublicKey())

        return this.sendMessage(RadixMessageContent(toAddress, fromAddress, message), fromIdentity, uniqueId)
    }

    fun sendMessage(message: String, fromIdentity: RadixIdentity, toAddress: RadixAddress): Observable<AtomSubmissionUpdate> {
        return this.sendMessage(message, fromIdentity, toAddress, null)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixMessaging::class.java)

        const val APPLICATION_ID = "radix-messaging"

        const val MAX_MESSAGE_LENGTH = 256

        /**
         * Lock to protect default messaging object
         */
        private val lock = Any()
        private var radixMessaging: RadixMessaging? = null

        @JvmStatic
        val instance: RadixMessaging
            get() = synchronized(lock) {
                if (radixMessaging == null) {
                    radixMessaging = RadixMessaging(RadixUniverse.instance)
                }
                return radixMessaging as RadixMessaging
            }
    }
}
