package com.example.appletras

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

// Al poner el diseño entre paréntesis, nos ahorramos muchísimo código
class InicioFragment : Fragment(R.layout.fragment_inicio) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aquí conectaremos tu botón de "Iniciar sesión" más adelante
        // val btnIniciarSesion = view.findViewById<Button>(R.id.btn_iniciar_sesion)
    }
}