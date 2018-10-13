package com.radixdlt.client.assets

import com.radixdlt.client.application.objects.Amount
import com.radixdlt.client.application.objects.Token
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountTest {
    @Test
    fun testXRD() {
        assertEquals("0 XRD", Amount.subUnitsOf(0, Token.TEST).toString())
        assertEquals("0.00001 XRD", Amount.subUnitsOf(1, Token.TEST).toString())
        assertEquals("0.1 XRD", Amount.subUnitsOf(10000, Token.TEST).toString())
        assertEquals("1.1 XRD", Amount.subUnitsOf(110000, Token.TEST).toString())
        assertEquals("1.23456 XRD", Amount.subUnitsOf(123456, Token.TEST).toString())
    }

    @Test
    fun testPOW() {
        assertEquals("0 POW", Amount.subUnitsOf(0, Token.POW).toString())
        assertEquals("0.00011 POW", Amount.subUnitsOf(11, Token.POW).toString())
        assertEquals("0.12345 POW", Amount.subUnitsOf(12345, Token.POW).toString())
    }
}
