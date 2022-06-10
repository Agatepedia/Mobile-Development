package com.example.agatepedia.ui.bookmark

import androidx.lifecycle.ViewModel
import com.example.agatepedia.data.remote.AgateRepository

class BookmarkViewModel(private val agateRepository: AgateRepository): ViewModel() {
    suspend fun getDataAgate() = agateRepository.getListAgate()
}