package com.example.hudkaraoke

import com.google.gson.annotations.SerializedName

data class RespuestaLetra(
    // LRCLIB nos devuelve la letra con los tiempos en este campo
    @SerializedName("syncedLyrics") val letraSincronizada: String?
)