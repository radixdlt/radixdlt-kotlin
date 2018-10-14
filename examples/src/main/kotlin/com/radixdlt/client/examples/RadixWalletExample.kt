package com.radixdlt.client.examples

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.identity.RadixIdentities
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Amount
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.TokenReference
import java.math.BigDecimal

object RadixWalletExample {

    private val TO_ADDRESS_BASE58 = "9ejksTjHEXJAPuSwUP1a9GDYNaRmUShJq5RgMkXQXgdHbdEkTbD"
    // private val TO_ADDRESS_BASE58 = null
    private val MESSAGE = "A gift for you!"
    private val AMOUNT = BigDecimal("0.01")

    // Initialize Radix Universe
    init {
        RadixUniverse.bootstrap(Bootstrap.BETANET)
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Identity Manager which manages user's keys, signing, encrypting and decrypting
        val radixIdentity: RadixIdentity = if (args.isNotEmpty()) {
            RadixIdentities.loadOrCreateFile(args[0])
        } else {
            RadixIdentities.loadOrCreateFile("my.key")
        }

        // Network updates
        RadixUniverse.getInstance()
            .network
            .getStatusUpdates()
            .subscribe { println(it) }

        val api = RadixApplicationAPI.create(radixIdentity)
        api.pull()

        println("My address: " + api.myAddress)
        println("My public key: " + api.myPublicKey)

        // Print out all past and future transactions
        api.getMyTokenTransfers()
            .subscribe(::println)

        /*
		api.createFixedSupplyToken("Joshy Token", "YOSHY", "The Best Coin Ever", 10000)
			.toObservable().subscribe(::println)
		*/

        // If specified, send money to another address
        if (TO_ADDRESS_BASE58 != null) {
            val toAddress = RadixAddress.fromString(TO_ADDRESS_BASE58)
            val token = TokenReference.of(AccountReference(api.myPublicKey), "YOSHY")
            api.sendTokens(toAddress, Amount.of(AMOUNT, token)).toObservable()
                .subscribe(
                    { println(it) },
                    { it.printStackTrace() }
                )
        }
    }
}
