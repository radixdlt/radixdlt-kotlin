//package com.radixdlt.client.services
//
//import com.radixdlt.client.assets.Asset
//import com.radixdlt.client.core.Bootstrap
//import com.radixdlt.client.core.RadixUniverse
//import com.radixdlt.client.core.address.RadixAddress
//import com.radixdlt.client.application.myIdentity.RadixIdentity
//import com.radixdlt.client.application.myIdentity.SimpleRadixIdentity
//import com.radixdlt.client.core.network.AtomSubmissionUpdate
//import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
//import com.radixdlt.client.dapps.messaging.RadixMessage
//import com.radixdlt.client.dapps.messaging.RadixMessaging
//import com.radixdlt.client.dapps.wallet.RadixWallet
//import io.reactivex.Observable
//import io.reactivex.Single
//
//import java.io.IOException
//import java.sql.Timestamp
//import java.util.AbstractMap.SimpleImmutableEntry
//import java.util.concurrent.ConcurrentHashMap
//
///**
// * A service which sends tokens to whoever sends it a message through
// * a Radix Universe.
// */
//class Faucet
///**
// * A faucet created on the default universe
// *
// * @param radixIdentity myIdentity to load faucet off of
// */
//private constructor(
//    /**
//     * The RadixIdentity of this faucet, an object which keeps the Chatbot's private key
//     */
//    private val radixIdentity: RadixIdentity) {
//
//    /**
//     * The address of this faucet on which one can send messages to
//     */
//    private val sourceAddress: RadixAddress
//
//    /**
//     * Map to keep track of last request timestamps per user
//     */
//    private val recipientTimestamps = ConcurrentHashMap<RadixAddress, Long>()
//
//    /**
//     * Retrieves all recent messages sent to this faucet
//     *
//     * @return stream of recent messages to this faucet
//     */
//    private// Print out all messages
//    // Don't send ourselves money
//    // Only deal with recent messages
//    val recentMessages: Observable<RadixMessage>
//        get() = RadixMessaging.instance
//                .getAllMessagesDecrypted(radixIdentity)
//                .doOnNext { message -> println(Timestamp(System.currentTimeMillis()).toLocalDateTime().toString() + " " + message) }
//                .filter { message -> message.from != sourceAddress }
//                .filter { message -> Math.abs(message.timestamp - System.currentTimeMillis()) < 60000 }
//
//    init {
//        this.sourceAddress = RadixUniverse.instance.getAddressFrom(radixIdentity.getPublicKey())
//    }
//
//    /**
//     * Given a message, will attempt to send the requestor some tokens
//     * and then sends a message indicating whether successful or not.
//     *
//     * @param message the original message received
//     * @return the reply the faucet will send to the user
//     */
//    private fun leakFaucet(message: RadixMessage): Single<SimpleImmutableEntry<RadixAddress, String>> {
//        val address = message.from
//        val timestamp = System.currentTimeMillis()
//
//        if (this.recipientTimestamps.containsKey(myAddress) && timestamp - this.recipientTimestamps[myAddress]!! < DELAY) {
//            val timeSince = timestamp - this.recipientTimestamps[address]!!
//            if (timeSince < DELAY) {
//                val secondsTimeLeft = (DELAY - timeSince) / 1000 % 60
//                val minutesTimeLeft = (DELAY - timeSince) / 1000 / 60
//                return io.reactivex.Single.just(SimpleImmutableEntry(
//                        message.from,
//                        "Don't be hasty! You can only make one request every 10 minutes. $minutesTimeLeft minutes and $secondsTimeLeft seconds left."
//                ))
//            }
//        }
//
//        return RadixWallet.instance.send((10 * Asset.XRD.subUnits).toLong(), radixIdentity, message.from)
//                .doOnNext { state -> println("Transaction: $state") }
//                .filter { it.isComplete }
//                .firstOrError()
//                .doOnSuccess { update ->
//                    if (update.state === AtomSubmissionState.STORED) {
//                        this.recipientTimestamps[address] = timestamp
//                    }
//                }
//                .map { update -> if (update.state === AtomSubmissionState.STORED) "Sent you 10 Test Rads!" else "Couldn't send you any (Reason: $update)" }
//                .map { replyMessage -> SimpleImmutableEntry(message.from, replyMessage) }
//                .onErrorReturn { throwable -> SimpleImmutableEntry(message.from, "Couldn't send you any (Reason: " + throwable.message + ")") }
//    }
//
//    /**
//     * Actually send a reply message to the requestor through the Universe
//     *
//     * @param reply reply to send to requestor
//     * @return state of the message atom submission
//     */
//    private fun sendReply(reply: Map.Entry<RadixAddress, String>): Observable<AtomSubmissionUpdate> {
//        return RadixMessaging.instance.sendMessage(reply.value, radixIdentity, reply.key)
//                .doOnNext { state -> println("Message: $state") }
//    }
//
//    /**
//     * Start the faucet service
//     */
//    fun run() {
//        println("Faucet Address: $sourceAddress")
//
//        // Print out current balance of faucet
//        RadixWallet.instance.getSubUnitBalance(sourceAddress, Asset.XRD)
//                .subscribe(
//                        { balance -> println("Faucet Balance: " + balance as Double / Asset.XRD.subUnits) },
//                        { it.printStackTrace() }
//                )
//
//        // Flow Logic
//        // Listen to any recent messages, send 10 XRD to the sender and then send a confirmation whether it succeeded or not
//        // NOTE: this is neither idempotent nor atomic!
//        this.recentMessages
//                .flatMapSingle<SimpleImmutableEntry<RadixAddress, String>>({ this.leakFaucet(it) }, true)
//                .flatMap<AtomSubmissionUpdate> { this.sendReply(it) }
//                .subscribe()
//    }
//
//    companion object {
//
//        /**
//         * The amount of time a requestor must wait to make subsequent token requests
//         */
//        private val DELAY = (1000 * 60 * 10).toLong() //10min
//
//        @Throws(IOException::class)
//        @JvmStatic
//        fun main(args: Array<String>) {
//            if (args.size < 2) {
//                println("Usage: java com.radixdlt.client.services.Faucet <highgarden|sunstone.json|winterfell|winterfell_local> <keyfile>")
//                System.exit(-1)
//            }
//
//            RadixUniverse.bootstrap(Bootstrap.valueOf(args[0].toUpperCase()))
//
//            RadixUniverse.instance
//                    .network
//                    .getStatusUpdates()
//                    .subscribe { println(it) }
//
//            val faucetIdentity = SimpleRadixIdentity(args[1])
//            val faucet = Faucet(faucetIdentity)
//            faucet.run()
//        }
//    }
//}
