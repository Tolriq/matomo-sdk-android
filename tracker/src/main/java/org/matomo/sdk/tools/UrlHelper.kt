/*
 *
 *  * Android SDK for Matomo
 *  *
 *  * @link https://github.com/matomo-org/matomo-android-sdk
 *  * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 *
 */
package org.matomo.sdk.tools

import android.util.Pair
import java.io.UnsupportedEncodingException
import java.net.URI
import java.net.URLDecoder
import java.util.ArrayList
import java.util.Scanner

/**
 * Helps us with Urls.
 */
object UrlHelper {
    private const val PARAMETER_SEPARATOR = "&"
    private const val NAME_VALUE_SEPARATOR = "="

    // Inspired by https://github.com/android/platform_external_apache-http/blob/master/src/org/apache/http/client/utils/URLEncodedUtils.java
    // Helper due to Apache http deprecation
    @JvmStatic
    fun parse(uri: URI, encoding: String?): List<Pair<String?, String?>?> {
        var result: List<Pair<String?, String?>?> = emptyList<Pair<String?, String?>>()
        val query = uri.rawQuery
        if (query != null && query.isNotEmpty()) {
            result = ArrayList()
            parse(result, Scanner(query), encoding)
        }
        return result
    }

    fun parse(parameters: MutableList<Pair<String?, String?>?>, scanner: Scanner, encoding: String?) {
        scanner.useDelimiter(PARAMETER_SEPARATOR)
        while (scanner.hasNext()) {
            val nameValue = scanner.next().split(NAME_VALUE_SEPARATOR).toTypedArray()
            require(!(nameValue.isEmpty() || nameValue.size > 2)) { "bad parameter" }
            val name = decode(nameValue[0], encoding)
            var value: String? = null
            if (nameValue.size == 2) value = decode(nameValue[1], encoding)
            parameters.add(Pair(name, value))
        }
    }

    private fun decode(content: String, encoding: String?): String {
        return try {
            URLDecoder.decode(content, encoding ?: "UTF-8")
        } catch (problem: UnsupportedEncodingException) {
            throw IllegalArgumentException(problem)
        }
    }
}