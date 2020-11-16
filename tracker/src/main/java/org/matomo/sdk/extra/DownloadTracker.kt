package org.matomo.sdk.extra

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.WorkerThread
import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe
import org.matomo.sdk.Tracker
import org.matomo.sdk.tools.Checksum
import java.io.File

open class DownloadTracker @JvmOverloads constructor(private val mTracker: Tracker, packageInfo: PackageInfo = getOurPackageInfo(mTracker.matomo.context)) {
    private val mTrackOnceLock = Any()
    private val mPreferences: SharedPreferences = mTracker.preferences
    private var mVersion: String? = null
    private val mPkgInfo: PackageInfo = packageInfo

    interface Extra {
        /**
         * Does your [Extra] implementation do work intensive stuff?
         * Network? IO?
         *
         * @return true if this should be run async and on a sepperate thread.
         */
        val isIntensiveWork: Boolean

        /**
         * Example:
         * <br></br>
         * com.example.pkg:1/ABCDEF01234567
         * <br></br>
         * "ABCDEF01234567" is the extra identifier here.
         *
         * @return a string that will be used as extra identifier or null
         */
        fun buildExtraIdentifier(): String?

        /**
         * The MD5 checksum of the apk file.
         * com.example.pkg:1/ABCDEF01234567
         */
        class ApkChecksum : Extra {
            private var mPackageInfo: PackageInfo? = null

            constructor(context: Context) {
                mPackageInfo = try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: Exception) {
                    null
                }
            }

            constructor(packageInfo: PackageInfo?) {
                mPackageInfo = packageInfo
            }

            override val isIntensiveWork: Boolean = true

            override fun buildExtraIdentifier(): String? {
                if (mPackageInfo != null && mPackageInfo!!.applicationInfo != null && mPackageInfo!!.applicationInfo.sourceDir != null) {
                    try {
                        return Checksum.getMD5Checksum(File(mPackageInfo!!.applicationInfo.sourceDir))
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                return null
            }
        }

        /**
         * Custom exta identifier. Supply your own \o/.
         */
        abstract class Custom : Extra

        /**
         * No extra identifier.
         * com.example.pkg:1
         */
        class None : Extra {

            override val isIntensiveWork: Boolean = false

            override fun buildExtraIdentifier(): String? {
                return null
            }
        }
    }

    var version: String?
        get() = if (mVersion != null) mVersion else mPkgInfo.versionCode.toString()
        set(version) {
            mVersion = version
        }

    @WorkerThread
    fun trackOnce(baseTrackme: TrackMe, extra: Extra) {
        val firedKey = "downloaded:" + mPkgInfo.packageName + ":" + version
        synchronized(mTrackOnceLock) {
            if (!mPreferences.getBoolean(firedKey, false)) {
                mPreferences.edit().putBoolean(firedKey, true).apply()
                trackNewAppDownload(baseTrackme, extra)
            }
        }
    }

    @WorkerThread
    fun trackNewAppDownload(baseTrackme: TrackMe, extra: Extra) {
        trackNewAppDownloadInternal(baseTrackme, extra)
    }

    private fun trackNewAppDownloadInternal(baseTrackMe: TrackMe, extra: Extra) {
        val installIdentifier = StringBuilder()
        installIdentifier.append("http://").append(mPkgInfo.packageName).append(":").append(version)
        val extraIdentifier = extra.buildExtraIdentifier()
        if (extraIdentifier != null) installIdentifier.append("/").append(extraIdentifier)

        // Usual USEFUL values of this field will be: "com.android.vending" or "com.android.browser", i.e. app packagenames.
        // This is not guaranteed, values can also look like: app_process /system/bin com.android.commands.pm.Pm install -r /storage/sdcard0/...
        var referringApp = mTracker.matomo.context.packageManager.getInstallerPackageName(mPkgInfo.packageName)
        if (referringApp != null && referringApp.length > 200) referringApp = referringApp.substring(0, 200)
        if (referringApp != null) referringApp = "http://$referringApp"
        mTracker.track(baseTrackMe
                .set(QueryParams.EVENT_CATEGORY, "Application")
                .set(QueryParams.EVENT_ACTION, "downloaded")
                .set(QueryParams.ACTION_NAME, "application/downloaded")
                .set(QueryParams.URL_PATH, "/application/downloaded")
                .set(QueryParams.DOWNLOAD, installIdentifier.toString())
                .set(QueryParams.REFERRER, referringApp)) // Can be null in which case the TrackMe removes the REFERRER parameter.
    }

    companion object {
        private fun getOurPackageInfo(context: Context): PackageInfo {
            return try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                throw RuntimeException(e)
            }
        }
    }
}
