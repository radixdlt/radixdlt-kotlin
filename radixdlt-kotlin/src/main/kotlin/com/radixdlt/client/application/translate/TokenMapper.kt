package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.CreateFixedSupplyToken
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Minted
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.TokenParticle
import com.radixdlt.client.core.atoms.particles.TokenParticle.MintPermissions
import java.util.Arrays

class TokenMapper {
    fun map(tokenCreation: CreateFixedSupplyToken?): List<Particle> {
        if (tokenCreation ==
            null
        ) {
            return emptyList()
        }

        val token = TokenParticle(
            tokenCreation.accountReference,
            tokenCreation.name,
            tokenCreation.iso,
            tokenCreation.description,
            MintPermissions.SAME_ATOM_ONLY,
            null
        )
        val minted = Minted(
            tokenCreation.fixedSupply * TokenRef.SUB_UNITS,
            tokenCreation.accountReference,
            System.currentTimeMillis(),
            token.tokenRef!!,
            System.currentTimeMillis() / 60000L + 60000
        )

        return Arrays.asList(token, minted)
    }
}
