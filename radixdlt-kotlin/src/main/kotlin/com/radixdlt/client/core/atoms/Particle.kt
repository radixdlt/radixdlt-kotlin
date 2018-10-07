package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID

interface Particle {
    fun getSpin(): Spin
    fun getDestinations(): Set<EUID>
}
