package com.example.appletras

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicaFragment : Fragment(R.layout.fragment_musica) {

    private var reproduciendo = false
    private var modoEspejo = false
    private var pantallaCompleta = false

    private var tiempoActualMl = 0L

    //Tipo job, corrutina, asincrona de muy bajo consumo.
    private var relojJob: kotlinx.coroutines.Job? = null

    private var listaLetras: List<LineaKaraoke> = emptyList()
    private var textoLimpioCompleto: String = ""
    private val lrcDePrueba = """
        [00:00.00] (Intro musical)
        [00:03.50] Obediente, un border collie
        [00:06.20] La pinga de tu ex era un boli (boli BIC)
        [00:09.10] Chiquita, pero peligrosa
        [00:12.00] La aprieto y salpica, la llamo Lil Uzi
        [00:15.50] Te pongo en cuatro millones
    """.trimIndent()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPlayPause = view.findViewById<ImageButton>(R.id.btn_play_pause)
        val btnMirrorNormal = view.findViewById<ImageButton>(R.id.btn_mirror)
        val btnMirrorFullscreen = view.findViewById<ImageButton>(R.id.btn_mirror_fullscreen)
        val btnExpandir = view.findViewById<ImageButton>(R.id.btn_expandir)
        val scrollLetra = view.findViewById<ScrollView>(R.id.scroll_letra)

        // --- LOGICA COLOR LETRAS
        val tvLetra = view.findViewById<android.widget.TextView>(R.id.tv_letra)
        listaLetras = procesarLRC(lrcDePrueba)
        textoLimpioCompleto = listaLetras.joinToString("\n") { it.texto }
        tvLetra.text = textoLimpioCompleto

        // --- LÓGICA PLAY/PAUSE ---
        btnPlayPause.setOnClickListener {
            reproduciendo = !reproduciendo
            btnPlayPause.setImageResource(

                if (reproduciendo)
                    android.R.drawable.ic_media_pause
                else
                    android.R.drawable.ic_media_play
            )

            //Control del Reloj
            if(reproduciendo)
                iniciarRelojFalso()
            else
                relojJob?.cancel()
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

    // --- LOGICA DE RELOJ Y TAL ---
    private fun iniciarRelojFalso (){
        // Lanzamos el job, atando su vida a los eventos de ciclo de vida, matando el job cuando haya un evento
        relojJob = viewLifecycleOwner.lifecycleScope.launch {

            while(true){

                //Delay != sleep, no bloquea, libera durante 100 mls
                delay(100)
                tiempoActualMl += 100
                actualizarColorKaraoke()


            }
        }
    }

    // --- LOGICA DE TRADUCIR
    private fun procesarLRC(textoLRC: String): List<LineaKaraoke> {
        val listaTraducida = mutableListOf<LineaKaraoke>()
        val lineas = textoLRC.split("\n")

        for (linea in lineas) {
            // Buscamos si la línea tiene el formato [mm:ss.xx]
            val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)".toRegex()
            val match = regex.find(linea)

            if (match != null) {
                // Extraemos los números y la letra
                val minutos = match.groupValues[1].toLong()
                val segundos = match.groupValues[2].toLong()
                val centesimas = match.groupValues[3].toLong()
                val letra = match.groupValues[4].trim()

                // Pasamos todo a milisegundos
                val tiempoTotalMilis = (minutos * 60 * 1000) + (segundos * 1000) + (centesimas * 10)

                listaTraducida.add(LineaKaraoke(tiempoTotalMilis, letra))
            }
        }
        return listaTraducida
    }

    // --- LOGICA PINTAR LETRA
        private fun actualizarColorKaraoke() {
        val tvLetra = view?.findViewById<android.widget.TextView>(R.id.tv_letra) ?: return
        val scrollLetra = view?.findViewById<ScrollView>(R.id.scroll_letra)
        // 1. Buscamos qué línea toca reproducir ahora mismo.
        // (Es la última línea cuyo tiempo de inicio sea menor o igual al tiempo de nuestro reloj)
        val lineaActualIndex = listaLetras.indexOfLast { it.tiempoMilisegundos <= tiempoActualMl }

        if (lineaActualIndex != -1) {
            // Creamos un lienzo especial basado en nuestro texto limpio
            val spannable = SpannableString(textoLimpioCompleto)

            // 2. Calculamos dónde empieza la frase exacta contando los caracteres anteriores
            var inicioChar = 0
            for (i in 0 until lineaActualIndex) {
                inicioChar += listaLetras[i].texto.length + 1 // Sumamos +1 por el salto de línea (\n)
            }

            // Dónde acaba es simplemente el inicio + la longitud de la frase actual
            val finChar = inicioChar + listaLetras[lineaActualIndex].texto.length

            // 3. Aplicamos el color verde brillante de Spotify a ese trozo exacto
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#1DB954")),
                inicioChar,
                finChar,
                0
            )
            tvLetra.text = spannable
            val layoutMapa = tvLetra.layout
            if (layoutMapa != null) {
                // 1. Averiguamos en qué línea de la pantalla cae nuestro inicioChar
                val lineaPantalla = layoutMapa.getLineForOffset(inicioChar)

                // 2. Le preguntamos a qué altura vertical (eje Y) está esa línea
                val posicionY = layoutMapa.getLineTop(lineaPantalla)

                // 3. Movemos el ScrollView suavemente hacia esa altura
                // (Le restamos 200 píxeles para que la frase quede un poco centrada y no pegada arriba del todo)
                scrollLetra?.smoothScrollTo(0, posicionY - 200)
            }

        }
    }
}

//clase aux para colorear letras
data class LineaKaraoke(
    val tiempoMilisegundos: Long,
    val texto: String
)
