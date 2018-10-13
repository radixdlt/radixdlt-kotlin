package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.crypto.ECPublicKey

interface Particle {
    fun getSpin(): Spin
    fun getAddresses(): Set<ECPublicKey>
}
