package com.radixdlt.client.core

import org.junit.Assert.assertNotNull
import org.junit.Test

class RadixUniverseTest {

    @Test(expected = IllegalStateException::class)
    fun testRadixUniverseCreationWithoutInitialization() {
        RadixUniverse.getInstance()
    }

    @Test
    fun testRadixUniverseCreation() {
        RadixUniverse.bootstrap(Bootstrap.BETANET)
        val universe = RadixUniverse.getInstance()
        assertNotNull(universe)
        assertNotNull(universe.systemPublicKey)
    }
}
