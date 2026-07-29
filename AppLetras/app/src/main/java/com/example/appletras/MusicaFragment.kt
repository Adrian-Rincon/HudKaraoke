package com.example.appletras

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager

class MusicaFragment : Fragment(R.layout.fragment_musica) {

    private var reproduciendo = false
    private var modoEspejo = false
    private var pantallaCompleta = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPlayPause = view.findViewById<ImageButton>(R.id.btn_play_pause)
        val btnMirrorNormal = view.findViewById<ImageButton>(R.id.btn_mirror)
        val btnMirrorFullscreen = view.findViewById<ImageButton>(R.id.btn_mirror_fullscreen)
        val btnExpandir = view.findViewById<ImageButton>(R.id.btn_expandir)
        val scrollLetra = view.findViewById<ScrollView>(R.id.scroll_letra)

        // --- LÓGICA PLAY/PAUSE ---
        btnPlayPause.setOnClickListener {
            reproduciendo = !reproduciendo
            btnPlayPause.setImageResource(
                if (reproduciendo) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }

        // --- LÓGICA ESPEJO ---
        val accionEspejo = {
            modoEspejo = !modoEspejo
            scrollLetra.scaleX = if (modoEspejo) -1f else 1f
            val colorActivo = 0xFF1DB954.toInt()
            val colorInactivo = 0xFFB3B3B3.toInt()
            btnMirrorNormal.setColorFilter(if (modoEspejo) colorActivo else colorInactivo)
            btnMirrorFullscreen.setColorFilter(if (modoEspejo) colorActivo else colorInactivo)
        }
        btnMirrorNormal.setOnClickListener { accionEspejo() }
        btnMirrorFullscreen.setOnClickListener { accionEspejo() }

        // --- LÓGICA PANTALLA COMPLETA ---
        btnExpandir.setOnClickListener {
            pantallaCompleta = !pantallaCompleta
            TransitionManager.beginDelayedTransition(view as ViewGroup)
            actualizarInterfaz() // Llamamos al cerebro para que aplique los cambios
        }

        // Llamamos al cerebro la primera vez que se crea la pantalla por si el móvil ya estaba girado
        actualizarInterfaz()
    }

    // --- EL CEREBRO DE LA INTERFAZ ---
    private fun actualizarInterfaz() {
        val view = view ?: return
        val miniReproductor = view.findViewById<View>(R.id.mini_reproductor)
        val btnEspejoFullscreen = view.findViewById<ImageButton>(R.id.btn_mirror_fullscreen)
        val tvLetra = view.findViewById<android.widget.TextView>(R.id.tv_letra)
        // Detectamos si el móvil está en horizontal
        val estaEnHorizontal = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // 1. GESTIÓN DE LA BARRA DE NAVEGACIÓN (Maps/Spotify)
        // Se esconde si estamos en Pantalla Completa O si está en Horizontal
        val mostrarNavegacion = !pantallaCompleta && !estaEnHorizontal
        val visibilidadNav = if (mostrarNavegacion) View.VISIBLE else View.GONE

        requireActivity().findViewById<View>(R.id.muro_central).visibility = visibilidadNav
        requireActivity().findViewById<View>(R.id.contenedor_maps).visibility = visibilidadNav
        requireActivity().findViewById<View>(R.id.contenedor_spotify).visibility = visibilidadNav

        //----------2. GESTIÓN DEL TAMAÑO DE LA LETRA (¡Lo nuevo!)
        // Si estamos en horizontal O en pantalla completa, hacemos la letra gigante
        if (estaEnHorizontal ) {
            tvLetra.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 58f) // Tamaño Mega Karaoke
        } else if(pantallaCompleta){

            tvLetra.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 38f) // Tamaño Completo
        } else {
            tvLetra.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 28f) // Tamaño Normal
        }


        // ----------3. GESTIÓN DEL REPRODUCTOR Y BOTONES SUPERIORES
        // SOLO dependen del botón de pantalla completa
        if (pantallaCompleta) {
            miniReproductor.visibility = View.GONE
            requireActivity().findViewById<View>(R.id.btn_ajustes).visibility = View.GONE
            btnEspejoFullscreen.visibility = View.VISIBLE
        } else {
            miniReproductor.visibility = View.VISIBLE
            requireActivity().findViewById<View>(R.id.btn_ajustes).visibility = View.VISIBLE
            btnEspejoFullscreen.visibility = View.GONE
        }
    }

    // --- DETECTOR DE GIRO DEL MÓVIL ---
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Si el usuario gira el móvil, simplemente le decimos al cerebro que recalcule
        TransitionManager.beginDelayedTransition(view as ViewGroup)
        actualizarInterfaz()
    }
}