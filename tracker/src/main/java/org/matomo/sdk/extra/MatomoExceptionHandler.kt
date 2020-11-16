/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.extra

import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.dispatcher.DispatchMode
import timber.log.Timber

/**
 * An exception handler that wraps the existing exception handler and dispatches event to a [org.matomo.sdk.Tracker].
 *
 *
 * Also see documentation for [TrackHelper.uncaughtExceptions]
 */
class MatomoExceptionHandler(val tracker: Tracker, private val mTrackMe: TrackMe?) : Thread.UncaughtExceptionHandler {
    private val mDefaultExceptionHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    /**
     * This will give you the previous exception handler that is now wrapped.
     */
    private val defaultExceptionHandler: Thread.UncaughtExceptionHandler?
        get() = mDefaultExceptionHandler

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            val excInfo = ex.message
            val tracker = tracker

            // Force the tracker into offline mode to ensure events are written to disk
            tracker.dispatchMode = DispatchMode.EXCEPTION
            TrackHelper.track(mTrackMe).exception(ex).description(excInfo).fatal(true).with(tracker)

            // Immediately dispatch as the app might be dying after rethrowing the exception and block until the dispatch is completed
            tracker.dispatchBlocking()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Couldn't track uncaught exception")
        } finally {
            // re-throw critical exception further to the os (important)
            if (defaultExceptionHandler != null && defaultExceptionHandler !== this) {
                defaultExceptionHandler!!.uncaughtException(thread, ex)
            }
        }
    }

    companion object {
        private val TAG = Matomo.tag(MatomoExceptionHandler::class.java)
    }

}