package com.radixdlt.client.core.atoms

import java.util.HashMap
import java.util.Objects

/**
 * Particle which can hold arbitrary data
 */
class DataParticle(
    /**
     * Arbitrary data, possibly encrypted
     */
    val bytes: Payload?,
    application: String?
) {
    /**
     * Nullable for the timebeing as we want dson to be optimized for
     * saving space and no way to skip empty maps in Dson yet.
     */
    private val metaData: MutableMap<String, Any>?

    init {
        Objects.requireNonNull(bytes) // Not needed in Kotlin but for now keep as close to java and pass unit tests
        if (application != null) {
            this.metaData = HashMap()
            this.metaData["application"] = application
        } else {
            this.metaData = null
        }
    }

    fun getMetaData(key: String): Any? {
        return if (metaData == null) {
            null
        } else metaData[key]
    }
}
