package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.UniqueProperty
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.UniqueParticle

/**
 * Translates an application layer unique property object to an atom level object;
 */
class UniquePropertyTranslator {
    fun map(uniqueProperty: UniqueProperty?): List<Particle> {
        if (uniqueProperty == null) {
            return emptyList()
        }

        val payload = Payload(uniqueProperty.unique)
        val ecPublicKey = uniqueProperty.address.publicKey
        val uniqueParticle = UniqueParticle.create(payload, ecPublicKey)
        return listOf<Particle>(uniqueParticle)
    }
}
