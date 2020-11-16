package org.matomo.sdk.dispatcher

import java.util.ArrayList
import java.util.concurrent.LinkedBlockingDeque

class EventCache(private val mDiskCache: EventDiskCache) {
    private val mQueue = LinkedBlockingDeque<Event>()
    fun add(event: Event) {
        mQueue.add(event)
    }

    fun drainTo(drainedEvents: MutableList<Event>) {
        mQueue.drainTo(drainedEvents)
    }

    fun clear() {
        mDiskCache.uncache()
        mQueue.clear()
    }

    val isEmpty: Boolean
        get() = mQueue.isEmpty() && mDiskCache.isEmpty

    fun updateState(online: Boolean): Boolean {
        if (online) {
            val uncache = mDiskCache.uncache()
            val it: ListIterator<Event> = uncache.listIterator(uncache.size)
            while (it.hasPrevious()) {
                // Anything from  disk cache is older then what the queue could currently contain.
                mQueue.offerFirst(it.previous())
            }
        } else if (!mQueue.isEmpty()) {
            val toCache: MutableList<Event> = ArrayList()
            mQueue.drainTo(toCache)
            mDiskCache.cache(toCache)
        }
        return online && !mQueue.isEmpty()
    }

    fun requeue(events: List<Event>) {
        for (e in events) {
            mQueue.offerFirst(e)
        }
    }
}
