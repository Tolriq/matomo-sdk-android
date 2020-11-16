/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.extra

import android.app.Application
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder

abstract class MatomoApplication : Application() {
    private var mMatomoTracker: Tracker? = null
    val matomo: Matomo
        get() = Matomo.getInstance(this)

    /**
     * Gives you an all purpose thread-safe persisted Tracker.
     *
     * @return a shared Tracker
     */
    @get:Synchronized
    val tracker: Tracker?
        get() {
            if (mMatomoTracker == null) mMatomoTracker = onCreateTrackerConfig().build(matomo)
            return mMatomoTracker
        }

    /**
     * See [TrackerBuilder].
     * You may be interested in [TrackerBuilder.createDefault]
     *
     * @return the tracker configuration you want to use.
     */
    abstract fun onCreateTrackerConfig(): TrackerBuilder
    override fun onLowMemory() {
        mMatomoTracker?.dispatch()
        super.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        if ((level == TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_COMPLETE)) {
            mMatomoTracker?.dispatch()
        }
        super.onTrimMemory(level)
    }
}