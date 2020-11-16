package org.matomo.sdk

import java.net.MalformedURLException
import java.net.URL

/**
 * Configuration details for a [Tracker]
 */
data class TrackerBuilder(val apiUrl: String, val siteId: Int, var trackerName: String) {
    var applicationBaseUrl: String = ""
        private set

    /**
     * Domain used to build the required parameter url (http://developer.matomo.org/api-reference/tracking-api)
     * Defaults to`https://your.packagename`
     *
     * @param domain your-domain.com
     */
    fun setApplicationBaseUrl(domain: String): TrackerBuilder {
        applicationBaseUrl = domain
        return this
    }

    fun build(matomo: Matomo): Tracker {
        if (applicationBaseUrl.isEmpty()) {
            applicationBaseUrl = String.format("https://%s/", matomo.context.packageName)
        }
        return Tracker(matomo, this)
    }

    companion object {
        @JvmStatic
        fun createDefault(apiUrl: String, siteId: Int): TrackerBuilder {
            return TrackerBuilder(apiUrl, siteId, "Default Tracker")
        }
    }

    init {
        try {
            URL(apiUrl)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
    }
}
