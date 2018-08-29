package com.radixdlt.client.examples

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.identity.SimpleRadixIdentity
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.dapps.messaging.RadixMessage
import com.radixdlt.client.dapps.messaging.RadixMessaging
import io.reactivex.Completable
import java.sql.Timestamp

/**
 * This is an example of a ChatBot service which uses the RadixMessaging module
 * to chat with users in a Radix Universe.
 */
class ChatBot(
    private val api: RadixApplicationAPI,
    /**
     * The chat algorithm to run on each new conversation
     *
     * TODO: make this asynchronous via Observers/Observables
     */
    private val chatBotAlgorithmSupplier: () -> (String) -> String
) {

    private val messaging: RadixMessaging = RadixMessaging(api)

    /**
     * Connect to the network and begin running the service
     */
    fun run() {
        println("Chatbot myAddress: " + api.myAddress)

        // Subscribe/Decrypt messages
        messaging
            .allMessagesGroupedByParticipants
            .flatMapCompletable { convo ->
                convo
                    .doOnNext { message -> println("Received at " + Timestamp(System.currentTimeMillis()) + ": " + message) } // Print messages
                    .filter { message -> message.from != api.myAddress } // Don't reply to ourselves!
                    .filter { message -> Math.abs(message.timestamp - System.currentTimeMillis()) < 60000 } // Only reply to recent messages
                    .flatMapCompletable(object : io.reactivex.functions.Function<RadixMessage, Completable> {
                        var chatBotAlgorithm = chatBotAlgorithmSupplier()

                        override fun apply(message: RadixMessage): Completable {
                            return messaging.sendMessage(chatBotAlgorithm(message.content), message.from)
                                .toCompletable()
                        }
                    })
            }.subscribe(
                { println() },
                { it.printStackTrace() }
            )
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            RadixUniverse.bootstrap(Bootstrap.WINTERFELL)

            RadixUniverse.instance
                .network
                .getStatusUpdates()
                .subscribe { println(it) }

            // Setup Identity of Chatbot
            val radixIdentity = SimpleRadixIdentity("chatbot.key")

            val api = RadixApplicationAPI.create(radixIdentity)

            val chatBot = ChatBot(api) {
                object : (String) -> String {
                    var messageCount = 0

                    override fun invoke(s: String): String {
                        return when (messageCount++) {
                            0 -> "Who dis?"
                            1 -> "Howdy $s"
                            5 -> "Chillz out yo"
                            else -> "I got nothing more to say"
                        }
                    }
                }
            }
            chatBot.run()
        }
    }
}
