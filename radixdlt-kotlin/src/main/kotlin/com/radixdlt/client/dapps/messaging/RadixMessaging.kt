package com.radixdlt.client.dapps.messaging

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.RadixApplicationAPI.Result
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECSignature
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.observables.GroupedObservable
import org.slf4j.LoggerFactory
import java.util.Objects

/**
 * High Level API for Instant Messaging. Currently being used by the Radix Android Mobile Wallet.
 */
class RadixMessaging(private val api: RadixApplicationAPI) {

    private val identity: RadixIdentity = api.identity
    private val myAddress: RadixAddress = api.address
    private val parser = JsonParser()

    val allMessages: Observable<RadixMessage>
        get() = api.getReadableData(myAddress)
            .filter { data -> APPLICATION_ID == data.metaData["application"] }
            .flatMapMaybe { data ->
                try {
                    val jsonObject = parser.parse(String(data.data)).asJsonObject
                    val from = RadixAddress(jsonObject.get("from").asString)
                    val to = RadixAddress(jsonObject.get("to").asString)
                    val content = jsonObject.get("content").asString
                    val signaturesUnchecked = data.metaData["signatures"]
                    val signatures = signaturesUnchecked as Map<String, ECSignature>
                    signatures[from.getUID().toString()] ?: throw RuntimeException("Unsigned message")
                    val timestamp = data.metaData["timestamp"] as Long
                    return@flatMapMaybe Maybe.just(
                        RadixMessage(from, to, content, timestamp, data.isFromEncryptedSource)
                    )
                } catch (e: Exception) {
                    LOGGER.warn(e.message)
                    return@flatMapMaybe Maybe.empty<RadixMessage>()
                }
            }

    val allMessagesGroupedByParticipants: Observable<GroupedObservable<RadixAddress, RadixMessage>>
        get() = this.allMessages
            .groupBy { msg -> if (msg.from.publicKey == identity.getPublicKey()) msg.to else msg.from }

    fun sendMessage(message: String, toAddress: RadixAddress, uniqueId: EUID?): Result {
        Objects.requireNonNull(message)
        Objects.requireNonNull(toAddress)

        if (message.length > MAX_MESSAGE_LENGTH) {
            throw IllegalArgumentException(
                "Message must be under " + MAX_MESSAGE_LENGTH + " characters but was " + message.length
            )
        }

        if (uniqueId != null) {
            throw IllegalArgumentException("Unique ids not supported")
        }

        val messageJson = JsonObject()
        messageJson.addProperty("from", myAddress.toString())
        messageJson.addProperty("to", toAddress.toString())
        messageJson.addProperty("content", message)

        val data = Data.DataBuilder()
            .bytes(messageJson.toString().toByteArray())
            .metaData("application", RadixMessaging.APPLICATION_ID)
            .addReader(toAddress.publicKey)
            .addReader(myAddress.publicKey)
            .build()

        return api.storeData(data, toAddress, myAddress)
    }

    fun sendMessage(message: String, toAddress: RadixAddress): Result {
        return this.sendMessage(message, toAddress, null)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixMessaging::class.java)

        const val APPLICATION_ID = "radix-messaging"

        const val MAX_MESSAGE_LENGTH = 256
    }
}
