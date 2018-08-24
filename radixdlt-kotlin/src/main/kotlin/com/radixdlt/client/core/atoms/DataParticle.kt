package com.radixdlt.client.core.atoms

/**
 * Particle which can hold arbitrary data
 */
class DataParticle(
    /**
     * Arbitrary data, possibly encrypted
     */
    val bytes: Payload,
    /**
     * Temporary property specifying the application this data particle
     * was meant for. Will change into some kind of metaData in the future.
     */
    val application: String?
)
