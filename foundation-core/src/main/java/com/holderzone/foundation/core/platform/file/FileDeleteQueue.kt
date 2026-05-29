package com.holderzone.foundation.core.platform.file

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

object FileDeleteQueue {
    private val queue = ConcurrentLinkedQueue<File>()

    fun add(file: File?) {
        if (file != null) queue.add(file)
    }

    fun enqueue(path: String?) {
        val cleanPath = path?.trim().orEmpty()
        if (cleanPath.isNotBlank()) {
            add(File(cleanPath))
        }
    }

    fun enqueueAll(paths: Iterable<String>) {
        paths.forEach(::enqueue)
    }

    fun drain(): Int {
        var deletedCount = 0
        while (true) {
            val file = queue.poll() ?: break
            if (file.exists() && file.delete()) {
                deletedCount += 1
            }
        }
        return deletedCount
    }

    fun clear() {
        queue.clear()
    }
}
