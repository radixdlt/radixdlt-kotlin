package com.radixdlt.client.core.atoms.particles

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.crypto.ECPublicKey

class TokenParticle(
    accountReference: AccountReference,
    val name: String?,
    val iso: String,
    val description: String?,
    @field:SerializedName("mint_permissions")
    private val mintPermissions: MintPermissions,
    private val icon: ByteArray?
) : Particle {

    // FIXME: bad hack
//    private val uid: EUID = RadixHash.of(Dson.instance.toDson(tokenRef)).toEUID()
    private val spin: Spin = Spin.UP
    private val addresses: List<AccountReference> = listOf(accountReference)

    val tokenRef: TokenRef?
        get() = TokenRef.of(addresses[0], iso)

    enum class MintPermissions {
        GENESIS_ONLY,
        SAME_ATOM_ONLY,
        POW
    }

    override fun getAddresses(): Set<ECPublicKey> {
        return setOf(addresses[0].getKey())
    }

    override fun getSpin(): Spin {
        return spin
    }
}
