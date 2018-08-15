package com.radixdlt.client.services

import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.identity.SimpleRadixIdentity
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.messaging.RadixMessage
import com.radixdlt.client.messaging.RadixMessaging
import io.reactivex.ObservableSource
import java.sql.Timestamp
import java.util.function.Function
import java.util.function.Supplier

/**
 * This is an example of a ChatBot service which uses the RadixMessaging module
 * to chat with users in a Radix Universe.
 */
class ChatBot(
        /**
         * The Chatbot's RadixIdentity, an object which keeps the Chatbot's private key
         */
        private val identity: RadixIdentity,
        /**
         * The chat algorithm to run on each new conversation
         *
         * TODO: make this asynchronous via Observers/Observables
         */
        private val chatBotAlgorithmSupplier: Supplier<Function<String, String>>) {

    /**
     * Connect to the network and begin running the service
     */
    fun run() {
        val address = RadixUniverse.instance.getAddressFrom(identity.getPublicKey())

        println("Chatbot address: $address")

        // Subscribe/Decrypt messages and reply
        RadixMessaging.instance
                .getAllMessagesDecryptedAndGroupedByParticipants(identity)
                .flatMap { convo ->
                    convo
                            .doOnNext { message -> println("Received at " + Timestamp(System.currentTimeMillis()) + ": " + message) } // Print messages
                            .filter { message -> message.from != address } // Don't reply to ourselves!
                            .filter { message -> Math.abs(message.timestamp - System.currentTimeMillis()) < 60000 } // Only reply to recent messages
                            .flatMap(object : io.reactivex.functions.Function<RadixMessage, ObservableSource<AtomSubmissionUpdate>> {
                                internal var chatBotAlgorithm = chatBotAlgorithmSupplier.get()

                                override fun apply(message: RadixMessage): ObservableSource<AtomSubmissionUpdate> {
                                    return RadixMessaging.instance.sendMessage(chatBotAlgorithm.apply(message.getContent()), identity, message.from)
                                }
                            })
                }.subscribe(
                        { println(it) },
                        { it.printStackTrace() }
                )
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            RadixUniverse.bootstrap(Bootstrap.ALPHANET)

            RadixUniverse.instance
                    .network
                    .getStatusUpdates()
                    .subscribe { println(it) }

            // Setup Identity of Chatbot
            val radixIdentity = SimpleRadixIdentity("chatbot.key")

            val chatBot = ChatBot(radixIdentity, Supplier {
                object : Function<String, String> {
                    var messageCount = 0

                    override fun apply(s: String): String {
                        return when (messageCount++) {
                            0 -> "Who dis?"
                            1 -> "Howdy $s"
                            5 -> "Chillz out yo"
                            else -> "I got nothing more to say"
                        }
                    }
                }
            })
            chatBot.run()
        }
    }
}
