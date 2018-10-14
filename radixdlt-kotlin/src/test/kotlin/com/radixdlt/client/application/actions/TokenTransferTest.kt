package com.radixdlt.client.application.actions

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TokenRef
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal

class TokenTransferTest {

    @Test
    fun testBadBigDecimalScale() {
        val from = mock(RadixAddress::class.java)
        val to = mock(RadixAddress::class.java)
        val tokenRef = mock(TokenRef::class.java)

        assertThatThrownBy { TransferTokens.create(from, to, BigDecimal("0.000001"), tokenRef) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun testSmallestAllowedAmount() {
        val from = mock(RadixAddress::class.java)
        val to = mock(RadixAddress::class.java)
        val tokenRef = mock(TokenRef::class.java)

        assertThat(TransferTokens.create(from, to, BigDecimal("0.00001"), tokenRef).toString()).isNotNull()
    }

    @Test
    fun testSmallestAllowedAmountLargeScale() {
        val from = mock(RadixAddress::class.java)
        val to = mock(RadixAddress::class.java)
        val tokenRef = mock(TokenRef::class.java)

        assertThat(TransferTokens.create(from, to, BigDecimal("0.000010000"), tokenRef).toString()).isNotNull()
    }
}
