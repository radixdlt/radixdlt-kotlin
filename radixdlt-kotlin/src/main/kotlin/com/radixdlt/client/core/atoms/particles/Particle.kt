package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.EUID

interface Particle {
    fun getSpin(): Spin
    fun getDestinations(): Set<EUID>
}
