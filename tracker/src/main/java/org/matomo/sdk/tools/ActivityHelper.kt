package org.matomo.sdk.tools

import android.app.Activity
import java.util.ArrayList

object ActivityHelper {
    fun getBreadcrumbs(activity: Activity?): String {
        var currentActivity = activity
        val breadcrumbs = ArrayList<String>()
        while (currentActivity != null) {
            breadcrumbs.add(currentActivity.title.toString())
            currentActivity = currentActivity.parent
        }
        return breadcrumbs.joinToString("/")
    }

    fun breadcrumbsToPath(breadcrumbs: String): String {
        return breadcrumbs.replace("\\s".toRegex(), "")
    }
}
