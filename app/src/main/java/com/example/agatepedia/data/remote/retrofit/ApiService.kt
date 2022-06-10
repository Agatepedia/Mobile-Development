package com.example.agatepedia.data.remote.retrofit

import com.example.agatepedia.data.remote.response.AgateResponse
import com.example.agatepedia.data.remote.response.AgateResponseItem
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("agates")
    suspend fun getAgate(): List<AgateResponseItem>

    @GET("agates/{jenis}")
    suspend fun searchAgate(@Path("jenis") jenis: String) : List<AgateResponseItem>


}