package com.radixdlt.client.assets

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetAmountTest {

    @Test
    fun testXRD() {
        assertEquals("0 TEST", AssetAmount(Asset.XRD, 0).toString())
        assertEquals("0.00001 TEST", AssetAmount(Asset.XRD, 1).toString())
        assertEquals("0.1 TEST", AssetAmount(Asset.XRD, 10000).toString())
        assertEquals("1.1 TEST", AssetAmount(Asset.XRD, 110000).toString())
        assertEquals("1.23456 TEST", AssetAmount(Asset.XRD, 123456).toString())
    }

    @Test
    fun testPOW() {
        assertEquals("0 POW", AssetAmount(Asset.POW, 0).toString())
        assertEquals("11 POW", AssetAmount(Asset.POW, 11).toString())
        assertEquals("12345 POW", AssetAmount(Asset.POW, 12345).toString())
    }

    @Test
    fun testUnusualSubUnits() {
        // 1 foot = 12 inches
        val foot = Asset("FOOT", 12)
        assertEquals("0 FOOT", AssetAmount(foot, 0).toString())
        assertEquals("1/12 FOOT", AssetAmount(foot, 1).toString())
        assertEquals("6/12 FOOT", AssetAmount(foot, 6).toString())
        assertEquals("1 FOOT", AssetAmount(foot, 12).toString())
        assertEquals("1 and 6/12 FOOT", AssetAmount(foot, 18).toString())
        assertEquals("1 and 8/12 FOOT", AssetAmount(foot, 20).toString())
    }
}
