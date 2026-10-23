package com.kkokkkok.android.ui.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session-only local selection, not an API request or an analysis result. */
class PhotoSelectionViewModel : ViewModel() {
    private val selection = MutableStateFlow<Uri?>(null)
    val selectedPhoto = selection.asStateFlow()

    fun onPhotoPicked(uri: Uri?) {
        // Cancelling a replacement must not discard the previous selection.
        if (uri != null) selection.value = uri
    }
}
