package com.example.appletras

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

// Al poner el diseño entre paréntesis, nos ahorramos muchísimo código
class InicioFragment : Fragment(R.layout.fragment_inicio) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       val btnIniciarSesion = view.findViewById<Button>(R.id.btn_iniciar_sesion)

        btnIniciarSesion.setOnClickListener{
            //Escribimos que la sesion esta inciada (TEMPORAL)
            val libreta = requireActivity().getSharedPreferences("PreferenciasApp", MODE_PRIVATE)
            val editor = libreta.edit()
            editor.putBoolean("sesion_iniciada", true)
            editor.apply()

            //Cambiamos al fragmento de múscia en vez de inicio
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MusicaFragment())
                .commit()
        }






    }
}