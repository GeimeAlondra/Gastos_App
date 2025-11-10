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
import com.example.gastosapp.R
import com.example.gastosapp.viewModels.GastoViewModel
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.*

class FragmentResumen : Fragment() {

    private lateinit var gastoViewModel: GastoViewModel
    private lateinit var presupuestoViewModel: PresupuestoViewModel

    private lateinit var tvGastosSemana: TextView
    private lateinit var tvTotalActivos: TextView
    private lateinit var tvPresupuestoTotal: TextView
    private lateinit var tvGastadoTotal: TextView
    private lateinit var containerCategorias: LinearLayout

    companion object {
        private const val TAG = "FragmentResumen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        gastoViewModel = ViewModelProvider(requireActivity())[GastoViewModel::class.java]
        presupuestoViewModel = ViewModelProvider(requireActivity())[PresupuestoViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView")
        return inflater.inflate(R.layout.fragment_resumen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        initViews(view)
        setupObservers()
    }

    private fun initViews(view: View) {
        tvGastosSemana = view.findViewById(R.id.tvGastosSemana)
        tvTotalActivos = view.findViewById(R.id.tvTotalActivos)
        tvPresupuestoTotal = view.findViewById(R.id.tvPresupuestoTotal)
        tvGastadoTotal = view.findViewById(R.id.tvGastadoTotal)
        containerCategorias = view.findViewById(R.id.containerCategorias)
    }

    private fun setupObservers() {
        Log.d(TAG, "Configurando observadores...")

        gastoViewModel.gastos.observe(viewLifecycleOwner) { gastos ->
            Log.d(TAG, "  Gastos actualizados: ${gastos.size} gastos")
            actualizarResumenSemanal(gastos)
            actualizarResumenCategorias(gastos) // NUEVA LÍNEA
        }

        presupuestoViewModel.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            Log.d(TAG, "  Presupuestos actualizados: ${presupuestos.size} presupuestos")
            calcularActivos(presupuestos)
        }
    }

    private fun actualizarResumenSemanal(gastos: List<com.example.gastosapp.Models.Gasto>) {
        try {
            val gastosSemana = obtenerGastosUltimaSemana(gastos)
            val totalSemanal = gastosSemana.sumOf { it.monto }
            tvGastosSemana.text = String.format("$%.2f", totalSemanal)
            Log.d(TAG, "  Resumen semanal: $${String.format("%.2f", totalSemanal)}")
        } catch (e: Exception) {
            Log.e(TAG, "  Error en actualizarResumenSemanal: ${e.message}")
        }
    }

    private fun actualizarResumenCategorias(gastos: List<com.example.gastosapp.Models.Gasto>) {
        try {
            // Limpiar contenedor
            containerCategorias.removeAllViews()

            // Agrupar gastos por categoría
            val gastosPorCategoria = gastos.groupBy { it.categoriaId }
                .mapValues { (_, gastosCategoria) -> gastosCategoria.sumOf { it.monto } }

            // Ordenar por monto (de mayor a menor)
            val categoriasOrdenadas = gastosPorCategoria.entries.sortedByDescending { it.value }

            if (categoriasOrdenadas.isEmpty()) {
                // Mostrar mensaje si no hay gastos
                val tvMensaje = TextView(requireContext()).apply {
                    text = "No hay gastos registrados por categoría"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 32, 0, 32)
                }
                containerCategorias.addView(tvMensaje)
                return
            }

            // Crear item por cada categoría
            categoriasOrdenadas.forEach { (categoriaId, total) ->
                if (total > 0) {
                    val itemCategoria = crearItemCategoria(categoriaId, total)
                    containerCategorias.addView(itemCategoria)
                }
            }

            Log.d(TAG, "  Resumen categorías actualizado: ${categoriasOrdenadas.size} categorías")

        } catch (e: Exception) {
            Log.e(TAG, "  Error en actualizarResumenCategorias: ${e.message}")
        }
    }

    private fun crearItemCategoria(categoriaId: Int, total: Double): View {
        val inflater = LayoutInflater.from(requireContext())
        val itemView = inflater.inflate(R.layout.item_categorias, containerCategorias, false)

        val tvNombreCategoria = itemView.findViewById<TextView>(R.id.tvNombreCategoria)
        val tvMontoCategoria = itemView.findViewById<TextView>(R.id.tvMontoCategoria)
        val progressBar = itemView.findViewById<View>(R.id.progressBarCategoria)

        // Obtener nombre de la categoría
        val nombreCategoria = Categorias.getNombrePorId(categoriaId)
        tvNombreCategoria.text = nombreCategoria
        tvMontoCategoria.text = String.format("$%.2f", total)

        // Calcular porcentaje del total (para la barra de progreso visual)
        val gastosTotales = gastoViewModel.gastos.value?.sumOf { it.monto } ?: 0.0
        val porcentaje = if (gastosTotales > 0) (total / gastosTotales) * 100 else 0.0

        // Configurar barra de progreso visual
        val layoutParams = progressBar.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = porcentaje.toFloat()
        progressBar.layoutParams = layoutParams

        // Color según la categoría (puedes personalizar esto)
        val color = obtenerColorPorCategoria(categoriaId)
        progressBar.setBackgroundColor(color)

        return itemView
    }

    private fun obtenerColorPorCategoria(categoriaId: Int): Int {
        val colores = listOf(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_light),
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_light),
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light),
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light),
            ContextCompat.getColor(requireContext(), android.R.color.holo_purple),
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark),
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
        )
        return colores[categoriaId % colores.size]
    }

    private fun calcularActivos(presupuestos: List<com.example.gastosapp.Models.Presupuesto>) {
        try {
            val totalPresupuesto = presupuestos.sumOf { it.cantidad }
            val totalGastado = presupuestos.sumOf { it.montoGastado ?: 0.0 }
            val activos = totalPresupuesto - totalGastado

            tvPresupuestoTotal.text = String.format("$%.2f", totalPresupuesto)
            tvGastadoTotal.text = String.format("$%.2f", totalGastado)
            tvTotalActivos.text = String.format("$%.2f", activos)

            Log.d(TAG, "  Activos calculados: Presupuesto=$$totalPresupuesto, Gastado=$$totalGastado, Activos=$$activos")
        } catch (e: Exception) {
            Log.e(TAG, "  Error en calcularActivos: ${e.message}")
        }
    }

    private fun obtenerGastosUltimaSemana(gastos: List<com.example.gastosapp.Models.Gasto>): List<com.example.gastosapp.Models.Gasto> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val fechaLimite = calendar.time

        return gastos.filter { gasto ->
            val fechaGasto = parsearFecha(gasto.fecha)
            fechaGasto?.after(fechaLimite) == true || fechaGasto?.equals(fechaLimite) == true
        }
    }

    private fun parsearFecha(fecha: String?): Date? {
        if (fecha.isNullOrEmpty()) return null
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fecha)
        } catch (e: Exception) {
            null
        }
    }


}