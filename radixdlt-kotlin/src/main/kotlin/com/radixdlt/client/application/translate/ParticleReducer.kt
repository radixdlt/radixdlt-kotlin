package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.particles.Particle

/**
 * Redux-like reducer where particles are the actions
 *
 * @param <T> The class of the state to manage
 */
interface ParticleReducer<T> {
    fun initialState(): T
    fun reduce(state: T, p: Particle): T
}
