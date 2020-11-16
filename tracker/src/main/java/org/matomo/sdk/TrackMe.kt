/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk

import java.util.HashMap

/**
 * This objects represents one query to Matomo.
 * For each event send to Matomo a TrackMe gets created, either explicitly by you or implicitly by the Tracker.
 */
class TrackMe {
    private val mQueryParams = HashMap<String, String>(DEFAULT_QUERY_CAPACITY)

    constructor() {}
    constructor(trackMe: TrackMe) {
        mQueryParams.putAll(trackMe.mQueryParams)
    }

    /**
     * Adds TrackMe to this TrackMe, overriding values if necessary.
     */
    fun putAll(trackMe: TrackMe): TrackMe {
        mQueryParams.putAll(trackMe.toMap())
        return this
    }

    /**
     * Consider using [QueryParams] instead of raw strings
     */
    @Synchronized
    operator fun set(key: String, value: String?): TrackMe {
        if (value == null) mQueryParams.remove(key) else if (value.length > 0) mQueryParams[key] = value
        return this
    }

    /**
     * Consider using [QueryParams] instead of raw strings
     */
    @Synchronized
    operator fun get(queryParams: String): String? {
        return mQueryParams[queryParams]
    }

    /**
     * You can set any additional Tracking API Parameters within the SDK.
     * This includes for example the local time (parameters h, m and s).
     * <pre>
     * set(QueryParams.HOURS, "10");
     * set(QueryParams.MINUTES, "45");
     * set(QueryParams.SECONDS, "30");
    </pre> *
     *
     * @param key   query params name
     * @param value value
     * @return tracker instance
     */
    @Synchronized
    operator fun set(key: QueryParams, value: String?): TrackMe {
        set(key.toString(), value)
        return this
    }

    @Synchronized
    operator fun set(key: QueryParams, value: Int): TrackMe {
        set(key, Integer.toString(value))
        return this
    }

    @Synchronized
    operator fun set(key: QueryParams, value: Float): TrackMe {
        set(key, java.lang.Float.toString(value))
        return this
    }

    @Synchronized
    operator fun set(key: QueryParams, value: Long): TrackMe {
        set(key, java.lang.Long.toString(value))
        return this
    }

    @Synchronized
    fun has(queryParams: QueryParams): Boolean {
        return mQueryParams.containsKey(queryParams.toString())
    }

    /**
     * Only sets the value if it doesn't exist.
     *
     * @param key   type
     * @param value value
     * @return this (for chaining)
     */
    @Synchronized
    fun trySet(key: QueryParams, value: Int): TrackMe {
        return trySet(key, value.toString())
    }

    /**
     * Only sets the value if it doesn't exist.
     *
     * @param key   type
     * @param value value
     * @return this (for chaining)
     */
    @Synchronized
    fun trySet(key: QueryParams, value: Float): TrackMe {
        return trySet(key, value.toString())
    }

    @Synchronized
    fun trySet(key: QueryParams, value: Long): TrackMe {
        return trySet(key, value.toString())
    }

    /**
     * Only sets the value if it doesn't exist.
     *
     * @param key   type
     * @param value value
     * @return this (for chaining)
     */
    @Synchronized
    fun trySet(key: QueryParams, value: String?): TrackMe {
        if (!has(key)) set(key, value)
        return this
    }

    /**
     * The tracker calls this to get the final data that will be transmitted
     *
     * @return the parameter map, but without the base URL
     */
    @Synchronized
    fun toMap(): Map<String, String> {
        return HashMap(mQueryParams)
    }

    @Synchronized
    operator fun get(queryParams: QueryParams): String? {
        return mQueryParams[queryParams.toString()]
    }

    @get:Synchronized
    val isEmpty: Boolean
        get() = mQueryParams.isEmpty()

    companion object {
        private const val DEFAULT_QUERY_CAPACITY = 14
    }
}