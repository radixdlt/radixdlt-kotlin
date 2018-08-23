package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECKeyPair

class JunkParticle(private val junk: ByteArray, owners: Set<RadixAddress>) :
    Particle(owners.asSequence().map { it.getUID() }.toSet(),
        owners.asSequence().map { it.publicKey }.map { ECKeyPair(it) }.toSet()
    )
