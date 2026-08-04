package com.example.hudkaraoke

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

// Al poner el diseño entre paréntesis, nos ahorramos muchísimo código
class InicioFragment : Fragment(R.layout.fragment_inicio) {

    // Conexion con spotify
    private val CLIENT_ID = "049622d929eb45d39aa6c96e08c73d78"
    private val REDIRECT_URI = "hudkaraoke://callback"
    private var REQUEST_CODE = 1337

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Preparamos las variables para el inicio y guardado
        val btnIniciarSesion = view.findViewById<Button>(R.id.btn_iniciar_sesion)
        val libreta = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
        val tokenGuardado = libreta.getString("spotify_token", null)
        //si existe vamos directos
        if (tokenGuardado != null){
            irMusicaFragment()
        }

        btnIniciarSesion.setOnClickListener{
            iniciarSesionSpotify()
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE){
            val response = AuthorizationClient.getResponse(resultCode, data)
            // Recuerda, es como un switch pero mejor, no bucle
            when(response.type){
                // Caso token
                AuthorizationResponse.Type.TOKEN -> {
                    val miToken = response.accessToken
                    Log.d("SpotifyWebAPI", "Exito, El token es:  $miToken")

                    val libreta = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
                    val editor = libreta.edit()
                    editor.putString("spotify_token", miToken)
                    editor.putBoolean("sesion_iniciada", true) // Lo puedes dejar por si te sirve para otra cosa
                    editor.apply()

                    irMusicaFragment()
                }
                // Caso Error
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("SpotifyWebAPI", "Error al iniciar sesion: ${response.error}")
                }
                // Caso dflt, se pulsa una X o se va hacia atrás
                else -> {
                    Log.d("SpotifyWebAPI", "El usuario ha cancelado")
                }
            }
        }
    }

    // --- Pedir Token Spotify ---
    private fun iniciarSesionSpotify(){
        //Creamos el constructor abierto para poder meterle parametros (p.e scopes)
        val builder = AuthorizationRequest.Builder(CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI)

        builder.setScopes(arrayOf(
            "app-remote-control",
            "user-read-playback-state",
            "user-modify-playback-state"
        ))
        builder.setShowDialog(true)
        // Una vez metidos, cerramos con build() y enviamos, la request, y un identificador
        val request = builder.build()
        val intent = AuthorizationClient.createLoginActivityIntent(requireActivity(), request)
        startActivityForResult(intent, REQUEST_CODE)
    }

    // --- Cambiar de Fragment
    private fun irMusicaFragment(){
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MusicaFragment())
            .commit()
    }
}