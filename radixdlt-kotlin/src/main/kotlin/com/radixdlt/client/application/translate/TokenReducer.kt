package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Minted
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.TokenParticle
import com.radixdlt.client.core.util.mergeAfterFunction
import java.math.BigDecimal
import java.util.HashMap

/**
 * Reduces particles at an address into concrete Tokens and their states
 */
class TokenReducer : ParticleReducer<Map<TokenRef, TokenState>> {
    override fun initialState(): Map<TokenRef, TokenState> {
        return emptyMap()
    }

    override fun reduce(state: Map<TokenRef, TokenState>, p: Particle): Map<TokenRef, TokenState> {
        if (!(p is TokenParticle || p is Minted)) {
            return state
        }

        val newMap = HashMap(state)
        if (p is TokenParticle) {
            val tokenState = TokenState(p.name, p.iso, p.description, BigDecimal.ZERO)

            newMap.mergeAfterFunction(p.tokenRef, tokenState) { a, b ->
                TokenState(b.name, b.iso, b.description, a.totalSupply)
            }

        } else {
            val minted = p as Minted
            val tokenState = TokenState(null, minted.tokenRef.iso, null, TokenRef.subUnitsToDecimal(minted.amount))

            newMap.mergeAfterFunction(minted.tokenRef, tokenState) { a, b ->
                TokenState(a.name, a.iso, a.description, a.totalSupply.add(b.totalSupply))
            }
        }

        return newMap
    }
}
