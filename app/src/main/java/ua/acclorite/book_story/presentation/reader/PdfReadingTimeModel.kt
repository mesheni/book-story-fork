package ua.acclorite.book_story.presentation.reader

import android.app.Application
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.parser.file.readPdfOutline
import ua.acclorite.book_story.domain.model.reader.PdfPageText
import ua.acclorite.book_story.domain.use_case.book.GetPdfPageTextsUseCase
import javax.inject.Inject

data class PdfReadingTimeData(val pages: List<PdfPageText>, val chapterPages: List<Int>)

@HiltViewModel
class PdfReadingTimeModel @Inject constructor(
    private val app: Application,
    private val getPageTexts: GetPdfPageTextsUseCase
) : ViewModel() {
    suspend fun load(bookId: Int, uri: String): PdfReadingTimeData? = withContext(Dispatchers.IO) {
        val pages = getPageTexts(bookId).getOrNull() ?: return@withContext null
        val chapterPages = runCatching {
            PDFBoxResourceLoader.init(app)
            val input = app.contentResolver.openInputStream(uri.toUri()) ?: error("Cannot read PDF")
            // Temp-file buffering: the viewer and the word-box session already
            // hold this PDF, a third in-memory copy may exhaust the heap
            input.use { stream -> PDDocument.load(stream, MemoryUsageSetting.setupTempFileOnly()).use { document ->
                readPdfOutline(document).filter { it.depth == 0 }.mapNotNull { it.page }.distinct().sorted()
            } }
        }.getOrDefault(emptyList())
        PdfReadingTimeData(pages, chapterPages)
    }
}
