package com.radixdlt.client.application.objects

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.atoms.TokenReference
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountTest {
    @Test
    fun testXRD() {
        val token = mock<TokenReference>()
        whenever(token.iso).thenReturn("XRD")

        assertEquals("0 XRD", Amount.subUnitsOf(0, token).toString())
        assertEquals("0.00001 XRD", Amount.subUnitsOf(1, token).toString())
        assertEquals("0.1 XRD", Amount.subUnitsOf(10000, token).toString())
        assertEquals("1.1 XRD", Amount.subUnitsOf(110000, token).toString())
        assertEquals("1.23456 XRD", Amount.subUnitsOf(123456, token).toString())
    }
}
