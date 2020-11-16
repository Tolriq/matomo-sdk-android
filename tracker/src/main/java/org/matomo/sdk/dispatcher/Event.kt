package org.matomo.sdk.dispatcher

import org.matomo.sdk.Matomo
import timber.log.Timber
import java.net.URLEncoder

class Event(val timeStamp: Long, val encodedQuery: String) {

    constructor(eventData: Map<String, String>) : this(urlEncodeUTF8(eventData))
    constructor(query: String) : this(System.currentTimeMillis(), query)

    override fun toString(): String {
        return encodedQuery
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val event = other as Event
        return timeStamp == event.timeStamp && encodedQuery == event.encodedQuery
    }

    override fun hashCode(): Int {
        var result = (timeStamp xor (timeStamp ushr 32)).toInt()
        result = 31 * result + encodedQuery.hashCode()
        return result
    }

    companion object {
        private val TAG = Matomo.tag(Event::class.java)

        /**
         * http://stackoverflow.com/q/4737841
         *
         * @param param raw data
         * @return encoded string
         */
        private fun urlEncodeUTF8(param: String): String {
            return try {
                URLEncoder.encode(param, "UTF-8").replace("\\+".toRegex(), "%20")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Cannot encode %s", param)
                ""
            }
        }

        /**
         * URL encodes a key-value map
         */
        private fun urlEncodeUTF8(map: Map<String, String>): String {
            val sb = StringBuilder(100)
            sb.append('?')
            for ((key, value) in map) {
                sb.append(urlEncodeUTF8(key))
                sb.append('=')
                sb.append(urlEncodeUTF8(value))
                sb.append('&')
            }
            return sb.substring(0, sb.length - 1)
        }
    }
}
