package com.radixdlt.client.core.atoms

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.address.EUID

class TokenParticle(
    accountReference: AccountReference,
    private val name: String,
    private val iso: String,
    private val description: String,
    @field:SerializedName("sub_units")
    private val subUnits: Long,
    private val icon: ByteArray?
) : Particle {
    private val uid: EUID = Token.calcEUID(iso)
    private val spin: Spin = Spin.UP
    private val addresses: List<AccountReference> = listOf(accountReference)

    // TODO: fix this to be an account
    override fun getDestinations(): Set<EUID> {
        return setOf(uid)
    }

    override fun getSpin(): Spin {
        return spin
    }
}
