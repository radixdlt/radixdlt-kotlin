package com.radixdlt.client.core.atoms

import java.util.Objects
import java.util.TreeMap

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
    private val metaData: Map<String, Any?>?
) {

    init {
        Objects.requireNonNull(bytes)
    }

    class DataParticleBuilder {
        private val metaData = TreeMap<String, Any?>()
        private var bytes: Payload? = null

        fun setMetaData(key: String, value: Any?): DataParticleBuilder {
            metaData[key] = value
            return this
        }

        fun payload(bytes: Payload): DataParticleBuilder {
            this.bytes = bytes
            return this
        }

        fun build(): DataParticle {
            return DataParticle(bytes, if (metaData.isEmpty()) null else metaData)
        }
    }

    fun getMetaData(key: String): Any? {
        return if (metaData == null) {
            null
        } else metaData[key]
    }
}
