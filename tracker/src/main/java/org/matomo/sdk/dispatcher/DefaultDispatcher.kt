/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.dispatcher

import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackMe
import org.matomo.sdk.tools.Connectivity
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Responsible for transmitting packets to a server
 */
class DefaultDispatcher(private val mEventCache: EventCache, private val mConnectivity: Connectivity, private val mPacketFactory: PacketFactory, private val mPacketSender: PacketSender) : Dispatcher {
    private val mThreadControl = Any()
    private val mSleepToken = Semaphore(0)

    @Volatile
    private var mTimeOut = Dispatcher.DEFAULT_CONNECTION_TIMEOUT

    @Volatile
    private var mDispatchInterval = Dispatcher.DEFAULT_DISPATCH_INTERVAL

    private var mDryRunTarget: MutableList<Packet>? = null

    @Volatile
    private var mRetryCounter = 0

    @Volatile
    private var mForcedBlocking = false
    private var mDispatchGzipped = false

    @Volatile
    override var dispatchMode: DispatchMode = DispatchMode.ALWAYS

    @Volatile
    private var mRunning = false

    @Volatile
    private var mDispatchThread: Thread? = null

    override var connectionTimeOut: Int
        get() = mTimeOut
        set(timeOut) {
            mTimeOut = timeOut
            mPacketSender.setTimeout(mTimeOut.toLong())
        }

    override var dispatchInterval: Long
        get() = mDispatchInterval
        set(dispatchInterval) {
            mDispatchInterval = dispatchInterval
            if (mDispatchInterval != -1L) launch()
        }

    override var dispatchGzipped: Boolean
        get() = mDispatchGzipped
        set(dispatchGzipped) {
            mDispatchGzipped = dispatchGzipped
            mPacketSender.setGzipData(mDispatchGzipped)
        }

    private fun launch(): Boolean {
        synchronized(mThreadControl) {
            if (!mRunning) {
                mRunning = true
                val thread = Thread(mLoop)
                thread.priority = Thread.MIN_PRIORITY
                thread.name = "Matomo-default-dispatcher"
                mDispatchThread = thread
                thread.start()
                return true
            }
        }
        return false
    }

    /**
     * Starts the dispatcher for one cycle if it is currently not working.
     * If the dispatcher is working it will skip the dispatch interval once.
     */
    override fun forceDispatch(): Boolean {
        if (!launch()) {
            mRetryCounter = 0
            mSleepToken.release()
            return false
        }
        return true
    }

    override fun forceDispatchBlocking() {
        synchronized(mThreadControl) {
            // force thread to exit after it completes its dispatch loop
            mForcedBlocking = true
        }
        if (forceDispatch()) {
            mSleepToken.release()
        }
        val dispatchThread = mDispatchThread
        if (dispatchThread != null) {
            try {
                dispatchThread.join()
            } catch (e: InterruptedException) {
                Timber.tag(TAG).d("Interrupted while waiting for dispatch thread to complete")
            }
        }
        synchronized(mThreadControl) {
            // re-enable default behavior
            mForcedBlocking = false
        }
    }

    override fun clear() {
        mEventCache.clear()
        // Try to exit the loop as the queue is empty
        if (mRunning) forceDispatch()
    }

    override fun submit(trackMe: TrackMe?) {
        mEventCache.add(Event(trackMe!!.toMap()))
        if (mDispatchInterval != -1L) launch()
    }

    private val mLoop = Runnable {
        mRetryCounter = 0
        while (mRunning) {
            try {
                var sleepTime = mDispatchInterval
                if (mRetryCounter > 1) sleepTime += (mRetryCounter * mDispatchInterval).coerceAtMost(5 * mDispatchInterval)

                // Either we wait the interval or forceDispatch() granted us one free pass
                mSleepToken.tryAcquire(sleepTime, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                Timber.tag(TAG).e(e)
            }
            if (mEventCache.updateState(isOnline)) {
                var count = 0
                val drainedEvents: MutableList<Event> = ArrayList()
                mEventCache.drainTo(drainedEvents)
                Timber.tag(TAG).d("Drained %s events.", drainedEvents.size)
                for (packet in mPacketFactory.buildPackets(drainedEvents)) {
                    val success: Boolean = if (mDryRunTarget != null) {
                        Timber.tag(TAG).d("DryRun, stored HttpRequest, now %d.", mDryRunTarget!!.size)
                        mDryRunTarget!!.add(packet)
                    } else {
                        mPacketSender.send(packet)
                    }
                    if (success) {
                        count += packet.eventCount
                        mRetryCounter = 0
                    } else {
                        // On network failure, requeue all un-sent events, but use isOnline to determine if events should be cached in
                        // memory or disk
                        Timber.tag(TAG).d("Failure while trying to send packet")
                        mRetryCounter++
                        break
                    }

                    // Re-check network connectivity to early exit if we drop offline.  This speeds up how quickly the setOffline method will
                    // take effect
                    if (!isOnline) {
                        Timber.tag(TAG).d("Disconnected during dispatch loop")
                        break
                    }
                }
                Timber.tag(TAG).d("Dispatched %d events.", count)
                if (count < drainedEvents.size) {
                    Timber.tag(TAG).d("Unable to send all events, requeueing %d events", drainedEvents.size - count)
                    // Requeue events to the event cache that weren't processed (either PacketSender failure or we are now offline).  Once the
                    // events are requeued we update the event cache state to write the requeued events to disk or to leave them in memory
                    // depending on the connectivity state of the device.
                    mEventCache.requeue(drainedEvents.subList(count, drainedEvents.size))
                    mEventCache.updateState(isOnline)
                }
            }
            synchronized(mThreadControl) {
                // We may be done or this was a forced dispatch.  If we are in a blocking force dispatch we need to exit immediately to ensure
                // the blocking doesn't take too long.
                if (mForcedBlocking || mEventCache.isEmpty || mDispatchInterval < 0) {
                    mRunning = false
                    return@Runnable
                }
            }
        }
    }
    private val isOnline: Boolean
        get() = if (!mConnectivity.isConnected) false else when (dispatchMode) {
            DispatchMode.EXCEPTION -> false
            DispatchMode.ALWAYS -> true
            DispatchMode.WIFI_ONLY -> mConnectivity.type == Connectivity.Type.WIFI
        }

    override var dryRunTarget: MutableList<Packet>?
        get() = mDryRunTarget
        set(value) {
            mDryRunTarget = value
        }

    companion object {
        private val TAG = Matomo.tag(DefaultDispatcher::class.java)
    }

    init {
        mPacketSender.setGzipData(mDispatchGzipped)
        mPacketSender.setTimeout(mTimeOut.toLong())
    }
}
