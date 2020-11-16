/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher

import org.matomo.sdk.TrackMe

/**
 * Responsible for transmitting packets to a server
 */
interface Dispatcher {
    /**
     * Connection timeout in milliseconds
     *
     */
    var connectionTimeOut: Int

    /**
     * Packets are collected and dispatched in batches, this intervals sets the pause between batches.
     *
     */
    var dispatchInterval: Long

    /**
     * Packets are collected and dispatched in batches. This boolean sets if post must be
     * gzipped or not. Use of gzip needs mod_deflate/Apache ou lua_zlib/NGINX
     *
     */
    var dispatchGzipped: Boolean

    var dispatchMode: DispatchMode

    /**
     * Starts the dispatcher for one cycle if it is currently not working.
     * If the dispatcher is working it will skip the dispatch interval once.
     */
    fun forceDispatch(): Boolean

    /**
     * Dispatch all events in the EventCache and return only after the dispatch is complete.
     *
     * This method may be invoked while the Runtime is being torn down and should not start new threads.
     */
    fun forceDispatchBlocking()

    /**
     * To clear the dispatchers queue
     */
    fun clear()

    /**
     * Submit for transmission
     */
    fun submit(trackMe: TrackMe?)

    /**
     * For debugging purposes
     * Mind thread-safety!
     *
     * When this is non null then instead of sending data over the network it will be written into this list.
     * Mind thread-safety!
     */
    var dryRunTarget: MutableList<Packet>?

    companion object {
        const val DEFAULT_CONNECTION_TIMEOUT = 5 * 1000
        const val DEFAULT_DISPATCH_INTERVAL = (120 * 1000).toLong()
    }
}
