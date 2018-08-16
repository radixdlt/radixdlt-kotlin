package com.radixdlt.client.core

import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.address.RadixUniverseConfigs
import com.radixdlt.client.core.network.PeerDiscovery
import com.radixdlt.client.core.network.PeersFromNodeFinder
import com.radixdlt.client.core.network.PeersFromSeed
import com.radixdlt.client.core.network.RadixPeer

enum class Bootstrap(private val config: () -> RadixUniverseConfig, private val discovery: PeerDiscovery) : BootstrapConfig {
    BETANET(
            RadixUniverseConfigs::betanet,
            PeersFromSeed(RadixPeer("localhost", false, 8080))
    ),
    ALPHANET(
            RadixUniverseConfigs::alphanet,
            PeersFromNodeFinder("https://alphanet.radixdlt.com/node-finder", 443)
    ),
    HIGHGARDEN(
            RadixUniverseConfigs::highgarden,
            PeersFromNodeFinder("https://highgarden.radixdlt.com/node-finder", 443)
    ),
    SUNSTONE(
            RadixUniverseConfigs::sunstone,
            PeersFromNodeFinder("https://sunstone.radixdlt.com/node-finder", 443)
    ),
    WINTERFELL(
            RadixUniverseConfigs::winterfell,
            PeersFromSeed(RadixPeer("52.190.0.18", false, 8080))
    ),
    WINTERFELL_LOCAL(
            RadixUniverseConfigs::winterfell,
            PeersFromSeed(RadixPeer("localhost", false, 8080))
    );

    override fun getConfig(): RadixUniverseConfig {
        return config()
    }

    override fun getDiscovery(): PeerDiscovery {
        return discovery
    }
}