package com.radixdlt.client.assets

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.math.BigDecimal

class AmountTest {
    @Test
    fun testBigDecimal() {
        val asset = mock(Asset::class.java)
        `when`<Int>(asset.subUnits).thenReturn(1)
        assertThatThrownBy { Amount.of(BigDecimal("1.1"), asset) }.isInstanceOf(IllegalArgumentException::class.java)
        assertEquals(Amount.of(BigDecimal("1.00"), asset), Amount.of(1, asset))
    }

    @Test
    fun testXRD() {
        assertEquals("0 TEST", Amount.subUnitsOf(0, Asset.TEST).toString())
        assertEquals("0.00001 TEST", Amount.subUnitsOf(1, Asset.TEST).toString())
        assertEquals("0.1 TEST", Amount.subUnitsOf(10000, Asset.TEST).toString())
        assertEquals("1.1 TEST", Amount.subUnitsOf(110000, Asset.TEST).toString())
        assertEquals("1.23456 TEST", Amount.subUnitsOf(123456, Asset.TEST).toString())
    }

    @Test
    fun testPOW() {
        assertEquals("0 POW", Amount.subUnitsOf(0, Asset.POW).toString())
        assertEquals("11 POW", Amount.subUnitsOf(11, Asset.POW).toString())
        assertEquals("12345 POW", Amount.subUnitsOf(12345, Asset.POW).toString())
    }

    @Test
    fun testUnusualSubUnits() {
        // 1 foot = 12 inches
        val foot = Asset("FOOT", 12)
        assertEquals("0 FOOT", Amount.subUnitsOf(0, foot).toString())
        assertEquals("1/12 FOOT", Amount.subUnitsOf(1, foot).toString())
        assertEquals("6/12 FOOT", Amount.subUnitsOf(6, foot).toString())
        assertEquals("1 FOOT", Amount.subUnitsOf(12, foot).toString())
        assertEquals("1 and 6/12 FOOT", Amount.subUnitsOf(18, foot).toString())
        assertEquals("1 and 8/12 FOOT", Amount.subUnitsOf(20, foot).toString())
    }
}
