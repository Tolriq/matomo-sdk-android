/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher

import org.json.JSONObject

/**
 * Data that can be send to the backend API via the Dispatcher
 */
/**
 * Constructor for POST requests
 *
 * @param targetURL  server
 * @param postData non null if HTTP POST packet
 * @param eventCount number of events in this packet
 */
class Packet @JvmOverloads constructor(val targetURL: String,
                                       /**
                                        * @return may be null if it is a GET request
                                        */
                                       val postData: JSONObject? = null,
                                       /**
                                        * Used to determine the event cache queue positions.
                                        *
                                        * @return how many events this packet contains
                                        */
                                       val eventCount: Int = 1) {
    /**
     * A timestamp to use when replaying offline data
     */
    val timeStamp: Long = System.currentTimeMillis()

    override fun toString(): String {
        val sb = StringBuilder("Packet(")
        if (postData != null) sb.append("type=POST, data=").append(postData) else sb.append("type=GET, data=").append(targetURL)
        return sb.append(")").toString()
    }
}
