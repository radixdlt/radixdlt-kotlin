package com.radixdlt.client.core.network

import com.google.gson.JsonElement
import com.radixdlt.client.core.atoms.Atom
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone

class AtomSubmissionUpdate private constructor(
    val atom: Atom,
    private val state: AtomSubmissionState,
    val data: JsonElement?
) {
    private val metaData = HashMap<String, Any>()
    val timestamp: Long = System.currentTimeMillis()

    // Needed to allow unit testing to pass
    // TODO: research if alternative
    fun getState(): AtomSubmissionState {
        return state
    }

    val isComplete: Boolean
        get() = this.getState().isComplete

    enum class AtomSubmissionState(val isComplete: Boolean) {
        SUBMITTING(false),
        SUBMITTED(false),
        FAILED(true),
        STORED(true),
        COLLISION(true),
        ILLEGAL_STATE(true),
        UNSUITABLE_PEER(true),
        VALIDATION_ERROR(true),
        UNKNOWN_ERROR(true)
    }

    fun putMetaData(key: String, value: Any): AtomSubmissionUpdate {
        metaData[key] = value
        return this
    }

    fun getMetaData(key: String): Any? {
        return metaData[key]
    }

    fun getMetaData(): Map<String, Any> {
        return Collections.unmodifiableMap(metaData)
    }

    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return "${sdf.format(Date(timestamp))} atom ${atom.hid} $state${if (data != null) ": $data" else ""}"
    }

    companion object {

        fun create(atom: Atom, code: AtomSubmissionState): AtomSubmissionUpdate {
            return AtomSubmissionUpdate(atom, code, null)
        }

        fun create(atom: Atom, code: AtomSubmissionState, data: JsonElement?): AtomSubmissionUpdate {
            return AtomSubmissionUpdate(atom, code, data)
        }
    }
}
