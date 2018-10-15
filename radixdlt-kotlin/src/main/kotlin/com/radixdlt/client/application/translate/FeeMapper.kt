package com.radixdlt.client.application.translate

import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.crypto.ECPublicKey

interface FeeMapper {
    fun map(particles: List<Particle>, universe: RadixUniverse, key: ECPublicKey): List<Particle>
}
