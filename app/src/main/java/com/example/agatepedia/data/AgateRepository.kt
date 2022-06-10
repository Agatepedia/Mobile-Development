package com.example.agatepedia.data.remote

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.example.agatepedia.data.Result
import com.example.agatepedia.data.local.entity.AgateEntity
import com.example.agatepedia.data.local.room.AgateDao
import com.example.agatepedia.data.remote.response.AgateResponseItem
import com.example.agatepedia.data.remote.retrofit.ApiService

class AgateRepository(
    private val apiService: ApiService,
    private val agateDao: AgateDao
) {

    fun getAgateData(): LiveData<Result<List<AgateResponseItem>>> = liveData {
        emit(Result.Loading)
        try {
            val response = apiService.getAgate()
            val resultData: Result.Success<List<AgateResponseItem>> = Result.Success(response)
            emit(resultData)
        } catch (e: Exception) {
            Log.d(TAG, "get Agate Data: ${e.message.toString()}")
            emit(Result.Error(e.message.toString()))
        }
    }

    fun searchAgateData(nameAgate: String): LiveData<Result<List<AgateResponseItem>>> = liveData {
        emit(Result.Loading)
        try {
            val response = apiService.searchAgate(nameAgate)
            val resultData: Result.Success<List<AgateResponseItem>> = Result.Success(response)
            emit(resultData)
        } catch (e: Exception) {
            Log.d(TAG, "get Agate Data: ${e.message.toString()}")
            emit(Result.Error(e.message.toString()))
        }
    }

    suspend fun saveBookmark(agate: AgateEntity) = agateDao.insertAgate(agate)


    suspend fun deleteBookmark(type: String) = agateDao.deleteBookmark(type)


    suspend fun getListAgate(): List<AgateEntity> = agateDao.getListAgateBookmark()


    suspend fun getAgateBookmark(type: String): Boolean = agateDao.getAgateBookmark(type)


    companion object{
        private val TAG = AgateRepository::class.java.simpleName

        @Volatile
        private var instance: AgateRepository? = null

        fun getInstance(
            apiService: ApiService,
            agateDao: AgateDao
        ): AgateRepository =
            instance ?: synchronized(this){
                instance?: AgateRepository(apiService, agateDao)
            }.also { instance = it }

    }
}