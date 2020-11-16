/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.extra

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.matomo.sdk.Matomo
import org.matomo.sdk.QueryParams
import org.matomo.sdk.TrackMe
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * A custom variable is a custom name-value pair that you can assign to your users or screen views,
 * and then visualize the reports of how many visits, conversions, etc. for each custom variable.
 * A custom variable is defined by a name — for example,
 * "User status" — and a value – for example, "LoggedIn" or "Anonymous".
 *
 *
 * You can track up to 5 custom variables for each user to your app,
 * and up to 5 custom variables for each screen view.
 * You may configure Matomo to track more custom variables: http://matomo.org/faq/how-to/faq_17931/
 *
 *
 * Desired json output:
 * {
 * "1":["OS","iphone 5.0"],
 * "2":["Matomo Mobile Version","1.6.2"],
 * "3":["Locale","en::en"],
 * "4":["Num Accounts","2"],
 * "5":["Level","over9k"]
 * }
 */
class CustomVariables {
    private val mVars: MutableMap<String?, JSONArray?> = ConcurrentHashMap()

    constructor()
    constructor(variables: CustomVariables) {
        mVars.putAll(variables.mVars)
    }

    constructor(json: String?) {
        if (json != null) {
            try {
                val jsonObject = JSONObject(json)
                val it = jsonObject.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    put(key, jsonObject.getJSONArray(key))
                }
            } catch (e: JSONException) {
                Timber.tag(TAG).e(e, "Failed to create CustomVariables from JSON")
            }
        }
    }

    fun putAll(customVariables: CustomVariables): CustomVariables {
        mVars.putAll(customVariables.mVars)
        return this
    }

    /**
     * Custom variable names and values are limited to 200 characters in length each.
     *
     * @param index this Integer accepts values from 1 to 5.
     * A given custom variable name must always be stored in the same "index" per session.
     * For example, if you choose to store the variable name = "Gender" in index = 1
     * and you record another custom variable in index = 1, then the "Gender" variable
     * will be deleted and replaced with the new custom variable stored in index 1.
     * You may configure Matomo to track more custom variables than 5.
     * Read more: http://matomo.org/faq/how-to/faq_17931/
     * @param name  of a specific Custom Variable such as "User type".
     * @param value of a specific Custom Variable such as "Customer".
     * @return super.put result if index in right range and name/value pair aren't null
     */
    fun put(index: Int, name: String?, value: String?): CustomVariables {
        var newName = name
        var newValue = value
        if (index > 0 && newName != null && newValue != null) {
            if (newName.length > MAX_LENGTH) {
                Timber.tag(TAG).w("Name is too long %s", newName)
                newName = newName.substring(0, MAX_LENGTH)
            }
            if (newValue.length > MAX_LENGTH) {
                Timber.tag(TAG).w("Value is too long %s", newValue)
                newValue = newValue.substring(0, MAX_LENGTH)
            }
            put(index.toString(), JSONArray(listOf(newName, newValue)))
        } else Timber.tag(TAG).w("Index is out of range or name/value is null")
        return this
    }

    /**
     * @param index  index accepts values from 1 to 5.
     * @param values packed key/value pair
     * @return super.put result or null if key is null or value length is not equals 2
     */
    fun put(index: String?, values: JSONArray): CustomVariables {
        if (values.length() == 2 && index != null) {
            mVars[index] = values
        } else Timber.tag(TAG).w("values.length() should be equal 2")
        return this
    }

    override fun toString(): String {
        val json = JSONObject(mVars)
        return if (json.length() > 0) json.toString() else ""
    }

    fun size(): Int {
        return mVars.size
    }

    /**
     * Sets the custom variables with scope VISIT to a [TrackMe].
     */
    fun injectVisitVariables(trackMe: TrackMe): TrackMe {
        trackMe[QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES] = this.toString()
        return trackMe
    }

    fun toVisitVariables(): TrackMe {
        return injectVisitVariables(TrackMe())
    }

    companion object {
        private val TAG = Matomo.tag(CustomVariables::class.java)
        const val MAX_LENGTH = 200
    }
}