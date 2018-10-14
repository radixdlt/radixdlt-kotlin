package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.atoms.particles.TokenParticle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class TokenReducerTest {
    @Test
    fun testTokenWithNoMint() {
        val tokenParticle = mock<TokenParticle>()
        val tokenRef = mock<TokenRef>()
        whenever(tokenParticle.tokenRef).thenReturn(tokenRef)
        whenever(tokenParticle.name).thenReturn("Name")
        whenever(tokenParticle.iso).thenReturn("ISO")
        whenever(tokenParticle.description).thenReturn("Desc")

        val tokenReducer = TokenReducer()
        val state: Map<TokenRef, TokenState> = tokenReducer.reduce(emptyMap(), tokenParticle)
        assertThat(state[tokenRef])
            .isEqualToComparingFieldByField(TokenState("Name", "ISO", "Desc", BigDecimal.ZERO))
    }

    @Test
    fun testTokenWithMint() {
        val tokenParticle = mock<TokenParticle>()
        val tokenRef = mock<TokenRef>()
        whenever(tokenParticle.tokenRef).thenReturn(tokenRef)
        whenever(tokenParticle.name).thenReturn("Name")
        whenever(tokenParticle.iso).thenReturn("ISO")
        whenever(tokenParticle.description).thenReturn("Desc")

        val minted = mock<Consumable>()
        whenever(minted.amount).thenReturn(100L)
        whenever(minted.type).thenReturn(Consumable.ConsumableType.MINTED)
        whenever(minted.tokenRef).thenReturn(tokenRef)
        whenever(minted.getSpin()).thenReturn(Spin.UP)

        val tokenReducer = TokenReducer()
        val state1: Map<TokenRef, TokenState> = tokenReducer.reduce(emptyMap(), tokenParticle)
        val state2: Map<TokenRef, TokenState> = tokenReducer.reduce(state1, minted)
        assertThat(state2[tokenRef])
            .isEqualToComparingFieldByField(
                TokenState("Name", "ISO", "Desc", TokenRef.subUnitsToDecimal(100L))
            )
    }
}
