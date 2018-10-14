package com.radixdlt.client.application.translate

import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.pow.ProofOfWorkBuilder
import java.util.Objects

class PowFeeMapper(
    private val hasher: (List<Particle>) -> (RadixHash),
    private val powBuilder: ProofOfWorkBuilder
) : FeeMapper {

    override fun map(particles: List<Particle>?, universe: RadixUniverse, key: ECPublicKey?): List<Particle> {
        Objects.requireNonNull(key)
        Objects.requireNonNull(universe)
        Objects.requireNonNull(particles)

        val seed = hasher(particles!!).toByteArray()
        val pow = powBuilder.build(universe.magic, seed, LEADING)

        val fee = AtomFeeConsumable(
            pow.nonce,
            AccountReference(key!!),
            System.nanoTime(),
            universe.powToken,
            System.currentTimeMillis() * 60000
        )

        return listOf<Particle>(fee)
    }

    companion object {
        private const val LEADING = 16
    }
}
