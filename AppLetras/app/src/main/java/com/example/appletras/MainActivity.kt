package com.example.appletras

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.TransitionManager

class MainActivity : AppCompatActivity() { // Los puntos son extends o implement

    /*En kotlin, primero va el nombre y despues el tipo. EL signo de interrogacion
    es porque puede ser NULL. El onCreate es para ver si hay datos y no tener que
    cargarlos de nuevo.
    * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Referencias a los botones
        val spotifyButton: ImageButton = findViewById(R.id.btn_spotify)
        val mapsButton: ImageButton = findViewById(R.id.btn_maps)
        val muro: androidx.constraintlayout.widget.Guideline = findViewById ( R.id.muro_central)
        val layoutPrincipal: ConstraintLayout = findViewById(R.id.principal)

        // Configuración de clics
        spotifyButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(layoutPrincipal)
            val params = muro.layoutParams as ConstraintLayout.LayoutParams
            params.guidePercent = 0.2f
            muro.layoutParams = params

        }

        mapsButton.setOnClickListener {
            TransitionManager.beginDelayedTransition(layoutPrincipal)
            val params = muro.layoutParams as ConstraintLayout.LayoutParams
            params.guidePercent = 0.8f
            muro.layoutParams = params
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