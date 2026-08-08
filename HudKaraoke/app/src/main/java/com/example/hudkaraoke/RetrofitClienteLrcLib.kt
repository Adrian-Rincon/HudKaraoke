package com.example.hudkaraoke

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClienteLrcLib {
    // La URL base específica de esta API
    private const val BASE_URL = "https://lrclib.net/api/"

    val apiService: LrcLibApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LrcLibApiService::class.java)
    }
}