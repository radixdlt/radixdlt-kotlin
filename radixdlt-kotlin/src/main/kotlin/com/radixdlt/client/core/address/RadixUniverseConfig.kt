package com.radixdlt.client.core.address

import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson
import com.radixdlt.client.core.serialization.RadixJson
import org.bouncycastle.util.encoders.Base64
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections

class RadixUniverseConfig
internal constructor(
    genesis: List<Atom>,
    private val port: Long,
    private val name: String,
    private val description: String,
    private val type: RadixUniverseType,
    private val timestamp: Long,
    val creator: ECPublicKey,
    private val magic: Long
) {

    val genesis: List<Atom> = Collections.unmodifiableList(genesis)

    // TODO: should this be Long?
    fun getMagic(): Int = magic.toInt()

    fun getMagicByte(): Byte = (getMagic() and 0xff).toByte()

    fun getSystemAddress(): RadixAddress = RadixAddress(this, creator)

    fun getHash(): RadixHash = RadixHash.of(Dson.instance.toDson(this))

    override fun toString(): String = "$name ${getMagic()}"

    override fun hashCode(): Int = getHash().hashCode()

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is RadixUniverseConfig) {
            return false
        }

        return this.getHash() == other.getHash()
    }

    companion object {
        @JvmStatic
        fun fromInputStream(inputStream: InputStream): RadixUniverseConfig {
            return RadixJson.gson.fromJson(InputStreamReader(inputStream), RadixUniverseConfig::class.java)
        }

        @JvmStatic
        fun fromDsonBase64(dsonBase64: String): RadixUniverseConfig {
            val universeJson = Dson.instance.parse(Base64.decode(dsonBase64))
            println(universeJson)
            return RadixJson.gson.fromJson(universeJson, RadixUniverseConfig::class.java)
        }
    }
}
