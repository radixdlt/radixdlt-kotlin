package com.radixdlt.client.core.atoms.particles

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.MetadataMap
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.ECPublicKey
import java.util.ArrayList
import java.util.Objects

/**
 * Particle which can hold arbitrary data
 */
class DataParticle private constructor(
    /**
     * Arbitrary data, possibly encrypted
     */
    val bytes: Payload?,
    /**
     * Nullable for the time being as we want dson to be optimized for
     * saving space and no way to skip empty maps in Dson yet.
     */
    private val metaData: MetadataMap?,
    private val addresses: List<AccountReference>
) : Particle {

    /**
     * Arbitrary data, possibly encrypted
     */
    private val spin = Spin.UP

    init {
        Objects.requireNonNull(bytes)
    }

    class DataParticleBuilder {
        private val addresses = ArrayList<AccountReference>()
        private val metaData = MetadataMap()
        private var bytes: Payload? = null

        fun setMetaData(key: String, value: String?): DataParticleBuilder {
            metaData[key] = value
            return this
        }

        fun payload(bytes: Payload): DataParticleBuilder {
            this.bytes = bytes
            return this
        }

        fun accounts(addresses: Collection<RadixAddress>): DataParticleBuilder {
            addresses.forEach { this.account(it) }
            return this
        }

        fun account(address: RadixAddress): DataParticleBuilder {
            addresses.add(AccountReference(address.publicKey))
            return this
        }

        fun build(): DataParticle {
            return DataParticle(
                bytes,
                if (metaData.isEmpty()) null else metaData,
                addresses
            )
        }
    }

    override fun getAddresses(): Set<ECPublicKey> {
        return addresses.asSequence().map(AccountReference::getKey).toSet()
    }

    override fun getSpin(): Spin {
        return spin
    }

    fun getMetaData(key: String): Any? {
        return if (metaData == null) {
            null
        } else metaData[key]
    }
}
