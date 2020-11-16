/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk

import android.content.SharedPreferences
import org.matomo.sdk.Matomo.Companion.tag
import org.matomo.sdk.dispatcher.DispatchMode
import org.matomo.sdk.dispatcher.DispatchMode.Companion.fromString
import org.matomo.sdk.dispatcher.Dispatcher
import org.matomo.sdk.dispatcher.Packet
import org.matomo.sdk.tools.Objects.equals
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashSet
import java.util.Locale
import java.util.Random
import java.util.UUID
import java.util.regex.Pattern

/**
 * Main tracking class
 * This class is threadsafe.
 */
class Tracker(val matomo: Matomo, config: TrackerBuilder) {
    val apiUrl: String = config.apiUrl
    val siteId: Int = config.siteId
    private val mDefaultApplicationBaseUrl: String = config.applicationBaseUrl
    private val mTrackingLock = Any()
    private val mDispatcher: Dispatcher
    val name: String = config.trackerName
    private val mRandomAntiCachingValue = Random(Date().time)

    /**
     * Matomo will use the content of this object to fill in missing values before any transmission.
     * While you can modify it's values, you can also just set them in your [TrackMe] object as already set values will not be overwritten.
     *
     * @return the default TrackMe object
     */
    val defaultTrackMe = TrackMe()

    /**
     * For testing purposes
     *
     * @return query of the event
     */
    var lastEventX: TrackMe? = null
        private set

    /**
     * Default is 30min (30*60*1000).
     *
     * @return session timeout value in miliseconds
     */
    var sessionTimeout = (30 * 60 * 1000).toLong()
        private set
    private var mSessionStartTime: Long = 0
    private var mOptOut: Boolean
    private val mTrackingCallbacks = LinkedHashSet<Callback>()
    private var mDispatchMode: DispatchMode? = null
    fun addTrackingCallback(callback: Callback) {
        mTrackingCallbacks.add(callback)
    }

    fun removeTrackingCallback(callback: Callback) {
        mTrackingCallbacks.remove(callback)
    }

    /**
     * Use this to disable this Tracker, e.g. if the user opted out of tracking.
     * The Tracker will persist the choice and remain disable on next instance creation.
     *
     */
    var isOptOut: Boolean
        get() = mOptOut
        set(optOut) {
            mOptOut = optOut
            preferences.edit().putBoolean(PREF_KEY_TRACKER_OPTOUT, optOut).apply()
            mDispatcher.clear()
        }

    fun startNewSession() {
        synchronized(mTrackingLock) { mSessionStartTime = 0 }
    }

    fun setSessionTimeout(milliseconds: Int) {
        synchronized(mTrackingLock) { sessionTimeout = milliseconds.toLong() }
    }

    /**
     * Processes all queued events in background thread
     */
    fun dispatch() {
        if (mOptOut) return
        mDispatcher.forceDispatch()
    }

    /**
     * Process all queued events and block until processing is complete
     */
    fun dispatchBlocking() {
        if (mOptOut) return
        mDispatcher.forceDispatchBlocking()
    }

    /**
     * Set the interval to 0 to dispatch events as soon as they are queued.
     * If a negative value is used the dispatch timer will never run, a manual dispatch must be used.
     *
     * @param dispatchInterval in milliseconds
     */
    fun setDispatchInterval(dispatchInterval: Long): Tracker {
        mDispatcher.dispatchInterval = dispatchInterval
        return this
    }

    /**
     * Defines if when dispatched, posted JSON must be Gzipped.
     * Need to be handle from web server side with mod_deflate/APACHE lua_zlib/NGINX.
     *
     * @param dispatchGzipped boolean
     */
    fun setDispatchGzipped(dispatchGzipped: Boolean): Tracker {
        mDispatcher.dispatchGzipped = dispatchGzipped
        return this
    }

    /**
     * @return in milliseconds
     */
    val dispatchInterval: Long
        get() = mDispatcher.dispatchInterval
    /**
     * See [.setOfflineCacheAge]
     *
     * @return maximum cache age in milliseconds
     */
    /**
     * For how long events should be stored if they could not be send.
     * Events older than the set limit will be discarded on the next dispatch attempt.<br></br>
     * The Matomo backend accepts backdated events for up to 24 hours by default.
     *
     *
     * &gt;0 = limit in ms<br></br>
     * 0 = unlimited<br></br>
     * -1 = disabled offline cache<br></br>
     *
     */
    var offlineCacheAge: Long
        get() = preferences.getLong(PREF_KEY_OFFLINE_CACHE_AGE, (24 * 60 * 60 * 1000).toLong())
        set(age) {
            preferences.edit().putLong(PREF_KEY_OFFLINE_CACHE_AGE, age).apply()
        }
    /**
     * Maximum size the offline cache is allowed to grow to.
     *
     * @return size in byte
     */
    /**
     * How large the offline cache may be.
     * If the limit is reached the oldest files will be deleted first.
     * Events older than the set limit will be discarded on the next dispatch attempt.<br></br>
     * The Matomo backend accepts backdated events for up to 24 hours by default.
     *
     *
     * &gt;0 = limit in byte<br></br>
     * 0 = unlimited<br></br>
     *
     */
    var offlineCacheSize: Long
        get() = preferences.getLong(PREF_KEY_OFFLINE_CACHE_SIZE, (4 * 1024 * 1024).toLong())
        set(size) {
            preferences.edit().putLong(PREF_KEY_OFFLINE_CACHE_SIZE, size).apply()
        }
    /**
     * The current dispatch behavior.
     *
     * @see DispatchMode
     */
    /**
     * Sets the dispatch mode.
     *
     * @see DispatchMode
     */
    var dispatchMode: DispatchMode
        get() {
            if (mDispatchMode == null) {
                val raw = preferences.getString(PREF_KEY_DISPATCHER_MODE, null)
                mDispatchMode = fromString(raw)
                if (mDispatchMode == null) mDispatchMode = DispatchMode.ALWAYS
            }
            return mDispatchMode!!
        }
        set(mode) {
            mDispatchMode = mode
            if (mode !== DispatchMode.EXCEPTION) {
                preferences.edit().putString(PREF_KEY_DISPATCHER_MODE, mode.toString()).apply()
            }
            mDispatcher.dispatchMode = mode
        }

    /**
     * Defines the User ID for this request.
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can
     * fetch this user ID from your system, and pass it to Matomo.
     *
     *
     * When specified, the User ID will be "enforced".
     * This means that if there is no recent visit with this User ID, a new one will be created.
     * If a visit is found in the last 30 minutes with your specified User ID,
     * then the new action will be recorded to this existing visit.
     *
     * @param userId passing null will delete the current user-id.
     */
    fun setUserId(userId: String?): Tracker {
        defaultTrackMe[QueryParams.USER_ID] = userId
        preferences.edit().putString(PREF_KEY_TRACKER_USERID, userId).apply()
        return this
    }

    /**
     * @return a user-id string, either the one you set or the one Matomo generated for you.
     */
    val userId: String?
        get() = defaultTrackMe[QueryParams.USER_ID]

    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Matomo will still track visits, but the unique visitors metric might be less accurate.
     */
    @Throws(IllegalArgumentException::class)
    fun setVisitorId(visitorId: String): Tracker {
        if (confirmVisitorIdFormat(visitorId)) defaultTrackMe[QueryParams.VISITOR_ID] = visitorId
        return this
    }

    val visitorId: String?
        get() = defaultTrackMe[QueryParams.VISITOR_ID]

    @Throws(IllegalArgumentException::class)
    private fun confirmVisitorIdFormat(visitorId: String): Boolean {
        if (PATTERN_VISITOR_ID.matcher(visitorId).matches()) return true
        throw IllegalArgumentException("VisitorId: " + visitorId + " is not of valid format, " +
                " the format must match the regular expression: " + PATTERN_VISITOR_ID.pattern())
    }

    /**
     * There parameters are only interesting for the very first query.
     */
    private fun injectInitialParams(trackMe: TrackMe?) {
        var firstVisitTime: Long
        var visitCount: Long
        var previousVisit: Long

        // Protected against Trackers on other threads trying to do the same thing.
        // This works because they would use the same preference object.
        synchronized(preferences) {
            visitCount = 1 + preferences.getLong(PREF_KEY_TRACKER_VISITCOUNT, 0)
            preferences.edit().putLong(PREF_KEY_TRACKER_VISITCOUNT, visitCount).apply()
        }
        synchronized(preferences) {
            firstVisitTime = preferences.getLong(PREF_KEY_TRACKER_FIRSTVISIT, -1)
            if (firstVisitTime == -1L) {
                firstVisitTime = System.currentTimeMillis() / 1000
                preferences.edit().putLong(PREF_KEY_TRACKER_FIRSTVISIT, firstVisitTime).apply()
            }
        }
        synchronized(preferences) {
            previousVisit = preferences.getLong(PREF_KEY_TRACKER_PREVIOUSVISIT, -1)
            preferences.edit().putLong(PREF_KEY_TRACKER_PREVIOUSVISIT, System.currentTimeMillis() / 1000).apply()
        }

        // trySet because the developer could have modded these after creating the Tracker
        defaultTrackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, firstVisitTime)
        defaultTrackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, visitCount)
        if (previousVisit != -1L) defaultTrackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, previousVisit)
        trackMe!!.trySet(QueryParams.SESSION_START, defaultTrackMe[QueryParams.SESSION_START])
        trackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, defaultTrackMe[QueryParams.FIRST_VISIT_TIMESTAMP])
        trackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, defaultTrackMe[QueryParams.TOTAL_NUMBER_OF_VISITS])
        trackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, defaultTrackMe[QueryParams.PREVIOUS_VISIT_TIMESTAMP])
    }

    /**
     * These parameters are required for all queries.
     */
    private fun injectBaseParams(trackMe: TrackMe?) {
        trackMe!!.trySet(QueryParams.SITE_ID, siteId)
        trackMe.trySet(QueryParams.RECORD, DEFAULT_RECORD_VALUE)
        trackMe.trySet(QueryParams.API_VERSION, DEFAULT_API_VERSION_VALUE)
        trackMe.trySet(QueryParams.RANDOM_NUMBER, mRandomAntiCachingValue.nextInt(100000))
        trackMe.trySet(QueryParams.DATETIME_OF_REQUEST, SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US).format(Date()))
        trackMe.trySet(QueryParams.SEND_IMAGE, "0")
        trackMe.trySet(QueryParams.VISITOR_ID, defaultTrackMe[QueryParams.VISITOR_ID])
        trackMe.trySet(QueryParams.USER_ID, defaultTrackMe[QueryParams.USER_ID])
        var urlPath = trackMe[QueryParams.URL_PATH]
        if (urlPath == null) {
            urlPath = defaultTrackMe[QueryParams.URL_PATH]
        } else if (!VALID_URLS.matcher(urlPath).matches()) {
            val urlBuilder = StringBuilder(mDefaultApplicationBaseUrl)
            if (!mDefaultApplicationBaseUrl.endsWith("/") && !urlPath.startsWith("/")) {
                urlBuilder.append("/")
            } else if (mDefaultApplicationBaseUrl.endsWith("/") && urlPath.startsWith("/")) {
                urlPath = urlPath.substring(1)
            }
            urlPath = urlBuilder.append(urlPath).toString()
        }

        // https://github.com/matomo-org/matomo-sdk-android/issues/92
        defaultTrackMe[QueryParams.URL_PATH] = urlPath
        trackMe[QueryParams.URL_PATH] = urlPath
        if (lastEventX == null || !equals(trackMe[QueryParams.USER_ID], lastEventX?.get(QueryParams.USER_ID))) {
            // https://github.com/matomo-org/matomo-sdk-android/issues/209
            trackMe.trySet(QueryParams.SCREEN_RESOLUTION, defaultTrackMe[QueryParams.SCREEN_RESOLUTION])
            trackMe.trySet(QueryParams.USER_AGENT, defaultTrackMe[QueryParams.USER_AGENT])
            trackMe.trySet(QueryParams.LANGUAGE, defaultTrackMe[QueryParams.LANGUAGE])
        }
    }

    fun track(trackMe: TrackMe): Tracker {
        var newTrackMe: TrackMe? = trackMe
        synchronized(mTrackingLock) {
            val newSession = System.currentTimeMillis() - mSessionStartTime > sessionTimeout
            if (newSession) {
                mSessionStartTime = System.currentTimeMillis()
                injectInitialParams(newTrackMe)
            }
            injectBaseParams(newTrackMe)
            for (callback in mTrackingCallbacks) {
                newTrackMe = newTrackMe?.run { callback.onTrack(this) }
                if (newTrackMe == null) {
                    Timber.tag(TAG).d("Tracking aborted by %s", callback)
                    return this
                }
            }
            lastEventX = newTrackMe
            if (!mOptOut) {
                mDispatcher.submit(newTrackMe)
                Timber.tag(TAG).d("Event added to the queue: %s", newTrackMe)
            } else {
                Timber.tag(TAG).d("Event omitted due to opt out: %s", newTrackMe)
            }
            return this
        }
    }

    val preferences: SharedPreferences by lazy { matomo.getTrackerPreferences(this) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val tracker = other as Tracker
        if (siteId != tracker.siteId) return false
        return if (apiUrl != tracker.apiUrl) false else name == tracker.name
    }

    override fun hashCode(): Int {
        var result = apiUrl.hashCode()
        result = 31 * result + siteId
        result = 31 * result + name.hashCode()
        return result
    }
    /**
     * If we are in dry-run mode then this will return a datastructure.
     *
     * @return a datastructure or null
     */
    /**
     * Set a data structure here to put the Dispatcher into dry-run-mode.
     * Data will be processed but at the last step just stored instead of transmitted.
     * Set it to null to disable it.
     *
     */
    var dryRunTarget: MutableList<Packet>?
        get() = mDispatcher.dryRunTarget
        set(dryRunTarget) {
            mDispatcher.dryRunTarget = dryRunTarget
        }

    fun interface Callback {
        /**
         * This method will be called after parameter injection and before transmission within [Tracker.track].
         * Blocking within this method will block tracking.
         *
         * @param trackMe The `TrackMe` that was passed to [Tracker.track] after all data has been injected.
         * @return The `TrackMe` that will be send, returning NULL here will abort transmission.
         */
        fun onTrack(trackMe: TrackMe): TrackMe?
    }

    companion object {
        private val TAG = tag(Tracker::class.java)

        // Matomo default parameter values
        private const val DEFAULT_UNKNOWN_VALUE = "unknown"
        private const val DEFAULT_TRUE_VALUE = "1"
        private const val DEFAULT_RECORD_VALUE = DEFAULT_TRUE_VALUE
        private const val DEFAULT_API_VERSION_VALUE = "1"

        // Sharedpreference keys for persisted values
        const val PREF_KEY_TRACKER_OPTOUT = "tracker.optout"
        const val PREF_KEY_TRACKER_USERID = "tracker.userid"
        private const val PREF_KEY_TRACKER_VISITORID = "tracker.visitorid"
        const val PREF_KEY_TRACKER_FIRSTVISIT = "tracker.firstvisit"
        const val PREF_KEY_TRACKER_VISITCOUNT = "tracker.visitcount"
        const val PREF_KEY_TRACKER_PREVIOUSVISIT = "tracker.previousvisit"
        private const val PREF_KEY_OFFLINE_CACHE_AGE = "tracker.cache.age"
        private const val PREF_KEY_OFFLINE_CACHE_SIZE = "tracker.cache.size"
        const val PREF_KEY_DISPATCHER_MODE = "tracker.dispatcher.mode"
        private val VALID_URLS = Pattern.compile("^(\\w+)(?:://)(.+?)$")
        private val PATTERN_VISITOR_ID = Pattern.compile("^[0-9a-f]{16}$")
        fun makeRandomVisitorId(): String {
            return UUID.randomUUID().toString().replace("-".toRegex(), "").substring(0, 16)
        }
    }

    fun getDispatchTimeout(): Int {
        return mDispatcher.connectionTimeOut
    }

    fun setDispatchTimeout(timeout: Int) {
        mDispatcher.connectionTimeOut = timeout
    }

    init {
        mOptOut = preferences.getBoolean(PREF_KEY_TRACKER_OPTOUT, false)
        mDispatcher = matomo.dispatcherFactory.build(this)
        mDispatcher.dispatchMode = dispatchMode
        val userId = preferences.getString(PREF_KEY_TRACKER_USERID, null)
        defaultTrackMe[QueryParams.USER_ID] = userId
        var visitorId = preferences.getString(PREF_KEY_TRACKER_VISITORID, null)
        if (visitorId == null) {
            visitorId = makeRandomVisitorId()
            preferences.edit().putString(PREF_KEY_TRACKER_VISITORID, visitorId).apply()
        }
        defaultTrackMe[QueryParams.VISITOR_ID] = visitorId
        defaultTrackMe[QueryParams.SESSION_START] = DEFAULT_TRUE_VALUE
        var resolution = DEFAULT_UNKNOWN_VALUE
        val res = matomo.deviceHelper.resolution
        if (res != null) resolution = String.format("%sx%s", res[0], res[1])
        defaultTrackMe[QueryParams.SCREEN_RESOLUTION] = resolution
        defaultTrackMe[QueryParams.USER_AGENT] = matomo.deviceHelper.userAgent
        defaultTrackMe[QueryParams.LANGUAGE] = matomo.deviceHelper.userLanguage
        defaultTrackMe[QueryParams.URL_PATH] = config.applicationBaseUrl
    }
}