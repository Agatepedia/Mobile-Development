package com.example.agatepedia.ui.detailagatepedia

import androidx.lifecycle.ViewModel
import com.example.agatepedia.data.remote.AgateRepository

class DetailViewModel(private val agateRepository: AgateRepository): ViewModel() {

    fun searchAgateData(agateName: String) = agateRepository.searchAgateData(agateName)
}