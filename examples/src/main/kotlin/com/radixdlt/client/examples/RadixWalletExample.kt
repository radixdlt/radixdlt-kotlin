package com.radixdlt.client.examples

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.identity.SimpleRadixIdentity
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.dapps.wallet.RadixWallet

object RadixWalletExample {

    private const val TO_ADDRESS_BASE58 = "JFgcgRKq6GbQqP8mZzDRhtr7K7YQM1vZiYopZLRpAeVxcnePRXX"
    //	private static String TO_ADDRESS_BASE58 = null;
    private const val AMOUNT: Long = 1
    private const val MESSAGE = "A gift for you!"

    // Initialize Radix Universe
    init {
        RadixUniverse.bootstrap(Bootstrap.BETANET)
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Network updates
        RadixUniverse.getInstance()
            .network
            .getStatusUpdates()
            .subscribe { println(it) }

        // Identity Manager which manages user's keys, signing, encrypting and decrypting
        val radixIdentity: RadixIdentity
        if (args.size > 0) {
            radixIdentity = SimpleRadixIdentity(args[0])
        } else {
            radixIdentity = SimpleRadixIdentity()
        }

        val api = RadixApplicationAPI.create(radixIdentity)
        val wallet = RadixWallet(api)

        // Print out all past and future transactions
        wallet.getXRDTransactions()
            .subscribe { println(it) }

        // Subscribe to current and future total balance
        wallet.getXRDBalance()
            .subscribe { balance -> println("My Balance: $balance") }

        // If specified, send money to another myAddress
        @Suppress("SENSELESS_COMPARISON")
        if (TO_ADDRESS_BASE58 != null) {
            val toAddress = RadixAddress.fromString(TO_ADDRESS_BASE58)
            wallet.transferXRDWhenAvailable(AMOUNT * Asset.TEST.subUnits, toAddress, MESSAGE)
                .toObservable()
                .subscribe(System.out::println, Throwable::printStackTrace)
        }
    }
}
