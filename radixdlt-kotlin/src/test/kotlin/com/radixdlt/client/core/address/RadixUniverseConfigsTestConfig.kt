package com.radixdlt.client.core.address

import org.junit.Test

import org.junit.Assert.assertNotNull

class RadixUniverseConfigsTestConfig {

    @Test
    fun createDevelopmentUniverseFromJson() {
        val betanet = RadixUniverseConfigs.betanet
        assertNotNull(betanet)
    }
}