package com.example.appletras

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Referencias a los botones
        val spotifyButton: ImageButton = findViewById(R.id.btn_spotify)
        val mapsButton: ImageButton = findViewById(R.id.btn_maps)
        val youtubeMusicButton: ImageButton = findViewById(R.id.btn_youtube_music)

        // Configuración de clics
        spotifyButton.setOnClickListener {
            openApp("com.spotify.music") // Paquete de Spotify
        }

        mapsButton.setOnClickListener {
            openApp("com.google.android.apps.maps") // Paquete de Google Maps
        }

        youtubeMusicButton.setOnClickListener {
            openApp("com.google.android.apps.youtube.music") // Paquete de YouTube Music
        }
    }

    /**
     * Intenta abrir una aplicación por su nombre de paquete.
     * Si no está instalada, abre su página en Google Play Store.
     */
    private fun openApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            // La aplicación está instalada, la abre
            startActivity(intent)
        } else {
            // La aplicación NO está instalada, abre Play Store
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")))
            } catch (e: android.content.ActivityNotFoundException) {
                // Si Play Store tampoco está disponible, abre el navegador web
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
    }
}