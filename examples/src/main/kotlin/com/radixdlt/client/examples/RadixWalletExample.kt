package com.radixdlt.client.examples

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.identity.RadixIdentities
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.dapps.wallet.RadixWallet

object RadixWalletExample {

    private val TO_ADDRESS_BASE58 = "JGuwJVu7REeqQtx7736GB9AJ91z5xB55t8NvteaoC25AumYovjp"
    // private val TO_ADDRESS_BASE58 = null;
    private val AMOUNT: Long = 1
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
