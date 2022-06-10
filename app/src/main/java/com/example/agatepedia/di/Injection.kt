package com.example.agatepedia.di

import android.content.Context
import com.example.agatepedia.data.local.room.AgateRoomDatabase
import com.example.agatepedia.data.remote.AgateRepository
import com.example.agatepedia.data.remote.retrofit.ApiConfig

object Injection {
    fun provideRepository(context: Context): AgateRepository{
        val apiService = ApiConfig.getApiService()
        val database = AgateRoomDatabase.getDatabase(context)
        val dao = database.agateDao()
        return AgateRepository.getInstance(apiService, dao)
    }
}