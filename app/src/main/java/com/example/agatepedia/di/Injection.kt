package com.example.agatepedia.di

import com.example.agatepedia.data.remote.AgateRepository
import com.example.agatepedia.data.remote.retrofit.ApiConfig

object Injection {
    fun provideRepository(): AgateRepository{
        val apiService = ApiConfig.getApiService()
        return AgateRepository.getInstance(apiService)
    }
}