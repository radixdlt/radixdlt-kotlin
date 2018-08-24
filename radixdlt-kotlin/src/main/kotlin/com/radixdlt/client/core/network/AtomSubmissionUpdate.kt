//package com.radixdlt.client.core.network

//import com.radixdlt.client.core.address.EUID
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.HashMap
//import java.util.Locale
//import java.util.TimeZone

//import com.radixdlt.client.core.address.EUID
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.TimeZone
//
//class AtomSubmissionUpdate private constructor(
//    private val hid: EUID,
//    private val state: AtomSubmissionState,
//    val message: String?
//) {
//
//    val isComplete: Boolean
//        get() = this.getState().isComplete
//
//    // Needed to allow unit testing to pass
//    // TODO: research if alternative
//    fun getState(): AtomSubmissionState {
//        return state
//    }
//
//    enum class AtomSubmissionState(val isComplete: Boolean) {
//        SUBMITTING(false),
//        SUBMITTED(false),
//        FAILED(true),
//        STORED(true),
//        COLLISION(true),
//        ILLEGAL_STATE(true),
//        UNSUITABLE_PEER(true),
//        VALIDATION_ERROR(true),
//        UNKNOWN_FAILURE(true)
//    }
//
//    override fun toString(): String {
//        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
//        sdf.timeZone = TimeZone.getDefault()
//
//        return "${sdf.format(Date(timestamp))} atom $hid $state${if (message != null) ": $message" else ""}"
//    }
//
//    companion object {
//
//        @JvmStatic
//        fun now(hid: EUID, code: AtomSubmissionState): AtomSubmissionUpdate {
//            return AtomSubmissionUpdate(hid, code, null, System.currentTimeMillis())
//        }
//
//        @JvmStatic
//        fun now(hid: EUID, code: AtomSubmissionState, message: String?): AtomSubmissionUpdate {
//            return AtomSubmissionUpdate(hid, code, message, System.currentTimeMillis())
//        }
//    }
//}

package com.radixdlt.client.core.network

import com.radixdlt.client.core.address.EUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.TimeZone

class AtomSubmissionUpdate private constructor(
    private val hid: EUID,
    private val state: AtomSubmissionState,
    val message: String?
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
        UNKNOWN_FAILURE(true)
    }

    fun putMetaData(key: String, value: Any): AtomSubmissionUpdate {
        metaData[key] = value
        return this
    }

    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(Date(timestamp)) + " atom " + hid + " " + state + (if (message != null) ": $message" else "") + " " + metaData
    }

    companion object {

        fun create(hid: EUID, code: AtomSubmissionState): AtomSubmissionUpdate {
            return AtomSubmissionUpdate(hid, code, null)
        }

        fun create(hid: EUID, code: AtomSubmissionState, message: String?): AtomSubmissionUpdate {
            return AtomSubmissionUpdate(hid, code, message)
        }
    }
}

