package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECKeyPair
import org.bouncycastle.util.encoders.Base64
import java.util.Collections

class NullAtom(owners: Set<RadixAddress>, junk: ByteArray, timestamp: Long) : Atom(
    owners.asSequence().map(RadixAddress::getUID).toSet(),
    Collections.singletonList(JunkParticle(junk, owners)),
    timestamp
) {

    internal class JunkParticle(internal val junk: ByteArray, owners: Set<RadixAddress>) : Particle(
        owners.asSequence().map(RadixAddress::getUID).toSet(),
        owners.asSequence().map(RadixAddress::publicKey).map(::ECKeyPair).toSet()
    )

    fun getJunk(): String {
        return Base64.toBase64String((particles?.get(0) as JunkParticle).junk)
    }
}
