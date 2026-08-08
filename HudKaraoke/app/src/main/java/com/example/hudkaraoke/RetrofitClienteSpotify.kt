package com.example.hudkaraoke

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClienteSpotify {

    // La dirección principal de la API de Spotify
    private const val BASE_URL = "https://api.spotify.com/v1/"

    // lazy significa que el motor solo arrancará la primera vez que lo necesitemos
    val apiService: SpotifyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Le enchufamos el traductor GSON
            .build()
            .create(SpotifyApiService::class.java) // Le pasamos nuestro "menú"
    }
}