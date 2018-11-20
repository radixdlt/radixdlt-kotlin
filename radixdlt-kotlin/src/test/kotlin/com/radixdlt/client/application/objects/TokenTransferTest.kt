package com.radixdlt.client.application.objects

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TokenTransferTest {

    @Test
    fun testNoAttachment() {
        val from = mock<RadixAddress>()
        val to = mock<RadixAddress>()
        val token = mock<Asset>()
        whenever(token.subUnits).thenReturn(1)
        val tokenTransfer = TokenTransfer(from, to, token, 1, null, 1L)
        assertThat(tokenTransfer.toString()).isNotNull()
        assertThat(tokenTransfer.attachment).isNull()
        assertThat(tokenTransfer.attachmentAsString).isNull()
    }

    @Test
    fun testWithAttachment() {
        val from = mock<RadixAddress>()
        val to = mock<RadixAddress>()
        val token = mock<Asset>()
        whenever(token.subUnits).thenReturn(1)
        val attachment = mock<UnencryptedData>()
        whenever(attachment.data).thenReturn("Hello".toByteArray())
        val tokenTransfer = TokenTransfer(from, to, token, 1, attachment, 1L)
        assertThat(tokenTransfer.toString()).isNotNull()
        assertThat(tokenTransfer.attachment).isNotNull
        assertThat(tokenTransfer.attachmentAsString).isEqualTo("Hello")
    }
}
