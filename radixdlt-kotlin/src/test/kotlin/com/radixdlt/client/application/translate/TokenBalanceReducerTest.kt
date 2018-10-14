package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Spin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TokenBalanceReducerTest {

    @Test
    fun testSimpleBalance() {
        val consumable = mock<Consumable>()
        val hash = mock<RadixHash>()
        whenever(consumable.getSignedAmount()).thenReturn(10L)
        whenever(consumable.amount).thenReturn(10L)
        whenever(consumable.getHash()).thenReturn(hash)
        whenever(consumable.getSpin()).thenReturn(Spin.UP)
        whenever(consumable.getDson()).thenReturn(byteArrayOf(1))
        val token = mock<TokenRef>()
        whenever(consumable.tokenRef).thenReturn(token)

        val reducer = TokenBalanceReducer()
        val tokenBalance = reducer.reduce(TokenBalanceState(), consumable)
        assertThat(tokenBalance.getBalance()[token]!!.amount.compareTo(TokenRef.subUnitsToDecimal(10L))).isEqualTo(0)
    }
}
