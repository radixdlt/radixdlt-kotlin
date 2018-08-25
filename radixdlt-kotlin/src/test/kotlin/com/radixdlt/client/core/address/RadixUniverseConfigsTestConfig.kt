package com.radixdlt.client.core.address

import org.junit.Assert.assertNotNull
import org.junit.Test

class RadixUniverseConfigsTestConfig {

    @Test
    fun createDevelopmentUniverseFromJson() {
        val betanet = RadixUniverseConfigs.betanet
        assertNotNull(betanet)
    }
}
