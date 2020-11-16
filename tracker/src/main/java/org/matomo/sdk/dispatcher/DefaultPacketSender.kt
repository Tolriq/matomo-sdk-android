package org.matomo.sdk.dispatcher

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.GZIPOutputStream

class DefaultPacketSender : PacketSender {
    private var mTimeout = Dispatcher.DEFAULT_CONNECTION_TIMEOUT
    private var mGzip = false
    override fun send(packet: Packet): Boolean {
        var urlConnection: HttpURLConnection? = null
        return try {
            urlConnection = URL(packet.targetURL).openConnection() as HttpURLConnection
            urlConnection.connectTimeout = mTimeout.toInt()
            urlConnection.readTimeout = mTimeout.toInt()

            // IF there is json data we have to do a post
            if (packet.postData != null) { // POST
                urlConnection.doOutput = true // Forces post
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("charset", "utf-8")
                val toPost = packet.postData.toString()
                if (mGzip) {
                    urlConnection.addRequestProperty("Content-Encoding", "gzip")
                    val byteArrayOS = ByteArrayOutputStream()
                    var gzipStream: GZIPOutputStream? = null
                    try {
                        gzipStream = GZIPOutputStream(byteArrayOS)
                        gzipStream.write(toPost.toByteArray(Charset.forName("UTF8")))
                    } finally {
                        // If closing fails we assume the written data to be invalid.
                        // Don't catch the exception and let it abort the `send(Packet)` call.
                        gzipStream?.close()
                    }
                    var outputStream: OutputStream? = null
                    try {
                        outputStream = urlConnection.outputStream
                        outputStream.write(byteArrayOS.toByteArray())
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close()
                            } catch (e: IOException) {
                                // Failing to close the stream is not enough to consider the transmission faulty.
                            }
                        }
                    }
                } else {
                    var writer: BufferedWriter? = null
                    try {
                        writer = BufferedWriter(OutputStreamWriter(urlConnection.outputStream, "UTF-8"))
                        writer.write(toPost)
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close()
                            } catch (e: IOException) {
                                // Failing to close the stream is not enough to consider the transmission faulty.
                            }
                        }
                    }
                }
            } else { // GET
                urlConnection.doOutput = false // Defaults to false, but for readability
            }
            val statusCode = urlConnection.responseCode
            val successful = statusCode == HttpURLConnection.HTTP_NO_CONTENT || statusCode == HttpURLConnection.HTTP_OK
            if (successful) {

                // https://github.com/matomo-org/matomo-sdk-android/issues/226
                val `is` = urlConnection.inputStream
                if (`is` != null) {
                    try {
                        `is`.close()
                    } catch (e: IOException) {
                        // Ignore
                    }
                }
            } else {
                // Consume the error stream (or at least close it) if the statuscode was non-OK (not 2XX)
                val errorReason = StringBuilder()
                var errorReader: BufferedReader? = null
                try {
                    errorReader = BufferedReader(InputStreamReader(urlConnection.errorStream))
                    var line: String?
                    while (errorReader.readLine().also { line = it } != null) errorReason.append(line)
                } finally {
                    if (errorReader != null) {
                        try {
                            errorReader.close()
                        } catch (e: IOException) {
                            // Ignore
                        }
                    }
                }
            }
            successful
        } catch (e: Exception) {
            false
        } finally {
            urlConnection?.disconnect()
        }
    }

    override fun setTimeout(timeout: Long) {
        mTimeout = timeout
    }

    override fun setGzipData(gzip: Boolean) {
        mGzip = gzip
    }
}
