package com.example.gastosapp.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.gastosapp.Models.Categorias
import com.example.gastosapp.Models.Registro // Asegúrate de importar tu modelo
import com.example.gastosapp.R
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class FragmentInicio : Fragment() {

    private lateinit var gastoViewModel: GastoViewModel
    private lateinit var presupuestoViewModel: PresupuestoViewModel

    private lateinit var tvSaludo: TextView
    private lateinit var tvNombreUsuario: TextView
    private lateinit var tvPresupuestoTotalInicio: TextView
    private lateinit var tvGastadoTotalInicio: TextView
    private lateinit var tvSaldoDisponibleInicio: TextView
    private lateinit var containerGastosRecientes: LinearLayout

    companion object {
        private const val TAG = "FragmentInicio"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gastoViewModel = ViewModelProvider(requireActivity())[GastoViewModel::class.java]
        presupuestoViewModel = ViewModelProvider(requireActivity())[PresupuestoViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_inicio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupObservers()
        actualizarSaludo()
        obtenerNombreUsuarioDesdeBD() // CAMBIADO: Usar la misma lógica que FragmentPerfil
    }

    private fun initViews(view: View) {
        tvSaludo = view.findViewById(R.id.tvSaludo)
        tvNombreUsuario = view.findViewById(R.id.tvNombreUsuario)
        tvPresupuestoTotalInicio = view.findViewById(R.id.tvPresupuestoTotalInicio)
        tvGastadoTotalInicio = view.findViewById(R.id.tvGastadoTotalInicio)
        tvSaldoDisponibleInicio = view.findViewById(R.id.tvSaldoDisponibleInicio)
        containerGastosRecientes = view.findViewById(R.id.containerGastosRecientes)
    }

    private fun setupObservers() {
        presupuestoViewModel.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            actualizarResumenPresupuestos(presupuestos)
        }

        gastoViewModel.gastos.observe(viewLifecycleOwner) { gastos ->
            actualizarGastosRecientes(gastos)
        }
    }

    private fun actualizarSaludo() {
        val calendar = Calendar.getInstance()
        val hora = calendar.get(Calendar.HOUR_OF_DAY)

        val saludo = when (hora) {
            in 5..11 -> "¡Buenos días!"
            in 12..18 -> "¡Buenas tardes!"
            else -> "¡Buenas noches!"
        }
        tvSaludo.text = saludo
    }

    // NUEVO MÉTODO: Usar la misma lógica que FragmentPerfil
    private fun obtenerNombreUsuarioDesdeBD() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid != null) {
            val database = FirebaseDatabase.getInstance().reference

            // Mostrar "Cargando..." mientras se obtienen los datos
            tvNombreUsuario.text = "Cargando..."

            database.child("usuarios").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val usuario = snapshot.getValue(Registro::class.java)
                        usuario?.let {
                            tvNombreUsuario.text = it.nombre
                            Log.d(TAG, "Nombre de usuario obtenido: ${it.nombre}")
                        }
                    } else {
                        // Si no existe en la BD, usar el email como fallback
                        val user = FirebaseAuth.getInstance().currentUser
                        val nombreFallback = user?.email?.substringBefore("@") ?: "Usuario"
                        tvNombreUsuario.text = nombreFallback
                        Log.w(TAG, "No se encontraron datos del usuario en BD, usando: $nombreFallback")
                    }
                }
                .addOnFailureListener {
                    // En caso de error, usar el email como fallback
                    val user = FirebaseAuth.getInstance().currentUser
                    val nombreFallback = user?.email?.substringBefore("@") ?: "Usuario"
                    tvNombreUsuario.text = nombreFallback
                    Log.e(TAG, "Error al obtener datos del usuario: ${it.message}")
                }
        } else {
            tvNombreUsuario.text = "Invitado"
            Log.e(TAG, "No hay usuario autenticado")
        }
    }

    private fun actualizarResumenPresupuestos(presupuestos: List<com.example.gastosapp.Models.Presupuesto>) {
        try {
            val totalPresupuesto = presupuestos.sumOf { it.cantidad }
            val totalGastado = presupuestos.sumOf { it.montoGastado ?: 0.0 }
            val saldoDisponible = totalPresupuesto - totalGastado

            tvPresupuestoTotalInicio.text = String.format("$%.2f", totalPresupuesto)
            tvGastadoTotalInicio.text = String.format("$%.2f", totalGastado)
            tvSaldoDisponibleInicio.text = String.format("$%.2f", saldoDisponible)

            Log.d(TAG, "Resumen presupuestos actualizado: $${totalPresupuesto}")
        } catch (e: Exception) {
            Log.e(TAG, "Error en actualizarResumenPresupuestos: ${e.message}")
        }
    }

    private fun actualizarGastosRecientes(gastos: List<com.example.gastosapp.Models.Gasto>) {
        try {
            containerGastosRecientes.removeAllViews()

            // Ordenar gastos por fecha (más recientes primero) y tomar últimos 5
            val gastosRecientes = gastos.sortedByDescending {
                parsearFecha(it.fecha)
            }.take(5)

            if (gastosRecientes.isEmpty()) {
                val tvMensaje = TextView(requireContext()).apply {
                    text = "No hay gastos recientes"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                containerGastosRecientes.addView(tvMensaje)
                return
            }

            gastosRecientes.forEach { gasto ->
                val itemGasto = crearItemGastoReciente(gasto)
                containerGastosRecientes.addView(itemGasto)
            }

            Log.d(TAG, "Gastos recientes actualizados: ${gastosRecientes.size} gastos")

        } catch (e: Exception) {
            Log.e(TAG, "Error en actualizarGastosRecientes: ${e.message}")
        }
    }

    private fun crearItemGastoReciente(gasto: com.example.gastosapp.Models.Gasto): View {
        val inflater = LayoutInflater.from(requireContext())
        val itemView = inflater.inflate(R.layout.item_gasto_reciente, containerGastosRecientes, false)

        val tvNombreGasto = itemView.findViewById<TextView>(R.id.tvNombreGastoReciente)
        val tvMontoGasto = itemView.findViewById<TextView>(R.id.tvMontoGastoReciente)
        val tvCategoriaGasto = itemView.findViewById<TextView>(R.id.tvCategoriaGastoReciente)
        val tvFechaGasto = itemView.findViewById<TextView>(R.id.tvFechaGastoReciente)

        tvNombreGasto.text = gasto.nombre ?: "Sin nombre"
        tvMontoGasto.text = String.format("-$%.2f", gasto.monto)
        tvCategoriaGasto.text = Categorias.getNombrePorId(gasto.categoriaId)
        tvFechaGasto.text = gasto.fecha ?: "--/--/----"

        // Color rojo para los montos de gasto
        tvMontoGasto.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))

        return itemView
    }

    private fun parsearFecha(fecha: String?): Date? {
        if (fecha.isNullOrEmpty()) return Date(0) // Fecha muy antigua
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fecha)
        } catch (e: Exception) {
            Date(0)
        }
    }
}