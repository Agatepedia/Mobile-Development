package com.example.agatepedia.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agatepedia.data.Result
import com.example.agatepedia.data.remote.AgateRepository
import com.example.agatepedia.data.remote.response.AgateResponseItem

class HomeViewModel(private val agateRepository: AgateRepository) : ViewModel() {

    var isSearch = false
    var searchViewData: List<AgateResponseItem>? = null
    val searchNotFound = MutableLiveData<Boolean>()


    init {
        searchNotFound.postValue(false)
    }
    fun getAgateData() = agateRepository.getAgateData()

    fun searchAgateData(agateName: String)= agateRepository.searchAgateData(agateName)
}