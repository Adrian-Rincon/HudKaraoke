package com.example.hudkaraoke

import com.google.gson.annotations.SerializedName

// --- Variass clases, creadas para traducir del JSON de Spotify a variables de kotling

data class CancionActualResponse(
    @SerializedName("item") val cancion: DetallesCancion?,
    @SerializedName("is_playing") val estaReproduciendo: Boolean,
    @SerializedName("progress_ms") val progresoMilisegundos: Long
)

// Dentro del "item" está la información de la pista
data class DetallesCancion(
    @SerializedName("name") val nombre: String,
    @SerializedName("artists") val artistas: List<Artista>,
    @SerializedName("duration_ms") val duracionMilisegundos: Long
)

// Sacamos el nombre de los artistas
data class Artista(
    @SerializedName("name") val nombre: String
)

// Las carátulas del album (por si acaso)
data class Album(
    @SerializedName("images") val imagenes: List<ImagenAlbum>
)

// la URL de la imagen
data class ImagenAlbum(
    @SerializedName("url") val url: String
)