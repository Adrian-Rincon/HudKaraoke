package com.example.hudkaraoke

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface LrcLibApiService {
    @Headers("User-Agent: HudKaraoke v1.0 (paraprimevideo8@gmail.com)")
    @GET("search")
    suspend fun buscarLetra(
        @Query("q") consulta: String
    ): Response<List<RespuestaLetra>>
}