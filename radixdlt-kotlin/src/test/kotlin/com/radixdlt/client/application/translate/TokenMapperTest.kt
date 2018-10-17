package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.actions.CreateFixedSupplyTokenAction
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.TokenParticle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TokenMapperTest {
    @Test
    fun testNormalConstruction() {
        val tokenCreation = mock<CreateFixedSupplyTokenAction>()
        val accountReference = mock<AccountReference>()
        whenever(tokenCreation.accountReference).thenReturn(accountReference)
        whenever(tokenCreation.iso).thenReturn("ISO")

        val tokenMapper = TokenMapper()
        val particles = tokenMapper.map(tokenCreation)
        assertThat(particles).hasAtLeastOneElementOfType(TokenParticle::class.java)
        assertThat(particles).hasAtLeastOneElementOfType(Consumable::class.java)
        assertThat(particles).hasSize(2)
    }
}
