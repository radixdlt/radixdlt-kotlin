package com.radixdlt.client.examples

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.identity.RadixIdentities
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.dapps.wallet.RadixWallet
import java.math.BigDecimal

object RadixWalletExample {

    private val TO_ADDRESS_BASE58 = "9ejksTjHEXJAPuSwUP1a9GDYNaRmUShJq5RgMkXQXgdHbdEkTbD"
    // private val TO_ADDRESS_BASE58 = null;
    private val AMOUNT = BigDecimal("100.0")
    private val MESSAGE = "A gift!"

    // Initialize Radix Universe
    init {
        RadixUniverse.bootstrap(Bootstrap.SUNSTONE)
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
        val wallet = RadixWallet(api)

        // Print out all past and future transactions
        wallet.getTransactions()
            .subscribe { println(it) }

        // Subscribe to current and future total balance
        wallet.getBalance()
            .subscribe { balance -> println("My Balance: $balance") }

        // If specified, send money to another myAddress
        @Suppress("SENSELESS_COMPARISON")
        if (TO_ADDRESS_BASE58 != null) {
            val toAddress = RadixAddress.fromString(TO_ADDRESS_BASE58)
            wallet.sendWhenAvailable(AMOUNT, toAddress, MESSAGE)
                .toObservable()
                .subscribe(System.out::println, Throwable::printStackTrace)
        }
    }
}
