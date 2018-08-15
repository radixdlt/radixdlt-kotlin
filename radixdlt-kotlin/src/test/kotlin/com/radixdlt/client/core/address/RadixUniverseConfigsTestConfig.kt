package com.radixdlt.client.core.address

import org.junit.Test

import org.junit.Assert.assertNotNull

class RadixUniverseConfigsTestConfig {

    @Test
    fun createDevelopmentUniverseFromJson() {
        val winterfell = RadixUniverseConfigs.winterfell
        assertNotNull(winterfell)
    }
}