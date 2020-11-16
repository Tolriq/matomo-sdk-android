package org.matomo.sdk.dispatcher

import org.matomo.sdk.Tracker
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.LinkedBlockingQueue

class EventDiskCache(tracker: Tracker) {
    private val mEventContainer = LinkedBlockingQueue<File>()
    private var mCacheDir: File
    private val mMaxAge: Long = tracker.offlineCacheAge
    private val mMaxSize: Long = tracker.offlineCacheSize
    private var mCurrentSize: Long = 0
    private var mDelayedClear = false

    // Must be called from a synchronized method
    private fun checkCacheLimits() {
        if (mMaxAge < 0) {
            while (!mEventContainer.isEmpty()) {
                val head = mEventContainer.poll()
                head.delete()
            }
        } else if (mMaxAge > 0) {
            val iterator = mEventContainer.iterator()
            while (iterator.hasNext()) {
                val head = iterator.next()
                var timestamp: Long
                timestamp = try {
                    val split = head.name.split("_").toTypedArray()
                    java.lang.Long.valueOf(split[1])
                } catch (e: Exception) {
                    0
                }
                if (timestamp < System.currentTimeMillis() - mMaxAge) {
                    head.delete()
                    iterator.remove()
                } else {
                    // List is sorted by age
                    break
                }
            }
        }
        if (mMaxSize != 0L) {
            val iterator = mEventContainer.iterator()
            while (iterator.hasNext() && mCurrentSize > mMaxSize) {
                val head = iterator.next()
                mCurrentSize -= head.length()
                iterator.remove()
                head.delete()
            }
        }
    }

    private val isCachingEnabled: Boolean
        get() = mMaxAge >= 0

    @Synchronized
    fun cache(toCache: List<Event>) {
        if (!isCachingEnabled || toCache.isEmpty()) return
        checkCacheLimits()
        val container = writeEventFile(toCache)
        if (container != null) {
            mEventContainer.add(container)
            mCurrentSize += container.length()
        }
    }

    @Synchronized
    fun uncache(): List<Event> {
        val events: MutableList<Event> = ArrayList()
        if (!isCachingEnabled) return events
        while (!mEventContainer.isEmpty()) {
            val head = mEventContainer.poll()
            if (head != null) {
                events.addAll(readEventFile(head))
                head.delete()
            }
        }
        checkCacheLimits()
        return events
    }

    @get:Synchronized
    val isEmpty: Boolean
        get() {
            if (!mDelayedClear) {
                checkCacheLimits()
                mDelayedClear = true
            }
            return mEventContainer.isEmpty()
        }

    private fun readEventFile(file: File): List<Event> {
        val events: MutableList<Event> = ArrayList()
        if (!file.exists()) return events
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val versionLine = bufferedReader.readLine()
            if (VERSION != versionLine) return events
            val cutoff = System.currentTimeMillis() - mMaxAge
            var line = ""
            while (bufferedReader.readLine()?.also { line = it } != null) {
                val split = line.indexOf(" ")
                if (split == -1) continue
                try {
                    val timestamp = line.substring(0, split).toLong()
                    if (mMaxAge > 0 && timestamp < cutoff) continue
                    val query = line.substring(split + 1)
                    events.add(Event(timestamp, query))
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: IOException) {
            // Ignore
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    // Ignore
                }
            }
        }
        return events
    }

    private fun writeEventFile(events: List<Event>): File? {
        if (events.isEmpty()) return null
        val newFile = File(mCacheDir, "events_" + events[events.size - 1].timeStamp)
        var out: FileWriter? = null
        var dataWritten = false
        try {
            out = FileWriter(newFile)
            out.append(VERSION).append("\n")
            val cutoff = System.currentTimeMillis() - mMaxAge
            for (event in events) {
                if (mMaxAge > 0 && event.timeStamp < cutoff) continue
                out.append(event.timeStamp.toString()).append(" ").append(event.encodedQuery).append("\n")
                dataWritten = true
            }
        } catch (e: Exception) {
            newFile.deleteOnExit()
            return null
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        // If just version data was written delete the file.
        return if (dataWritten) newFile else {
            newFile.delete()
            null
        }
    }

    companion object {
        private const val CACHE_DIR_NAME = "piwik_cache"
        private const val VERSION = "1"
    }

    init {
        val baseDir = File(tracker.matomo.context.cacheDir, CACHE_DIR_NAME)
        mCacheDir = try {
            File(baseDir, URL(tracker.apiUrl).host)
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
        val storedContainers = mCacheDir.listFiles()
        if (storedContainers != null) {
            Arrays.sort(storedContainers)
            for (container in storedContainers) {
                mCurrentSize += container.length()
                mEventContainer.add(container)
            }
        } else {
            mCacheDir.mkdirs()
        }
    }
}