/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ua.acclorite.book_story.core.crash.CrashHandler
import java.io.File
import kotlin.concurrent.thread

private val uuidName = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

/**
 * Leftovers of the previous process in cacheDir: full book copies made by
 * `CachedFile.rawFile` (UUID names) and PDF viewer fallback copies. Nothing uses
 * them across processes, so they are safe to drop on startup.
 */
internal fun isStaleCacheFile(name: String): Boolean =
    uuidName.matches(name) || (name.startsWith("pdf-view-") && name.endsWith(".pdf"))

@HiltAndroidApp
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        clearStaleCache(cacheDir)
    }

    private fun clearStaleCache(directory: File) {
        // Only files from before this process started: a parse may already be
        // creating a fresh copy while the cleanup runs
        val processStart = System.currentTimeMillis()
        thread(name = "cache-cleanup", isDaemon = true) {
            runCatching {
                directory.listFiles { file ->
                    file.isFile && isStaleCacheFile(file.name) && file.lastModified() < processStart
                }
                    ?.forEach { it.delete() }
            }
        }
    }
}
