package com.example.hudkaraoke

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SpotifyApiService {

    // Este es el "enlace" interno de Spotify para ver la canción actual
    @GET("me/player/currently-playing")
    suspend fun getCancionActual(
        // Spotify exige que le enseñemos el Token en la cabecera (Header) de la petición
        @Header("Authorization") token: String
    ): Response<CancionActualResponse> // De momento ponemos 'Any', luego crearemos la estructura real de la canción

    // Orden para pausar la música
    @PUT("me/player/pause")
    suspend fun pausarMusica(@Header("Authorization") token: String): Response<Unit>

    // Orden para reanudar la música
    @PUT("me/player/play")
    suspend fun reproducirMusica(@Header("Authorization") token: String): Response<Unit>

    // Orden para pasar a la siguiente canción
    @POST("me/player/next")
    suspend fun siguienteCancion(@Header("Authorization") token: String): Response<Unit>

    // Orden para volver a la canción anterior
    @POST("me/player/previous")
    suspend fun cancionAnterior(@Header("Authorization") token: String): Response<Unit>

    @PUT("me/player/seek")
    suspend fun irATiempo(
        @Header("Authorization") token: String,
        @Query("position_ms") posicionMilisegundos: Long
    ): Response<Unit>

}