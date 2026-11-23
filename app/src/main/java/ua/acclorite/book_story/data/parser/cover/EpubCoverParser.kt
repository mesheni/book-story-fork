/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.cover

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.decodeHref
import java.util.zip.ZipFile
import javax.inject.Inject

private const val TAG = "EpubCoverParser"

class EpubCoverParser @Inject constructor() : CoverParser {

    override suspend fun parse(cachedFile: CachedFile): CoverImage? {
        return try {
            var coverImage: CoverImage? = null

            val rawFile = cachedFile.rawFile
            if (rawFile == null || !rawFile.exists() || !rawFile.canRead()) return null

            withContext(Dispatchers.IO) {
                ZipFile(rawFile).use { zip ->
                    val opfEntry = zip.entries().asSequence().find { entry ->
                        entry.name.endsWith(".opf", ignoreCase = true)
                    } ?: return@withContext

                    val opfContent = zip
                        .getInputStream(opfEntry)
                        .bufferedReader()
                        .use { it.readText() }
                    val document = Jsoup.parse(opfContent, Parser.xmlParser())

                    val coverImagePath = document
                        .select("metadata > meta[name=cover]")
                        .attr("content")
                        .let { coverId ->
                            if (coverId.isNotBlank()) {
                                document
                                    .select("manifest > item")
                                    .firstOrNull { it.attr("id") == coverId }
                                    ?.attr("href")
                                    .orEmpty()
                                    .also { src ->
                                        if (src.isBlank()) return@also
                                        return@let src
                                    }
                            }

                            document
                                .select("manifest > item[media-type*=image]")
                                .firstOrNull()
                                ?.attr("href")
                                ?.also { src -> if (src.isBlank()) return@let null }
                        }
                        ?.decodeHref()
                        ?: return@use

                    // href is relative to the OPF and may contain "../", so match
                    // the full path first and fall back to the file name
                    val coverFileName = coverImagePath.substringAfterLast('/')
                    val entries = zip.entries().toList()
                    val coverEntry = entries.firstOrNull { it.name.endsWith(coverImagePath) }
                        ?: entries.firstOrNull {
                            it.name.substringAfterLast('/').equals(coverFileName, ignoreCase = true)
                        }
                        ?: return@use

                    val imageBytes = zip.getInputStream(coverEntry).readBytes()
                    coverImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }
            }
            coverImage
        } catch (e: Exception) {
            logE(TAG, "Could not parse cover image with message: ${e.message}.")
            null
        } finally {
            // The cover is the last consumer of this CachedFile's cacheDir copy
            // (after EpubFileParser on import), so drop the copy here
            runCatching { cachedFile.rawFile?.delete() }
        }
    }
}