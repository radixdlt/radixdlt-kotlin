package com.radixdlt.client.core

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.core.network.PeerDiscovery
import com.radixdlt.client.core.network.RadixNetwork

/**
 * A RadixUniverse represents the interface through which a client can interact
 * with a Radix Universe (an instance of a Radix Ledger + Radix Network).
 *
 *
 * The configuration file of a Radix Universe defines the genesis atoms of the
 * distributed getLedger and distinguishes this universe from other universes.
 * (e.g. Public net vs Test net) It is shared amongst all participants of this
 * instance of the Radix getNetwork.
 *
 *
 * The network interface is available to directly connect with Node Runners in
 * the Radix Network in order to read/write atoms on the distributed ledger.
 *
 *
 * The ledger interface is a thin wrapper on the network interface which provides
 * an abstraction which doesn't require managing network peers. It can for example
 * be used to cache atoms locally.
 */
class RadixUniverse private constructor(
    /**
     * Universe Configuration
     */
    val config: RadixUniverseConfig,
    /**
     * Network Interface
     */
    val network: RadixNetwork,
    /**
     * Ledger Interface
     */
    val ledger: RadixLedger
) {

    val magic: Int
        get() = config.magic

    /**
     * Returns the system public key, also defined as the creator of this Universe
     *
     * @return the system public key
     */
    val systemPublicKey: ECPublicKey
        get() = config.creator

    /**
     * Maps a public key to it's corresponding Radix myAddress in this universe.
     * Within a universe, a public key has a one to one bijective relationship to an myAddress
     *
     * @param publicKey the key to get an myAddress from
     * @return the corresponding myAddress to the key for this universe
     */
    fun getAddressFrom(publicKey: ECPublicKey): RadixAddress {
        return RadixAddress(config, publicKey)
    }

    /**
     * Attempts to gracefully free all resources associated with this Universe
     */
    fun disconnect() {
        ledger.close()
        network.close()
    }

    companion object {

        /**
         * Lock to protect default Radix Universe instance
         */
        private val lock = Any()

        /**
         * Default Universe Instance
         */
        private var defaultUniverse: RadixUniverse? = null

        /**
         * Initializes the default universe with a Peer Discovery mechanism.
         * Should only be called once at the start of the program.
         *
         * @param peerDiscovery The peer discovery mechanism
         * @return The default universe created, can also be retrieved with RadixUniverse.getInstance()
         */
        fun bootstrap(config: RadixUniverseConfig, peerDiscovery: PeerDiscovery): RadixUniverse {
            synchronized(lock) {
                if (defaultUniverse != null) {
                    throw IllegalStateException("Default Universe already bootstrapped")
                }

                val network = RadixNetwork(peerDiscovery)
                val ledger = RadixLedger(config.magic, network)

                defaultUniverse = RadixUniverse(config, network, ledger)

                return defaultUniverse!!
            }
        }

        @JvmStatic
        fun bootstrap(bootstrapConfig: BootstrapConfig): RadixUniverse? {
            return bootstrap(bootstrapConfig.getConfig(), bootstrapConfig.getDiscovery())
        }

        /**
         * Returns the default RadixUniverse instance
         * @return the default RadixUniverse instance
         */
        @JvmStatic
        fun getInstance(): RadixUniverse {
            synchronized(lock) {
                if (defaultUniverse == null) {
                    throw IllegalStateException("Default Universe was not initialized via RadixUniverse.bootstrap()")
                }
                return defaultUniverse!!
            }
        }
    }
}
