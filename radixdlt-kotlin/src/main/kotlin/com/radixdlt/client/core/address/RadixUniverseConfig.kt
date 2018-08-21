package com.radixdlt.client.core.address

import com.google.gson.JsonObject
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.RadixJson
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections

class RadixUniverseConfig
internal constructor(
    genesis: List<Atom>,
    private val port: Int,
    private val name: String,
    private val description: String,
    private val type: RadixUniverseType,
    private val timestamp: Long,
    val creator: ECPublicKey,
    val magic: Int
) {

    val genesis: List<Atom> = Collections.unmodifiableList(genesis)

    val magicByte: Byte
        get() = (magic and 0xff).toByte()

    val systemAddress: RadixAddress
        get() = RadixAddress(this, creator)

    fun toJson(): JsonObject {
        val universe = JsonObject()
        universe.addProperty("magic", magic)
        universe.addProperty("port", port)
        universe.addProperty("name", name)
        universe.addProperty("description", description)
        universe.add("type", RadixJson.gson.toJsonTree(type))
        universe.addProperty("timestamp", timestamp)
        universe.add("creator", RadixJson.gson.toJsonTree(creator))
        universe.add("genesis", RadixJson.gson.toJsonTree(genesis))

        return universe
    }

    override fun toString(): String {
        return name
    }

    override fun hashCode(): Int {
        // TODO: fix this
        return (magic.toString() + ":" + port + ":" + name + ":" + timestamp).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is RadixUniverseConfig) {
            return false
        }

        val c = other as RadixUniverseConfig?
        if (magic != c!!.magic) {
            return false
        }
        if (port != c.port) {
            return false
        }
        if (name != c.name) {
            return false
        }
        if (type != c.type) {
            return false
        }
        if (timestamp != c.timestamp) {
            return false
        }
        return creator == c.creator
    }

    companion object {
        @JvmStatic
        fun fromInputStream(inputStream: InputStream): RadixUniverseConfig {
            return RadixJson.gson.fromJson(InputStreamReader(inputStream), RadixUniverseConfig::class.java)
        }
    }
}
