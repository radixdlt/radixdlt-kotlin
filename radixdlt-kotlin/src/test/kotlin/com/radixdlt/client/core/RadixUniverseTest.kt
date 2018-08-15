package com.radixdlt.client.core

import org.junit.Test

import org.junit.Assert.assertNotNull

class RadixUniverseTest {

    @Test(expected = IllegalStateException::class)
    fun testRadixUniverseCreationWithoutInitialization() {
        RadixUniverse.instance
    }

    @Test
    fun testRadixUniverseCreation() {
        RadixUniverse.bootstrap(Bootstrap.WINTERFELL)
        val universe = RadixUniverse.instance
        assertNotNull(universe)
        assertNotNull(universe.systemPublicKey)
    }
}