package org.matomo.sdk

import java.net.MalformedURLException
import java.net.URL

/**
 * Configuration details for a [Tracker]
 */
class TrackerBuilder(apiUrl: String, siteId: Int, trackerName: String) {
    val apiUrl: String
    val siteId: Int
    var trackerName: String
        private set
    var applicationBaseUrl: String? = null
        private set

    /**
     * A unique name for this Tracker. Used to store Tracker settings independent of URL and id changes.
     */
    fun setTrackerName(name: String): TrackerBuilder {
        trackerName = name
        return this
    }

    /**
     * Domain used to build the required parameter url (http://developer.matomo.org/api-reference/tracking-api)
     * Defaults to`https://your.packagename`
     *
     * @param domain your-domain.com
     */
    fun setApplicationBaseUrl(domain: String?): TrackerBuilder {
        applicationBaseUrl = domain
        return this
    }

    fun build(matomo: Matomo): Tracker {
        if (applicationBaseUrl == null) {
            applicationBaseUrl = String.format("https://%s/", matomo.context.packageName)
        }
        return Tracker(matomo, this)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as TrackerBuilder
        return siteId == that.siteId && apiUrl == that.apiUrl && trackerName == that.trackerName
    }

    override fun hashCode(): Int {
        var result = apiUrl.hashCode()
        result = 31 * result + siteId
        result = 31 * result + trackerName.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        fun createDefault(apiUrl: String, siteId: Int): TrackerBuilder {
            return TrackerBuilder(apiUrl, siteId, "Default Tracker")
        }
    }

    /**
     * @param apiUrl      Tracking HTTP API endpoint, for example, https://matomo.yourdomain.tld/matomo.php
     * @param siteId      id of your site in the backend
     * @param trackerName name of your tracker, will be used to store configuration data
     */
    init {
        try {
            URL(apiUrl)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
        this.apiUrl = apiUrl
        this.siteId = siteId
        this.trackerName = trackerName
    }
}