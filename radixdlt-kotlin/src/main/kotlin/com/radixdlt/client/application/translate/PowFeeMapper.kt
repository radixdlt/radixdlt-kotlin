package com.radixdlt.client.application.translate

import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomFeeConsumableBuilder
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.crypto.ECPublicKey

class PowFeeMapper : FeeMapper {
    override fun map(particles: List<Particle>?, universe: RadixUniverse, key: ECPublicKey?): List<Particle> {
        val atom = Atom(particles)
        val fee = AtomFeeConsumableBuilder()
            .powToken(universe.powToken)
            .atom(atom)
            .owner(key!!)
            .pow(universe.magic, 16)
            .build()
        return listOf<Particle>(fee)
    }
}
