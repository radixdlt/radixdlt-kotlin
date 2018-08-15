package com.radixdlt.client.core

import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.network.PeerDiscovery

interface BootstrapConfig {
    fun getConfig(): RadixUniverseConfig
    fun getDiscovery(): PeerDiscovery
}