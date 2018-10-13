package com.radixdlt.client.core.atoms.particles

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.crypto.ECPublicKey

class TokenParticle(
    accountReference: AccountReference,
    private val name: String,
    private val iso: String,
    private val description: String,
    @field:SerializedName("mint_permissions")
    private val mintPermissions: MintPermissions,
    private val icon: ByteArray?
) : Particle {
    private val uid: EUID = Token.calcEUID(iso)
    private val spin: Spin = Spin.UP
    private val addresses: List<AccountReference> = listOf(accountReference)

    enum class MintPermissions {
        GENESIS_ONLY,
        SAME_ATOM_ONLY
    }

    // TODO: fix this to be an account
    override fun getAddresses(): Set<ECPublicKey> {
        return setOf(addresses[0].getKey())
    }

    override fun getSpin(): Spin {
        return spin
    }
}
