package com.radixdlt.client.examples

import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.identity.SimpleRadixIdentity
import com.radixdlt.client.messaging.RadixMessaging

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
        val radixIdentity = SimpleRadixIdentity()

        // Addresses
        val toAddress = RadixAddress.fromString(TO_ADDRESS_BASE58)

        when (queryType) {
            RadixMessagesQueryType.ALL ->
                // Print out to console all received messages
                RadixMessaging.instance
                        .getAllMessagesDecrypted(radixIdentity)
                        .subscribe { println(it) }

            RadixMessagesQueryType.BY_CONVO ->
                // Group messages by other address, useful for messaging apps
                RadixMessaging.instance
                        .getAllMessagesDecryptedAndGroupedByParticipants(radixIdentity)
                        .subscribe { convo ->
                            println("New Conversation with: " + convo.key)
                            convo.subscribe { println(it) }
                        }
        }

        // Send a message!
        RadixMessaging.instance
                .sendMessage(MESSAGE, radixIdentity, toAddress)
                .subscribe { println(it) }
    }
}
