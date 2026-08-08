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
    private var monitorJob: kotlinx.coroutines.Job? = null
    private var tiempoActualMl = 0L
    private var tiempoUltimoClickAnterior = 0L
    private var duracionTotalMl = 0L

    private var tituloSonandoAhora: String = ""
    private var listaLetras = mutableListOf<LineaKaraoke>()
    private var textoLimpioCompleto: String = ""
    private val lrcDePrueba = """
        [00:00.00] Cargando Letra...
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
        procesarLRC(lrcDePrueba)
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
                            val respuesta = RetrofitClienteSpotify.apiService.reproducirMusica("Bearer $tokenGuardado")
                            if (!respuesta.isSuccessful) Log.e("SpotifyAPI", "Fallo al reproducir: ${respuesta.code()}")
                        } else {
                            val respuesta = RetrofitClienteSpotify.apiService.pausarMusica("Bearer $tokenGuardado")
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
                        val respuesta = RetrofitClienteSpotify.apiService.siguienteCancion("Bearer $token")
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
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // 1. Si llevamos más de 3 segundos (3000 ms), REINICIAMOS la canción
                        if (tiempoActualMl > 3000L) {
                            val respuesta = RetrofitClienteSpotify.apiService.irATiempo("Bearer $token", 0L)

                            if (respuesta.isSuccessful) {
                                // Cambiamos el tiempo al inicio
                                tiempoActualMl = 0L

                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    // Subimos el texto del karaoke arriba del todo de golpe
                                    view?.findViewById<ScrollView>(R.id.scroll_letra)?.scrollTo(0, 0)

                                    // ¡CLAVE! Reiniciamos el reloj para que empiece a contar desde 0
                                    iniciarRelojFalso()
                                }
                            }
                        }
                        // 2. Si llevamos menos de 3 segundos, pasamos a la ANTERIOR de verdad
                        else {
                            val respuesta = RetrofitClienteSpotify.apiService.cancionAnterior("Bearer $token")

                            if (respuesta.isSuccessful) {
                                // Le damos a Spotify 800ms (un poco más que antes) para asegurar
                                // que cambia de canción en sus servidores antes de pedir los datos
                                delay(800)
                                obtenerCancionActual(token)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SpotifyAPI", "Error en botón anterior: ${e.message}")
                    }
                }
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
                            val respuesta = RetrofitClienteSpotify.apiService.irATiempo("Bearer $token", nuevoTiempoMilis)

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
            iniciarMonitorSpotify()
        } else {
            Log.e("SpotifyAPI", "Ojo, no hay token guardado. Expulsando a Inicio...")

            // Si no hay token, lo mandamos inmediatamente a iniciar sesión
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InicioFragment())
                .commitAllowingStateLoss()
        }
    }

    override fun onResume() {
        super.onResume()

        // Cada vez que la app vuelve a primer plano, comprobamos el estado real
        val tokenGuardado = rescatarToken()

        if (tokenGuardado != null) {
            // Esto forzará a la app a descargar el título, el progreso exacto y la letra correcta
            obtenerCancionActual(tokenGuardado)
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

        val ventana = requireActivity().window
        val controladorVentana = androidx.core.view.WindowCompat.getInsetsController(ventana, ventana.decorView)

        if (pantallaCompleta) {
            // Escondemos la hora, batería y botones de navegación
            controladorVentana.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Hacemos que si el usuario desliza desde el borde, las barras salgan temporalmente y se vuelvan a esconder
            controladorVentana.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Volvemos a mostrar cuando salimos de pantalla completa
            controladorVentana.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
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
        // Borramos posibles relojes de otras canciones
        relojJob?.cancel()

        relojJob = viewLifecycleOwner.lifecycleScope.launch {
            // 1. Fichamos la hora exacta (del mundo real) a la que arranca el reloj
            val marcaTiempoInicio = System.currentTimeMillis()

            // 2. Guardamos de dónde partía la canción según Spotify en este momento
            val progresoInicial = tiempoActualMl

            while(true) {
                // Podemos bajar el delay a 50 para que el Scroll vaya ultra fluido
                delay(50)

                // 3. ¿Cuánto tiempo físico real ha pasado desde que arrancó el reloj?
                val tiempoFisicoTranscurrido = System.currentTimeMillis() - marcaTiempoInicio

                // 4. El tiempo actual es siempre: El inicio + El tiempo que ha pasado en la vida real
                // ¡Se acabaron los desfases por lag del procesador!
                tiempoActualMl = progresoInicial + tiempoFisicoTranscurrido

                view?.findViewById<android.widget.SeekBar>(R.id.barra_progreso)?.progress = tiempoActualMl.toInt()
                actualizarColorKaraoke()

                if (duracionTotalMl > 0L && tiempoActualMl >= duracionTotalMl) {
                    val token = rescatarToken()
                    if (token != null) {
                        obtenerCancionActual(token)
                    }
                    relojJob?.cancel()
                }
            }
        }
    }

    // --- LOGICA DE TRADUCIR
    private fun procesarLRC(letraCruda: String) {
        listaLetras.clear()

        val lineas = letraCruda.split("\n")

        for(linea in lineas){
            if (linea.startsWith("[")) {
                val cierreCorchete = linea.indexOf("]")
                if (cierreCorchete != -1) {
                    //Divide en antras del "]", tiempo y texto
                    val tiempoTexto = linea.substring(1, cierreCorchete)
                    val textoLetra = linea.substring(cierreCorchete + 1).trim()
                    //Pasa el tiempo a mlsegundos y lo mete en la lista
                    val partesTiempo = tiempoTexto.split(":")
                    if (partesTiempo.size == 2) {
                        val minutos = partesTiempo[0].toLongOrNull() ?: 0L
                        val segundosMilis = partesTiempo[1].toDoubleOrNull() ?: 0.0

                        val tiempoEnMilisegundos = (minutos * 60 * 1000) + (segundosMilis * 1000).toLong()

                        listaLetras.add(LineaKaraoke(tiempoEnMilisegundos, textoLetra))
                    }
                }
            }
        }
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
                scrollLetra?.smoothScrollTo(0, posicionY - 50)
            }

        }
    }

    // --- PETICIONES CON SPOTIFY
    private fun obtenerCancionActual(token: String){
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            consultarYActualizarSpotify(token)
        }
    }

    private suspend fun consultarYActualizarSpotify(token: String) {
        try {
            val respuesta = RetrofitClienteSpotify.apiService.getCancionActual("Bearer $token")

            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()

                if (cuerpo?.cancion != null) {
                    val titulo = cuerpo.cancion.nombre
                    val artista = if (cuerpo.cancion.artistas.isNotEmpty()) {
                        cuerpo.cancion.artistas.joinToString(", ") { it.nombre }
                    } else {
                        "Desconocido"
                    }

                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        // 1. Actualizamos botones y barra de progreso (Esto va siempre)
                        actualizarInterfazReproductor(cuerpo, titulo, artista)

                        // 2. Filtro de Letras: Solo descarga si la canción cambia
                        if (titulo != tituloSonandoAhora) {
                            Log.d("SpotifyAPI", "🎶 Nueva canción detectada: $titulo")
                            tituloSonandoAhora = titulo
                            buscarYMostrarLetra(titulo, artista)
                        }
                    }
                }
            } else if (respuesta.code() == 401) {
                // --- GESTIÓN DE TOKEN CADUCADO ---
                Log.e("SpotifyAPI", "Token caducado (Error 401). Borrando datos...")
                val libreta = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
                libreta.edit().remove("spotify_token").apply()

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (isAdded) {
                        val fragmentoInicio = InicioFragment()
                        val notaSecreta = Bundle()
                        notaSecreta.putBoolean("modo_auto_refresco", true)
                        fragmentoInicio.arguments = notaSecreta

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragmentoInicio)
                            .commitAllowingStateLoss()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SpotifyAPI", "Error silencioso al consultar Spotify: ${e.message}")
        }
    }

    private fun iniciarMonitorSpotify() {
        monitorJob?.cancel() // Borramos monitores antiguos por seguridad

        monitorJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            while(true) {
                delay(5000) // Espera 5 segundos

                val token = rescatarToken()
                if (token != null) {
                    // Llama al cerebro silenciosamente
                    consultarYActualizarSpotify(token)
                }
            }
        }
    }

    // Auxiliar de obtenerCancionActual, encargada del reproductor
    private fun actualizarInterfazReproductor(cuerpo: CancionActualResponse?, titulo: String, artista: String) {
        val tvTitulo = view?.findViewById<android.widget.TextView>(R.id.tv_nombre_cancion)
        val tvAutor = view?.findViewById<android.widget.TextView>(R.id.tv_autor)
        val btnPlayPause = view?.findViewById<android.widget.ImageButton>(R.id.btn_play_pause)
        val barraProgreso = view?.findViewById<android.widget.SeekBar>(R.id.barra_progreso)

        // 1. Textos
        tvTitulo?.text = titulo
        tvAutor?.text = artista
        tvTitulo?.isSelected = true
        tvAutor?.isSelected = true

        // 2. Play/Pause
        reproduciendo = cuerpo?.estaReproduciendo ?: false
        btnPlayPause?.setImageResource(
            if (reproduciendo) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )

        // 3. Barra de progreso y reloj interno
        duracionTotalMl = cuerpo?.cancion?.duracionMilisegundos ?: 0L
        barraProgreso?.max = duracionTotalMl.toInt()

        tiempoActualMl = cuerpo?.progresoMilisegundos ?: 0L
        barraProgreso?.progress = tiempoActualMl.toInt()

        if (reproduciendo) {
            iniciarRelojFalso()
        } else {
            relojJob?.cancel()
        }
    }

    // Auxiliar de obtenerCancionActual, encargada de buscar la letra en LcrLib
    private fun buscarYMostrarLetra(titulo: String, artista: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {


                // 2. Creamos una consulta abierta tipo: "Bohemian Rhapsody Queen"
                val consulta = "$titulo $artista"

                // 3. Usamos el nuevo endpoint de búsqueda
                val respuestaLetra = RetrofitClienteLrcLib.apiService.buscarLetra(consulta)

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (respuestaLetra.isSuccessful) {
                        val listaResultados = respuestaLetra.body()

                        // 4. Buscamos en la lista el primer resultado que SÍ tenga letra sincronizada
                        val mejorResultado = listaResultados?.firstOrNull { !it.letraSincronizada.isNullOrEmpty() }
                        val letraSincronizada = mejorResultado?.letraSincronizada

                        if (letraSincronizada != null) {
                            procesarLRC(letraSincronizada)
                        } else {
                            procesarLRC("[00:00.00] Letra no sincronizada disponible 😔")
                        }
                    } else {
                        procesarLRC("[00:00.00] Letra no encontrada para esta canción 🔍")
                    }

                    // Actualizamos el TextView
                    textoLimpioCompleto = listaLetras.joinToString("\n") { it.texto }
                    view?.findViewById<android.widget.TextView>(R.id.tv_letra)?.text = textoLimpioCompleto
                    view?.findViewById<ScrollView>(R.id.scroll_letra)?.scrollTo(0, 0)
                }
            } catch (e: Exception) {
                Log.e("LRCLIB", "Error al buscar letra: ${e.message}")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    procesarLRC("[00:00.00] Error de conexión al buscar la letra 📡")
                    textoLimpioCompleto = listaLetras.joinToString("\n") { it.texto }
                    view?.findViewById<android.widget.TextView>(R.id.tv_letra)?.text = textoLimpioCompleto
                    view?.findViewById<ScrollView>(R.id.scroll_letra)?.scrollTo(0, 0)
                }
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
