package com.radixdlt.client.core.atoms

import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.pow.ProofOfWorkBuilder
import java.util.Objects

class AtomFeeConsumableBuilder {
    private var owner: ECPublicKey? = null
    private var magic: Int = 0
    private var leading: Int = 0
    private var unsignedAtom: UnsignedAtom? = null

    fun pow(magic: Int, leading: Int): AtomFeeConsumableBuilder {
        this.magic = magic
        this.leading = leading
        return this
    }

    fun atom(atom: UnsignedAtom): AtomFeeConsumableBuilder {
        this.unsignedAtom = atom
        return this
    }

    fun owner(owner: ECPublicKey): AtomFeeConsumableBuilder {
        this.owner = owner
        return this
    }

    fun build(): AtomFeeConsumable {
        Objects.requireNonNull<UnsignedAtom>(unsignedAtom)
        Objects.requireNonNull<ECPublicKey>(owner)

        val seed = unsignedAtom!!.rawAtom.hash.toByteArray()

        val pow = ProofOfWorkBuilder().build(magic, seed, leading)

        return AtomFeeConsumable(
            pow.nonce,
            AccountReference(owner!!),
            System.nanoTime(),
            Token.POW.id,
            System.currentTimeMillis() * 60000
        )
    }
}
