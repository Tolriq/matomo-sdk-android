/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.tools

import android.content.Context
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import org.matomo.sdk.Matomo
import timber.log.Timber
import java.util.Locale

/**
 * Helper class to gain information about the device we are running on
 */
class DeviceHelper(private val mContext: Context, private val mPropertySource: PropertySource, private val mBuildInfo: BuildInfo) {
    /**
     * Returns user language
     *
     * @return language
     */
    val userLanguage: String
        get() = Locale.getDefault().language

    /**
     * Returns android system user agent
     *
     * @return well formatted user agent
     */
    val userAgent: String
        get() {
            var httpAgent = mPropertySource.httpAgent
            if (httpAgent == null || httpAgent.startsWith("Apache-HttpClient/UNAVAILABLE (java")) {
                var dalvik = mPropertySource.jvmVersion
                if (dalvik == null) dalvik = "0.0.0"
                val android = mBuildInfo.release
                val model = mBuildInfo.model
                val build = mBuildInfo.buildId
                httpAgent = String.format(Locale.US, "Dalvik/%s (Linux; U; Android %s; %s Build/%s)", dalvik, android, model, build)
            }
            return httpAgent
        }

    /**
     * Tries to get the most accurate device resolution.
     * On devices below API17 resolution might not account for statusbar/softkeys.
     *
     * @return [width, height]
     */
    val resolution: IntArray?
        get() {
            var width: Int
            var height: Int
            val display: Display
            display = try {
                val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.defaultDisplay
            } catch (e: NullPointerException) {
                Timber.tag(TAG).e(e, "Window service was not available from this context")
                return null
            }
            // Recommended way to get the resolution but only available since API17
            var dm = DisplayMetrics()
            display.getRealMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels
            if (width == -1 || height == -1) {
                // This is not accurate on all 4.2+ devices, usually the height is wrong due to statusbar/softkeys
                // Better than nothing though.
                dm = DisplayMetrics()
                display.getMetrics(dm)
                width = dm.widthPixels
                height = dm.heightPixels
            }
            return intArrayOf(width, height)
        }

    companion object {
        private val TAG = Matomo.tag(DeviceHelper::class.java)
    }
}