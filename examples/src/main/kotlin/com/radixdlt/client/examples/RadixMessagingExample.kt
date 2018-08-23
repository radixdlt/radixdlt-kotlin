package com.radixdlt.client.examples

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.identity.SimpleRadixIdentity
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.dapps.messaging.RadixMessaging

object RadixMessagingExample {
    private const val TO_ADDRESS_BASE58 = "JFgcgRKq6GbQqP8mZzDRhtr7K7YQM1vZiYopZLRpAeVxcnePRXX"
    private const val MESSAGE = "Hello World!"
    private val queryType = RadixMessagesQueryType.BY_CONVO

    private enum class RadixMessagesQueryType {
        ALL,
        BY_CONVO
    }

    init {
        RadixUniverse.bootstrap(Bootstrap.BETANET)
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Display network connectivity
        RadixUniverse.instance
            .network
            .getStatusUpdates()
            .subscribe { println(it) }

        // Identity Manager which manages user's keys, signing, encrypting and decrypting
        val api = RadixApplicationAPI.create(SimpleRadixIdentity())

        // Addresses
        val toAddress = RadixAddress.fromString(TO_ADDRESS_BASE58)

        val messaging = RadixMessaging(api)

        when (queryType) {
            RadixMessagingExample.RadixMessagesQueryType.ALL ->
                // Print out to console all received messages
                messaging
                    .allMessages
                    .subscribe(::println, Throwable::printStackTrace)

            RadixMessagingExample.RadixMessagesQueryType.BY_CONVO ->
                // Group messages by other address, useful for messaging apps
                messaging
                    .allMessagesGroupedByParticipants
                    .subscribe { convo ->
                        println("New Conversation with: " + convo.key)
                        convo.subscribe(::println, Throwable::printStackTrace)
                    }
        }

        // Send a message!
        messaging
            .sendMessage(MESSAGE, toAddress)
            .toCompletable()
            .subscribe({ println("Submitted") }, Throwable::printStackTrace)
    }
}
