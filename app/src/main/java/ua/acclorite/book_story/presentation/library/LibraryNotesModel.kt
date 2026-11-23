package ua.acclorite.book_story.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import ua.acclorite.book_story.domain.repository.NoteRepository
import javax.inject.Inject

@HiltViewModel
class LibraryNotesModel @Inject constructor(repository: NoteRepository) : ViewModel() {
    val notes = repository.getAllNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
