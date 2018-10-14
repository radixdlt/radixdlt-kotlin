package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.particles.Particle

interface ParticleReducer<T> {
    fun initialState(): T
    fun reduce(state: T, p: Particle): T
}
