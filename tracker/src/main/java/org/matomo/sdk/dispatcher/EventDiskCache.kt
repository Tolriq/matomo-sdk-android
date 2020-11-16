package org.matomo.sdk.dispatcher

import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import timber.log.Timber
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
        val startTime = System.currentTimeMillis()
        if (mMaxAge < 0) {
            Timber.tag(TAG).d("Caching is disabled.")
            while (!mEventContainer.isEmpty()) {
                val head = mEventContainer.poll()
                if (head.delete()) {
                    Timber.tag(TAG).e("Deleted cache container %s", head.path)
                }
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
                    Timber.tag(TAG).e(e)
                    0
                }
                if (timestamp < System.currentTimeMillis() - mMaxAge) {
                    if (head.delete()) Timber.tag(TAG).e("Deleted cache container %s", head.path) else Timber.tag(TAG).e("Failed to delete cache container %s", head.path)
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
                if (head.delete()) Timber.tag(TAG).e("Deleted cache container %s", head.path) else Timber.tag(TAG).e("Failed to delete cache container %s", head.path)
            }
        }
        val stopTime = System.currentTimeMillis()
        Timber.tag(TAG).d("Cache check took %dms", stopTime - startTime)
    }

    private val isCachingEnabled: Boolean
        get() = mMaxAge >= 0

    @Synchronized
    fun cache(toCache: List<Event>) {
        if (!isCachingEnabled || toCache.isEmpty()) return
        checkCacheLimits()
        val startTime = System.currentTimeMillis()
        val container = writeEventFile(toCache)
        if (container != null) {
            mEventContainer.add(container)
            mCurrentSize += container.length()
        }
        val stopTime = System.currentTimeMillis()
        Timber.tag(TAG).d("Caching of %d events took %dms (%s)", toCache.size, stopTime - startTime, container)
    }

    @Synchronized
    fun uncache(): List<Event> {
        val events: MutableList<Event> = ArrayList()
        if (!isCachingEnabled) return events
        val startTime = System.currentTimeMillis()
        while (!mEventContainer.isEmpty()) {
            val head = mEventContainer.poll()
            if (head != null) {
                events.addAll(readEventFile(head))
                if (!head.delete()) Timber.tag(TAG).e("Failed to delete cache container %s", head.path)
            }
        }
        checkCacheLimits()
        val stopTime = System.currentTimeMillis()
        Timber.tag(TAG).d("Uncaching of %d events took %dms", events.size, stopTime - startTime)
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
        var `in`: InputStream? = null
        try {
            `in` = FileInputStream(file)
            val inputStreamReader = InputStreamReader(`in`)
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
                    Timber.tag(TAG).e(e)
                }
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e(e)
        } finally {
            if (`in` != null) {
                try {
                    `in`.close()
                } catch (e: IOException) {
                    Timber.tag(TAG).e(e)
                }
            }
        }
        Timber.tag(TAG).d("Restored %d events from %s", events.size, file.path)
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
            Timber.tag(TAG).e(e)
            newFile.deleteOnExit()
            return null
        } finally {
            if (out != null) {
                try {
                    out.close()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e)
                }
            }
        }
        Timber.tag(TAG).d("Saved %d events to %s", events.size, newFile.path)

        // If just version data was written delete the file.
        return if (dataWritten) newFile else {
            newFile.delete()
            null
        }
    }

    companion object {
        private val TAG = Matomo.tag(EventDiskCache::class.java)
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
            if (!mCacheDir.mkdirs()) Timber.tag(TAG).e("Failed to make disk-cache dir %s", mCacheDir)
        }
    }
}