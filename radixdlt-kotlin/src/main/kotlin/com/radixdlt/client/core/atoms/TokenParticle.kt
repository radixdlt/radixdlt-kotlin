package com.radixdlt.client.core.atoms

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

class TokenParticle(
    private val owners: Set<ECKeyPair>,
    @field:SerializedName("sub_units")
    private val subUnits: Long,
    @field:SerializedName("maximum_units")
    private val iso: String,
    private val label: String,
    private val description: String,
    private val icon: ByteArray
) : Particle {

    private val spin: Spin = Spin.UP
    private val id: EUID = Asset.calcEUID(iso)

    // TODO: fix this to be an account
    override fun getDestinations(): Set<EUID> {
        return setOf(id)
    }

    override fun getSpin(): Spin {
        return spin
    }
}
