package com.radixdlt.client.core.network

import com.radixdlt.client.core.address.EUID
import java.text.SimpleDateFormat
import java.util.*

class AtomSubmissionUpdate(private val hid: EUID, val state: AtomSubmissionState, val message: String?, val timestamp: Long) {

    val isComplete: Boolean
        get() = state.isComplete

    enum class AtomSubmissionState(val isComplete: Boolean) {
        SUBMITTING(false),
        SUBMITTED(false),
        FAILED(true),
        STORED(true),
        COLLISION(true),
        ILLEGAL_STATE(true),
        UNSUITABLE_PEER(true),
        VALIDATION_ERROR(true),
        UNKNOWN_FAILURE(true)
    }

    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return "${sdf.format(Date(timestamp))} atom $hid $state${if (message != null) ": $message" else ""}"
    }

    companion object {

        @JvmStatic
        fun now(hid: EUID, code: AtomSubmissionState): AtomSubmissionUpdate {
            return AtomSubmissionUpdate(hid, code, null, System.currentTimeMillis())
        }

        @JvmStatic
        fun now(hid: EUID, code: AtomSubmissionState, message: String?): AtomSubmissionUpdate {
            return AtomSubmissionUpdate(hid, code, message, System.currentTimeMillis())
        }
    }
}
