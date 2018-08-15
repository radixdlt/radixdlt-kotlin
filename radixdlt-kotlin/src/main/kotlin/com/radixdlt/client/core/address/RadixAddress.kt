package com.radixdlt.client.core.address

import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.util.Base58
import java.util.*

class RadixAddress {

    // The Base58 address string
    private val addressBase58: String
    @Transient
    val publicKey: ECPublicKey

    constructor(addressBase58: String) {
        val raw = Base58.fromBase58(addressBase58)
        val check = RadixHash.of(raw, 0, raw.size - 4)
        for (i in 0..3) {
            if (check.get(i) != raw[raw.size - 4 + i]) {
                throw IllegalArgumentException("Address $addressBase58 checksum mismatch")
            }
        }

        val publicKey = ByteArray(raw.size - 5)
        System.arraycopy(raw, 1, publicKey, 0, raw.size - 5)

        this.addressBase58 = addressBase58
        this.publicKey = ECPublicKey(publicKey)
    }

    constructor(universe: RadixUniverseConfig, publicKey: ECPublicKey) : this(universe.magic, publicKey)

    constructor(magic: Int, publicKey: ECPublicKey) {
        Objects.requireNonNull(publicKey)
        if (publicKey.length() != 33) {
            throw IllegalArgumentException("Public key must be 33 bytes but was " + publicKey.length())
        }

        val addressBytes = ByteArray(1 + publicKey.length() + 4)
        // Universe magic byte
        addressBytes[0] = (magic and 0xff).toByte()
        // Public Key
        publicKey.copyPublicKey(addressBytes, 1)
        // Checksum
        val check = RadixHash.of(addressBytes, 0, publicKey.length() + 1).toByteArray()
        System.arraycopy(check, 0, addressBytes, publicKey.length() + 1, 4)

        this.addressBase58 = Base58.toBase58(addressBytes)
        this.publicKey = publicKey
    }

    fun ownsKey(ecKeyPair: ECKeyPair): Boolean {
        return this.ownsKey(ecKeyPair.getPublicKey())
    }

    fun ownsKey(publicKey: ECPublicKey): Boolean {
        return this.publicKey == publicKey
    }

    override fun toString(): String {
        return addressBase58
    }

    fun getUID(): EUID {
        return publicKey.getUID()
    }

    fun toECKeyPair(): ECKeyPair {
        return ECKeyPair(publicKey)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is RadixAddress) {
            return false
        }

        val o = other as RadixAddress?
        return o!!.addressBase58 == this.addressBase58
    }

    override fun hashCode(): Int {
        return addressBase58.hashCode()
    }

    companion object {
        @JvmStatic
        fun fromString(addressBase58: String): RadixAddress {
            return RadixAddress(addressBase58)
        }
    }
}
