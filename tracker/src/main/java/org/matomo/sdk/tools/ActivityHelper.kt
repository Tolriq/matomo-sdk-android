package org.matomo.sdk.tools

import android.app.Activity
import android.text.TextUtils
import java.util.ArrayList

object ActivityHelper {
    fun getBreadcrumbs(activity: Activity?): String {
        var currentActivity = activity
        val breadcrumbs = ArrayList<String?>()
        while (currentActivity != null) {
            breadcrumbs.add(currentActivity.title.toString())
            currentActivity = currentActivity.parent
        }
        return joinSlash(breadcrumbs)
    }

    private fun joinSlash(sequence: List<String?>?): String {
        return if (sequence != null && sequence.isNotEmpty()) {
            TextUtils.join("/", sequence)
        } else ""
    }

    fun breadcrumbsToPath(breadcrumbs: String): String {
        return breadcrumbs.replace("\\s".toRegex(), "")
    }
}