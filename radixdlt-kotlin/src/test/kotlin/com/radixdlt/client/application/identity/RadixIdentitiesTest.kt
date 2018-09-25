package com.radixdlt.client.application.identity

import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.io.Writer

class RadixIdentitiesTest {

    @Test
    @Throws(Exception::class)
    fun newEncryptedIdentityWriterTest() {
        val writer = mock(Writer::class.java)
        RadixIdentities.createNewEncryptedIdentity(writer, "")
        verify(writer, times(1)).flush()
    }
}
