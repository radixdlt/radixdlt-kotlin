package com.radixdlt.client.services

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.IdParticle
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.identity.SimpleRadixIdentity
import com.radixdlt.client.messaging.RadixMessage
import com.radixdlt.client.messaging.RadixMessaging
import com.radixdlt.client.wallet.RadixWallet
import io.reactivex.Observable
import java.io.IOException
import java.nio.ByteBuffer

class MultiSigWallet private constructor(private val radixIdentity: RadixIdentity, private val keyA: ECPublicKey, private val keyB: ECPublicKey) {

    private val allTransactions: Observable<RadixMessage> = RadixMessaging.instance
            .getAllMessagesDecrypted(radixIdentity)
            .filter { it.validateSignature() }
            .publish()
            .autoConnect(2)

    inner class SignedRequest(internal val to: RadixAddress, internal val amount: Long, nonce: Long) {
        private val euid: EUID

        init {

            val pub = to.publicKey.toByteArray()
            val unique = ByteBuffer
                    .allocate(pub.size + 8 + 8)
                    .put(pub)
                    .putLong(amount)
                    .putLong(nonce)
                    .array()

            this.euid = RadixHash.of(unique).toEUID()
        }

        fun toJson(): String {
            return gson.toJson(this)
        }

        fun hash(): EUID {
            return euid
        }

        override fun hashCode(): Int {
            return euid.hashCode()
        }

        override fun equals(o: Any?): Boolean {
            val msg = o as SignedRequest?
            return euid == msg!!.euid
        }
    }

    private fun signedReqsFrom(key: ECPublicKey): Observable<SignedRequest> {
        return this.allTransactions.filter { txReq -> txReq.from.publicKey == key }
                .map { txReq -> gson.fromJson(txReq.getContent(), SignedRequest::class.java) }
                .doOnNext { txReq -> println("Tx " + txReq.hash() + " Signed By " + key) }
    }

    private fun uniqueId(euid: EUID): IdParticle {
        return IdParticle.create("multi-sig", euid, radixIdentity.getPublicKey())
    }

    fun run() {
        val address = RadixUniverse.instance.getAddressFrom(radixIdentity.getPublicKey())
        val wallet = RadixWallet.instance

        println("MultiSig Address: $address")

        wallet.getSubUnitBalance(address, Asset.XRD)
                .subscribe { balance -> println("Balance: " + balance!!) }

        RadixMessaging.instance
                .getAllMessagesDecrypted(radixIdentity)
                .filter { it.validateSignature() }
                .subscribe { println(it) }

        val signedFromA = signedReqsFrom(keyA)
        val signedFromB = signedReqsFrom(keyB).replay()

        signedFromA
                .flatMapSingle { txA -> signedFromB.filter { txB -> txA.hash() == txB.hash() }.firstOrError() }
                .doOnSubscribe { x -> signedFromB.connect() }
                .subscribe { tx -> wallet.transferXRD(tx.amount, radixIdentity, tx.to, uniqueId(tx.hash())) }
    }

    companion object {

        private val addressDeserializer = JsonDeserializer<RadixAddress> { json, typeOf, context -> RadixAddress(json.getAsString()) }
        private val addressSerializer = JsonSerializer<RadixAddress> { src, typeOf, context -> JsonPrimitive(src.toString()) }

        private val gson = GsonBuilder()
                .registerTypeAdapter(RadixAddress::class.java, addressDeserializer)
                .registerTypeAdapter(RadixAddress::class.java, addressSerializer)
                .create()

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {

            if (args.size < 2) {
                println("Usage: java com.radixdlt.client.services.MultiSig <highgarden|sunstone|winterfell|winterfell_local> <keyfile>")
                System.exit(-1)
            }

            RadixUniverse.bootstrap(Bootstrap.valueOf(args[0].toUpperCase()))

            RadixUniverse.instance
                    .network
                    .getStatusUpdates()
                    .subscribe { println(it) }

            val multiSigIdentity = SimpleRadixIdentity(args[1])
            val person1 = SimpleRadixIdentity("1.key")
            val person2 = SimpleRadixIdentity("2.key")

            val multiSigWallet = MultiSigWallet(multiSigIdentity, person1.getPublicKey(), person2.getPublicKey())
            multiSigWallet.run()
        }
    }
}
