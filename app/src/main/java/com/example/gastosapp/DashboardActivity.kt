package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gastosapp.Fragments.FragmentGasto
import com.example.gastosapp.Fragments.FragmentInicio
import com.example.gastosapp.Fragments.FragmentPerfil
import com.example.gastosapp.Fragments.FragmentPresupuesto
import com.example.gastosapp.Fragments.FragmentResumen
import com.example.gastosapp.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fragmento por defecto
        verFragmentoInicio()

        binding.bottomNV.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_inicio -> {
                    verFragmentoInicio()
                    true
                }

                R.id.item_gasto -> {
                    verFragmentoGasto()
                    true
                }

                R.id.item_presupuesto -> {
                    verFragmentoPresupuesto()
                    true
                }

                R.id.item_resumen -> {
                    verFragmentoResumen()
                    true
                }

                R.id.item_perfil -> {
                    val intent = Intent(this, PerfilActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> {
                    true
                }
            }
        }
    }

    private fun verFragmentoInicio() {
        binding.tvTitulo.text = "Inicio"

        val fragmentInicio = FragmentInicio()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFl.id, fragmentInicio, "Fragment Inicio")
        fragmentTransaction.commit()
    }

    private fun verFragmentoGasto() {
        binding.tvTitulo.text = "Gasto"

        val fragmentGasto = FragmentGasto()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFl.id, fragmentGasto, "Fragment Gasto")
        fragmentTransaction.commit()
    }

    private fun verFragmentoPresupuesto() {
        binding.tvTitulo.text = "Presupuesto"

        val fragmentPresupuesto = FragmentPresupuesto()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFl.id, fragmentPresupuesto, "Fragment Presupuesto")
        fragmentTransaction.commit()
    }

    private fun verFragmentoResumen() {
        binding.tvTitulo.text = "Resumen"

        val fragmentResumen = FragmentResumen()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFl.id, fragmentResumen, "Fragment Resumen")
        fragmentTransaction.commit()
    }

    private fun verFragmentoPerfil() {
        binding.tvTitulo.text = "Perfil"

        val fragmentPerfil = FragmentPerfil()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentoFl.id, fragmentPerfil, "Fragment Perfil")
        fragmentTransaction.commit()
    }
}