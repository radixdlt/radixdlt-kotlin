package com.radixdlt.client.messaging

import com.radixdlt.client.core.address.RadixAddress

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RadixMessage(
        val from: RadixAddress,
        val to: RadixAddress,
        val content: String,
        val timestamp: Long
) {

    override fun toString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()

        return "Time: " + sdf.format(Date(timestamp)) + "\nFrom: " + from + "\nTo: " + to + "\nContent: " + content
    }
}
