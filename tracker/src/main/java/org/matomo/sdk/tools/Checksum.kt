/*
 * Android SDK for Matomo
 *
 * @link https://github.com/matomo-org/matomo-android-sdk
 * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 */
package org.matomo.sdk.tools

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Offers to calculate checksums
 */
object Checksum {
    private const val HEXES = "0123456789ABCDEF"

    /**
     * Transforms byte into hex representation.
     */
    @JvmStatic
    fun getHex(raw: ByteArray?): String? {
        if (raw == null) return null
        val hex = StringBuilder(2 * raw.size)
        for (b in raw) {
            val i = b.toInt()
            hex.append(HEXES[i and 0xF0 shr 4]).append(HEXES[i and 0x0F])
        }
        return hex.toString()
    }

    /**
     * MD5-Checksum for a string.
     */
    @Throws(Exception::class)
    @JvmStatic
    fun getMD5Checksum(string: String): String? {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(string.toByteArray())
        val messageDigest = digest.digest()
        return getHex(messageDigest)
    }

    /**
     * MD5-Checksum for a file.
     */
    @JvmStatic
    @Throws(Exception::class)
    fun getMD5Checksum(file: File): String? {
        if (!file.isFile) return null
        val fis: InputStream = FileInputStream(file)
        val buffer = ByteArray(1024)
        val complete = MessageDigest.getInstance("MD5")
        var numRead: Int
        do {
            numRead = fis.read(buffer)
            if (numRead > 0) complete.update(buffer, 0, numRead)
        } while (numRead != -1)
        fis.close()
        return getHex(complete.digest())
    }
}