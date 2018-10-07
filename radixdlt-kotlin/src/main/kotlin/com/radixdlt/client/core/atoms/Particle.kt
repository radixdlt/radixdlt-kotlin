package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID

interface Particle {
    fun getSpin(): Long
    fun getDestinations(): Set<EUID>
}
