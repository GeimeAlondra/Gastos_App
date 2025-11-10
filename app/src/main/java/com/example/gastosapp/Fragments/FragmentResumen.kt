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
import com.example.gastosapp.views.BarChartView

class FragmentResumen : Fragment() {

    private lateinit var gastoViewModel: GastoViewModel
    private lateinit var presupuestoViewModel: PresupuestoViewModel

    private lateinit var tvGastosSemana: TextView
    private lateinit var tvTotalActivos: TextView
    private lateinit var tvPresupuestoTotal: TextView
    private lateinit var tvGastadoTotal: TextView
    private lateinit var containerCategorias: LinearLayout

    private lateinit var chartDiario: BarChartView
    private lateinit var chartSemanal: BarChartView
    private lateinit var chartMensual: BarChartView

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

        chartDiario = view.findViewById(R.id.chartDiario)
        chartSemanal = view.findViewById(R.id.chartSemanal)
        chartMensual = view.findViewById(R.id.chartMensual)
    }

    private fun setupObservers() {
        Log.d(TAG, "Configurando observadores...")

        gastoViewModel.gastos.observe(viewLifecycleOwner) { gastos ->
            Log.d(TAG, "  Gastos actualizados: ${gastos.size} gastos")
            actualizarResumenSemanal(gastos)
            actualizarResumenCategorias(gastos)
            actualizarGraficas(gastos)
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

    private fun actualizarGraficas(gastos: List<com.example.gastosapp.Models.Gasto>) {
        try {
            actualizarGraficaDiaria(gastos)
            actualizarGraficaSemanal(gastos)
            actualizarGraficaMensual(gastos)
            Log.d(TAG, "✅ Gráficas actualizadas")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando gráficas: ${e.message}")
        }
    }

    // NUEVO: Gráfica Diaria - Días del mes actual
// MODIFICADO: Gráfica Diaria - Solo 7 días de la semana actual
    private fun actualizarGraficaDiaria(gastos: List<com.example.gastosapp.Models.Gasto>) {
        val gastosSemana = obtenerGastosSemanaActual(gastos)

        // Agrupar por día de la semana
        val gastosPorDia = mutableMapOf<String, Float>()
        val diasSemana = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

        // Inicializar todos los días en 0
        diasSemana.forEach { dia ->
            gastosPorDia[dia] = 0f
        }

        // Sumar gastos por día
        gastosSemana.forEach { gasto ->
            val dia = obtenerDiaDeLaSemana(gasto.fecha)
            gastosPorDia[dia] = gastosPorDia.getOrDefault(dia, 0f) + gasto.monto.toFloat()
        }

        // Crear datos para el BarChartView
        val datosDiarios = diasSemana.map { dia ->
            Pair(dia, gastosPorDia[dia] ?: 0f)
        }

        chartDiario.setData(datosDiarios)
        chartDiario.setTextSize(20f)
        chartDiario.setBarSpacing(0.15f)
        Log.d(TAG, "📊 Gráfica diaria actualizada: ${datosDiarios.size} días")
    }
    // MODIFICADO: Gráfica Semanal - Semanas del año (1-52)
// MODIFICADO: Gráfica Semanal - Solo últimas semanas
    private fun actualizarGraficaSemanal(gastos: List<com.example.gastosapp.Models.Gasto>) {
        // Agrupar por semana del año
        val gastosPorSemana = mutableMapOf<Int, Float>()

        // Sumar gastos por semana
        gastos.forEach { gasto ->
            val semana = obtenerSemanaDelAnio(gasto.fecha)
            gastosPorSemana[semana] = gastosPorSemana.getOrDefault(semana, 0f) + gasto.monto.toFloat()
        }

        // Solo mostrar las últimas 16 semanas con datos
        val semanaActual = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        val semanasRecientes = (semanaActual - 15..semanaActual).filter { it > 0 }

        val datosSemanales = semanasRecientes.map { semana ->
            Pair("Sem$semana", gastosPorSemana[semana] ?: 0f)
        }

        chartSemanal.setData(datosSemanales)
        chartSemanal.setTextSize(16f)
        chartSemanal.setBarWidthFactor(0.7f)
        chartSemanal.setBarSpacing(0.1f)
        Log.d(TAG, "📊 Gráfica semanal actualizada: ${datosSemanales.size} semanas")
    }
    // MODIFICADO: Gráfica Mensual - Meses del año
    private fun actualizarGraficaMensual(gastos: List<com.example.gastosapp.Models.Gasto>) {
        // Agrupar por mes del año
        val gastosPorMes = mutableMapOf<String, Float>()

        // Meses del año
        val mesesAnio = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

        // Inicializar todos los meses
        mesesAnio.forEach { mes ->
            gastosPorMes[mes] = 0f
        }

        // Sumar gastos por mes
        gastos.forEach { gasto ->
            val mes = obtenerMesDelAnio(gasto.fecha)
            if (mes in 1..12) {
                val claveMes = mesesAnio[mes - 1]
                gastosPorMes[claveMes] = gastosPorMes.getOrDefault(claveMes, 0f) + gasto.monto.toFloat()
            }
        }

        // Crear datos ordenados
        val datosMensuales = mesesAnio.map { mes ->
            Pair(mes, gastosPorMes[mes] ?: 0f)
        }

        chartMensual.setData(datosMensuales)
        chartMensual.setTextSize(20f)
        chartMensual.setBarWidthFactor(0.7f)
        chartDiario.setBarSpacing(0.1f)
        Log.d(TAG, "📊 Gráfica mensual actualizada: ${datosMensuales.size} meses")
    }

    // MÉTODOS AUXILIARES NUEVOS
    private fun obtenerDiasEnMesActual(): List<String> {
        val calendar = Calendar.getInstance()
        val diasEnMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..diasEnMes).map { it.toString() }
    }

    private fun obtenerSemanaDelAnio(fecha: String?): Int {
        if (fecha.isNullOrEmpty()) return 1
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(fecha)
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.get(Calendar.WEEK_OF_YEAR)
        } catch (e: Exception) {
            1
        }
    }

    private fun obtenerMesDelAnio(fecha: String?): Int {
        if (fecha.isNullOrEmpty()) return 1
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(fecha)
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.get(Calendar.MONTH) + 1 // 1-12
        } catch (e: Exception) {
            1
        }
    }

    // MÉTODOS EXISTENTES (los necesitamos para las otras funciones)
    private fun obtenerGastosSemanaActual(gastos: List<com.example.gastosapp.Models.Gasto>): List<com.example.gastosapp.Models.Gasto> {
        val calendar = Calendar.getInstance()
        val semanaActual = calendar.get(Calendar.WEEK_OF_YEAR)
        val añoActual = calendar.get(Calendar.YEAR)

        return gastos.filter { gasto ->
            gasto.fecha?.let { fecha ->
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaGasto = sdf.parse(fecha)
                    val calGasto = Calendar.getInstance()
                    calGasto.time = fechaGasto

                    calGasto.get(Calendar.WEEK_OF_YEAR) == semanaActual &&
                            calGasto.get(Calendar.YEAR) == añoActual
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
    }

    private fun obtenerGastosMesActual(gastos: List<com.example.gastosapp.Models.Gasto>): List<com.example.gastosapp.Models.Gasto> {
        val calendar = Calendar.getInstance()
        val mesActual = calendar.get(Calendar.MONTH)
        val añoActual = calendar.get(Calendar.YEAR)

        return gastos.filter { gasto ->
            gasto.fecha?.let { fecha ->
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaGasto = sdf.parse(fecha)
                    val calGasto = Calendar.getInstance()
                    calGasto.time = fechaGasto

                    calGasto.get(Calendar.MONTH) == mesActual &&
                            calGasto.get(Calendar.YEAR) == añoActual
                } catch (e: Exception) {
                    false
                }
            } ?: false
        }
    }

    private fun obtenerDiaDeLaSemana(fecha: String?): String {
        if (fecha.isNullOrEmpty()) return "Lun"

        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(fecha)
            val calendar = Calendar.getInstance()
            calendar.time = date

            val diaSemana = calendar.get(Calendar.DAY_OF_WEEK)
            when (diaSemana) {
                Calendar.MONDAY -> "Lun"
                Calendar.TUESDAY -> "Mar"
                Calendar.WEDNESDAY -> "Mié"
                Calendar.THURSDAY -> "Jue"
                Calendar.FRIDAY -> "Vie"
                Calendar.SATURDAY -> "Sáb"
                Calendar.SUNDAY -> "Dom"
                else -> "Lun"
            }
        } catch (e: Exception) {
            "Lun"
        }
    }

    private fun obtenerDiaDelMes(fecha: String?): String {
        if (fecha.isNullOrEmpty()) return "1"

        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(fecha)
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.get(Calendar.DAY_OF_MONTH).toString()
        } catch (e: Exception) {
            "1"
        }
    }
}