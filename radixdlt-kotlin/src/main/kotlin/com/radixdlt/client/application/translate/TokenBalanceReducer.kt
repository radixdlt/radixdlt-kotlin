package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Particle

/**
 * Reduces particles at an address to it's token balances
 */
class TokenBalanceReducer : ParticleReducer<TokenBalanceState> {
    override fun initialState(): TokenBalanceState {
        return TokenBalanceState()
    }

    override fun reduce(state: TokenBalanceState, p: Particle): TokenBalanceState {
        return if (p !is Consumable || p is AtomFeeConsumable) state else TokenBalanceState.merge(state, p)
    }
}
