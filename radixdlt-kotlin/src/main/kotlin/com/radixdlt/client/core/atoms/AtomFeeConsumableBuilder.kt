package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.pow.ProofOfWorkBuilder
import java.util.Objects

class AtomFeeConsumableBuilder {
    private var owner: ECPublicKey? = null
    private var magic: Int = 0
    private var leading: Int = 0
    private var atom: Atom? = null
    private var powToken: TokenRef? = null

    fun powToken(powToken: TokenRef): AtomFeeConsumableBuilder {
        this.powToken = powToken
        return this
    }

    fun pow(magic: Int, leading: Int): AtomFeeConsumableBuilder {
        this.magic = magic
        this.leading = leading
        return this
    }

    fun atom(atom: Atom): AtomFeeConsumableBuilder {
        this.atom = atom
        return this
    }

    fun owner(owner: ECPublicKey): AtomFeeConsumableBuilder {
        this.owner = owner
        return this
    }

    fun build(): AtomFeeConsumable {
        Objects.requireNonNull<Atom>(atom)
        Objects.requireNonNull<ECPublicKey>(owner)

        val seed = atom!!.hash.toByteArray()

        val pow = ProofOfWorkBuilder().build(magic, seed, leading)

        return AtomFeeConsumable(
            pow.nonce,
            AccountReference(owner!!),
            System.nanoTime(),
            powToken!!,
            System.currentTimeMillis() * 60000
        )
    }
}
