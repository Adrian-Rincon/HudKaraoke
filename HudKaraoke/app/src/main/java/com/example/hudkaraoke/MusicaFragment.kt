package com.example.hudkaraoke

import android.content.Context.MODE_PRIVATE
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicaFragment : Fragment(R.layout.fragment_musica) {

    private var reproduciendo = false
    private var modoEspejo = false
    private var pantallaCompleta = false

    //Tipo job, corrutina, asincrona de muy bajo consumo.
    private var relojJob: kotlinx.coroutines.Job? = null
    private var tiempoActualMl = 0L
    private var tiempoUltimoClickAnterior = 0L
    private var duracionTotalMl = 0L

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
        val btnSiguiente = view.findViewById<ImageButton>(R.id.btn_siguiente)
        val btnAnterior = view.findViewById<ImageButton>(R.id.btn_anterior)
        val barraProgreso = view.findViewById<android.widget.SeekBar>(R.id.barra_progreso)

        // --- LOGICA COLOR LETRAS
        val tvLetra = view.findViewById<android.widget.TextView>(R.id.tv_letra)
        listaLetras = procesarLRC(lrcDePrueba)
        textoLimpioCompleto = listaLetras.joinToString("\n") { it.texto }
        tvLetra.text = textoLimpioCompleto

        // --- LÓGICA PLAY/PAUSE ---
        btnPlayPause.setOnClickListener {
            // 1. Rescatamos el token para poder dar la orden
            val tokenGuardado = rescatarToken()

            if (tokenGuardado != null) {
                // 2. Invertimos el estado (si estaba sonando, lo pausamos, y viceversa)
                reproduciendo = !reproduciendo

                // 3. Cambiamos el icono al instante para que la app parezca muy rápida
                btnPlayPause.setImageResource(
                    if (reproduciendo) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )

                // 4. Control del Reloj del karaoke
                if (reproduciendo) {
                    iniciarRelojFalso()
                } else {
                    relojJob?.cancel()
                }

                // 5. ¡Mandamos la orden a Spotify en segundo plano!
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        if (reproduciendo) {
                            val respuesta = RetrofitCliente.apiService.reproducirMusica("Bearer $tokenGuardado")
                            if (!respuesta.isSuccessful) Log.e("SpotifyAPI", "Fallo al reproducir: ${respuesta.code()}")
                        } else {
                            val respuesta = RetrofitCliente.apiService.pausarMusica("Bearer $tokenGuardado")
                            if (!respuesta.isSuccessful) Log.e("SpotifyAPI", "Fallo al pausar: ${respuesta.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("SpotifyAPI", "Error enviando orden: ${e.message}")
                    }
                }
            } else {
                Log.e("SpotifyAPI", "No se puede dar la orden: falta el token")
            }
        }

        // --- LOGICA SIGUIENTE ---
        btnSiguiente.setOnClickListener {
            val token = rescatarToken()
            if (token != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val respuesta = RetrofitCliente.apiService.siguienteCancion("Bearer $token")
                        if (respuesta.isSuccessful) {
                            // Esperamos medio segundo a que Spotify cambie de canción
                            // y pedimos los datos nuevos para actualizar los textos de la pantalla
                            delay(500)
                            obtenerCancionActual(token)
                        }
                    } catch (e: Exception) {
                        Log.e("SpotifyAPI", "Error al pasar canción: ${e.message}")
                    }
                }
            }
        }

        // --- LOGICA ANTERIOR ---
        btnAnterior.setOnClickListener {
            val token = rescatarToken()
            if (token != null) {
                val tiempoActual = System.currentTimeMillis()

                // Si la diferencia entre este toque y el último es menor a 500ms -> DOBLE CLICK
                if (tiempoActual - tiempoUltimoClickAnterior < 500) {

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val respuesta = RetrofitCliente.apiService.cancionAnterior("Bearer $token")
                            if (respuesta.isSuccessful) {
                                kotlinx.coroutines.delay(500)
                                obtenerCancionActual(token)
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyAPI", "Error al retroceder canción: ${e.message}")
                        }
                    }

                } else {
                    // UN SOLO CLICK -> Volver al principio de la canción (0 milisegundos)
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val respuesta = RetrofitCliente.apiService.irATiempo("Bearer $token", 0L)
                            if (respuesta.isSuccessful) {
                                // Reiniciamos tu reloj de letras falso para que la letra vuelva al inicio
                                tiempoActualMl = 0L
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyAPI", "Error al reiniciar canción: ${e.message}")
                        }
                    }
                }

                // Actualizamos el registro del último toque
                tiempoUltimoClickAnterior = tiempoActual
            }
        }

        barraProgreso.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                // No hacemos nada mientras se mueve
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                // Pausamos el reloj para que no nos quite la barra del dedo
                relojJob?.cancel()
            }

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                // El usuario ha soltado el dedo. Calculamos a qué milisegundo quiere ir
                val nuevoTiempoMilis = seekBar?.progress?.toLong() ?: 0L
                val token = rescatarToken()

                if (token != null) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val respuesta = RetrofitCliente.apiService.irATiempo("Bearer $token", nuevoTiempoMilis)

                            if (respuesta.isSuccessful) {
                                tiempoActualMl = nuevoTiempoMilis

                                // Volvemos al hilo principal para tocar el reloj
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    iniciarRelojFalso()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("SpotifyAPI", "Error al saltar en la barra: ${e.message}")
                        }
                    }
                }
            }
        })

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

        // --- LLAMADAS A SPOTIFY
        val tokenGuardado = rescatarToken()


        if (tokenGuardado != null) {
            // Si tenemos token, llamamos a Spotify
            obtenerCancionActual(tokenGuardado)
        } else {
            Log.e("SpotifyAPI", "Ojo, no hay token guardado. Expulsando a Inicio...")

            // Si no hay token, lo mandamos inmediatamente a iniciar sesión
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InicioFragment())
                .commitAllowingStateLoss()
        }
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
        //Borramos posibles relojes de otras cancniones
        relojJob?.cancel()
        // Lanzamos el job, atando su vida a los eventos de ciclo de vida, matando el job cuando haya un evento
        relojJob = viewLifecycleOwner.lifecycleScope.launch {

            while(true){
                //Delay != sleep, no bloquea, libera durante 100 mls
                delay(100)
                tiempoActualMl += 100
                view?.findViewById<android.widget.SeekBar>(R.id.barra_progreso)?.progress = tiempoActualMl.toInt()
                actualizarColorKaraoke()

                if (duracionTotalMl > 0L && tiempoActualMl >= duracionTotalMl) {
                    val token = rescatarToken()
                    if (token != null) {
                        // Le pedimos a Spotify los datos de la nueva canción
                        obtenerCancionActual(token)
                    }
                    // Paramos este reloj. Cuando obtenerCancionActual acabe, lo volverá a arrancar solo
                    relojJob?.cancel()
                }


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

    // --- PETICIONES CON SPOTIFY
    private fun obtenerCancionActual(token: String){
        // Depuracion
        //val tokenRoto = token + "X"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val respuesta = RetrofitCliente.apiService.getCancionActual("Bearer $token")

                if (respuesta.isSuccessful) {
                    val cuerpo = respuesta.body()

                    if (cuerpo?.cancion != null) {
                        val titulo = cuerpo.cancion.nombre
                        val artista = cuerpo.cancion.artistas.firstOrNull()?.nombre ?: "Desconocido"
                        Log.d("SpotifyAPI", "🎶 ¡ÉXITO! Está sonando: $titulo - $artista")

                        kotlinx.coroutines.withContext(Dispatchers.Main) {

                            // Buscamos los TextView de la pantalla
                            val tvTitulo = view?.findViewById<android.widget.TextView>(R.id.tv_nombre_cancion)
                            val tvAutor = view?.findViewById<android.widget.TextView>(R.id.tv_autor)
                            val btnPlayPause = view?.findViewById<ImageButton>(R.id.btn_play_pause)
                            val barraProgreso = view?.findViewById<android.widget.SeekBar>(R.id.barra_progreso)

                            duracionTotalMl = cuerpo.cancion?.duracionMilisegundos ?: 0L
                            barraProgreso?.max = duracionTotalMl.toInt()

                            // Sincronizamos nuestro reloj interno y la barra con el progreso real de Spotify
                            tiempoActualMl = cuerpo.progresoMilisegundos
                            barraProgreso?.progress = tiempoActualMl.toInt()

                            // Escribimos los datos de Spotify
                            tvTitulo?.text = titulo
                            tvAutor?.text = artista
                            // Forzamos la selección para que el texto largo empiece a moverse
                            tvTitulo?.isSelected = true
                            tvAutor?.isSelected = true
                            reproduciendo = cuerpo.estaReproduciendo

                            btnPlayPause?.setImageResource(
                                if (reproduciendo) android.R.drawable.ic_media_pause
                                else android.R.drawable.ic_media_play
                            )

                            // Si la canción ya estaba sonando, arrancamos tu reloj de letras automático
                            if (reproduciendo) {
                                iniciarRelojFalso()
                            } else {
                                relojJob?.cancel()
                            }
                        }
                    }
                } else if (respuesta.code() == 401) {
                    Log.e("SpotifyAPI", "Token caducado (Error 401). Borrando datos...")

                    val libreta = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
                    libreta.edit().remove("spotify_token").apply()

                    // Usamos withContext para cambiar al hilo principal sin perder la referencia
                    kotlinx.coroutines.withContext(Dispatchers.Main) {

                        // isAdded comprueba que el usuario no haya cerrado la app justo en este segundo
                        if (isAdded) {
                            // 1. Creamos la instancia del fragmento
                            val fragmentoInicio = InicioFragment()

                            // 2. Le metemos el "mensaje secreto" en la mochila
                            val notaSecreta = Bundle()
                            notaSecreta.putBoolean("modo_auto_refresco", true)
                            fragmentoInicio.arguments = notaSecreta

                            // 3. Hacemos el cambio de pantalla enviando el fragmento con la nota
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragmentoInicio)
                                .commitAllowingStateLoss()
                        }
                    }
                } else {
                    Log.e("SpotifyAPI", "Error del servidor: ${respuesta.code()}")
                }
            } catch (e: Exception) {
                Log.e("SpotifyAPI", "Error de internet o código: ${e.message}")
            }
        }
    }

    private fun rescatarToken(): String? {
        val libreta = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
        return libreta.getString("spotify_token", null)
    }

}

//clase aux para colorear letras
data class LineaKaraoke(
    val tiempoMilisegundos: Long,
    val texto: String
)
