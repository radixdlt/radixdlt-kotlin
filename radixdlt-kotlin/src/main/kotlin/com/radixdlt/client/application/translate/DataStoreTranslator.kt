package com.radixdlt.client.application.translate

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.DataParticle
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.crypto.Encryptor
import io.reactivex.Completable
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.HashMap

class DataStoreTranslator private constructor() {

    // TODO: figure out correct method signature here (return Single<AtomBuilder> instead?)
    fun translate(dataStore: DataStore, atomBuilder: AtomBuilder): Completable {
        val payload = Payload(dataStore.data.bytes)
        val application = dataStore.data.getMetaData()["application"] as String?

        val dataParticle = DataParticle.DataParticleBuilder()
            .payload(payload)
            .setMetaData("application", application)
            .build()
        atomBuilder.addDataParticle(dataParticle)
        val encryptor = dataStore.data.encryptor
        if (encryptor != null) {
            val protectorsJson = JsonArray()
            encryptor.protectors.asSequence().map(EncryptedPrivateKey::base64).forEach(protectorsJson::add)

            val encryptorPayload = Payload(protectorsJson.toString().toByteArray(StandardCharsets.UTF_8))
            val encryptorParticle = DataParticle.DataParticleBuilder()
                .payload(encryptorPayload)
                .setMetaData("application", "encryptor")
                .setMetaData("contentType", "application/json")
                .build()
            atomBuilder.addDataParticle(encryptorParticle)
        }
        dataStore.getAddresses().forEach { atomBuilder.addDestination(it) }

        return Completable.complete()
    }

    // TODO: don't pass in maps, utilize a metadata builder?
    fun fromAtom(atom: Atom): Any {

        // This is here to pass the test, can be removed in the future
        if (atom.dataParticles == null) {
            return Any()
        }

        val bytesParticle: DataParticle = atom.dataParticles!!.asSequence()
            .filter { p -> "encryptor" != p.getMetaData("application") }
            .firstOrNull() ?: return Any()

        val metaData = HashMap<String, Any?>()
        metaData["timestamp"] = atom.timestamp
        metaData["signatures"] = atom.signatures

        val application: String? = bytesParticle.getMetaData("application") as String?
        metaData.computeSynchronisedFunction("application") { _, _ -> application }

        val encryptorParticle: DataParticle? = atom.dataParticles!!.asSequence()
            .filter { p -> "encryptor" == p.getMetaData("application") }
            .firstOrNull()
        metaData["encrypted"] = encryptorParticle != null

        val encryptor: Encryptor?
        encryptor = if (encryptorParticle != null) {
            val protectorsJson = JSON_PARSER.parse(encryptorParticle.bytes!!.toUtf8()).asJsonArray
            val protectors = ArrayList<EncryptedPrivateKey>()
            protectorsJson.forEach { protectorJson ->
                protectors.add(EncryptedPrivateKey.fromBase64(protectorJson.asString))
            }
            Encryptor(protectors)
        } else {
            null
        }

        return Data.raw(bytesParticle.bytes?.bytes, metaData, encryptor)
    }

    companion object {
        @JvmStatic
        val instance = DataStoreTranslator()

        private val JSON_PARSER = JsonParser()
    }
}

// Prevent null values
fun <K, V> HashMap<K, V>.computeSynchronisedFunction(key: K, remappingFunction: (t: K, u: V?) -> V) {
    return synchronized(this) {
        if (this[key] != null) {
            val valueSynchronized = remappingFunction(key, this[key])
            this[key] = valueSynchronized
        }
    }
}
