package com.radixdlt.client.application.objects

import com.radixdlt.client.application.objects.Data.DataBuilder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DataTest {
    @Test
    fun builderNoBytesTest() {
        val dataBuilder = DataBuilder()
        assertThatThrownBy { dataBuilder.build() }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun builderUnencryptedTest() {
        val data = DataBuilder().bytes(byteArrayOf()).unencrypted().build()
        assertEquals(0, data.bytes!!.size.toLong())
        assertNull(data.encryptor)
    }

    @Test
    fun builderNoReadersTest() {
        assertThatThrownBy { DataBuilder().bytes(byteArrayOf()).build() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
