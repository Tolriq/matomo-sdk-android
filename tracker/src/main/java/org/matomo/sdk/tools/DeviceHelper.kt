/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.tools

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
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
                httpAgent = String.format(Locale.US,
                        "Dalvik/%s (Linux; U; Android %s; %s Build/%s)",
                        dalvik, android, model, build
                )
            }
            return httpAgent
        }// This is not accurate on all 4.2+ devices, usually the height is wrong due to statusbar/softkeys
    // Better than nothing though.
// Reflection bad, still this is the best way to get an accurate screen size on API14-16.// Recommended way to get the resolution but only available since API17
    /**
     * Tries to get the most accurate device resolution.
     * On devices below API17 resolution might not account for statusbar/softkeys.
     *
     * @return [width, height]
     */
    @get:TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    val resolution: IntArray?
        get() {
            var width = -1
            var height = -1
            val display: Display
            display = try {
                val wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.defaultDisplay
            } catch (e: NullPointerException) {
                Timber.tag(TAG).e(e, "Window service was not available from this context")
                return null
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // Recommended way to get the resolution but only available since API17
                val dm = DisplayMetrics()
                display.getRealMetrics(dm)
                width = dm.widthPixels
                height = dm.heightPixels
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                // Reflection bad, still this is the best way to get an accurate screen size on API14-16.
                try {
                    val getRawWidth = Display::class.java.getMethod("getRawWidth")
                    val getRawHeight = Display::class.java.getMethod("getRawHeight")
                    width = getRawWidth.invoke(display) as Int
                    height = getRawHeight.invoke(display) as Int
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Reflection of getRawWidth/getRawHeight failed on API14-16 unexpectedly.")
                }
            }
            if (width == -1 || height == -1) {
                // This is not accurate on all 4.2+ devices, usually the height is wrong due to statusbar/softkeys
                // Better than nothing though.
                val dm = DisplayMetrics()
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