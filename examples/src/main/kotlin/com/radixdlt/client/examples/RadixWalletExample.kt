package com.radixdlt.client.examples

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.Bootstrap
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.identity.SimpleRadixIdentity
import com.radixdlt.client.wallet.RadixWallet

object RadixWalletExample {

    private const val TO_ADDRESS_BASE58 = "9fXrpiDg65UUpDMLKq5eKNd2nCULWchugeBc1ob1Y8vAtSbV2Jx"
    //	private static String TO_ADDRESS_BASE58 = null;
    private const val PAYLOAD = "A gift for you!"
    private const val AMOUNT: Long = 1

    // Initialize Radix Universe
    init {
        RadixUniverse.bootstrap(Bootstrap.ALPHANET)
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        // Network updates
        RadixUniverse.instance
                .network
                .getStatusUpdates()
                .subscribe { println(it) }

        // Identity Manager which manages user's keys, signing, encrypting and decrypting
        val radixIdentity: RadixIdentity = if (args.isNotEmpty()) {
            if (args[0] == "-system") {
                RadixUniverse.instance.systemIdentity ?: throw IllegalStateException("System key not present")
            } else {
                SimpleRadixIdentity(args[0])
            }
        } else {
            SimpleRadixIdentity()
        }
        val myAddress = RadixUniverse.instance.getAddressFrom(radixIdentity.getPublicKey())
        println(RadixAddress.fromString(myAddress.toString()))

        // Print out all past and future transactions
        RadixWallet.instance
                .getXRDTransactions(myAddress)
                .subscribe { println(it) }

        // Subscribe to current and future total balance
        RadixWallet.instance
                .getXRDSubUnitBalance(myAddress)
                .subscribe { balance -> println("My Balance: ${balance / Asset.XRD.subUnits}") }


        // If specified, send money to another address
        @Suppress("SENSELESS_COMPARISON")
        if (TO_ADDRESS_BASE58 != null) {
            val toAddress = RadixAddress.fromString(TO_ADDRESS_BASE58)
            RadixWallet.instance
                    .transferXRDWhenAvailable(AMOUNT * Asset.XRD.subUnits, radixIdentity, toAddress, PAYLOAD)
                    .subscribe(
                            { status -> println("Transaction $status") },
                            { it.printStackTrace() }
                    )
        }
    }
}
