package com.example.agatepedia.ui.detailagatepedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agatepedia.data.local.entity.AgateEntity
import com.example.agatepedia.data.remote.AgateRepository
import kotlinx.coroutines.launch

class DetailViewModel(private val agateRepository: AgateRepository): ViewModel() {

    fun searchAgateData(agateName: String) = agateRepository.searchAgateData(agateName)

    suspend fun getStateBookmark(type: String) = agateRepository.getAgateBookmark(type)

    suspend fun insertBookmark(agate: AgateEntity) = agateRepository.saveBookmark(agate)

    suspend fun deleteBookmark(type: String) = agateRepository.deleteBookmark(type)
}