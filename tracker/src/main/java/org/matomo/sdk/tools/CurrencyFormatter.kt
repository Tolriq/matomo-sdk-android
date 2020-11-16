/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.tools

import java.math.BigDecimal

object CurrencyFormatter {
    @JvmStatic
    fun priceString(cents: Int?): String? {
        return if (cents == null) null else BigDecimal(cents).movePointLeft(2).toPlainString()
    }
}
