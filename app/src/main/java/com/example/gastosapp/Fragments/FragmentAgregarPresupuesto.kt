package com.example.gastosapp.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.gastosapp.Models.Categoria
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentAgregarPresupuestoBinding
import java.text.SimpleDateFormat
import java.util.*

class FragmentAgregarPresupuesto : DialogFragment() {

    private var _binding: FragmentAgregarPresupuestoBinding? = null
    private val binding get() = _binding!!

    private val calendarInicio = Calendar.getInstance()
    private val calendarFinal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

    private var onPresupuestoSaved: ((Presupuesto) -> Unit)? = null
    private var presupuestoAEditar: Presupuesto? = null
    private var categoriasOcupadas = listOf<String>()

    fun setOnPresupuestoSaved(listener: (Presupuesto) -> Unit) {
        onPresupuestoSaved = listener
    }

    fun editarPresupuesto(presupuesto: Presupuesto, listener: (Presupuesto) -> Unit) {
        presupuestoAEditar = presupuesto
        onPresupuestoSaved = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getStringArrayList("categorias_ocupadas")?.let {
            categoriasOcupadas = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAgregarPresupuestoBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.3f)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarFechasPorDefecto()
        configurarCategoria()
        configurarDatePickers()
        configurarBotones()

        presupuestoAEditar?.let { precargarDatos(it) }
    }

    private fun inicializarFechasPorDefecto() {
        binding.etFechaInicio.text = formatearFecha(calendarInicio)
        binding.etFechaFinal.text = formatearFecha(calendarFinal)
        binding.btnFechaInicio.text = "Cambiar"
        binding.btnFechaFinal.text = "Cambiar"
    }

    private fun configurarCategoria() {
        val todosLosNombres = Categoria.values().map { it.nombre }
        val categoriaActual = presupuestoAEditar?.categoria?.nombre

        val nombresDisponibles = todosLosNombres.filter {
            it !in categoriasOcupadas || it == categoriaActual
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresDisponibles)
        binding.etCategoria.setAdapter(adapter)

        if (presupuestoAEditar == null && nombresDisponibles.isNotEmpty()) {
            val defecto = if ("Otros" in nombresDisponibles) "Otros" else nombresDisponibles[0]
            binding.etCategoria.setText(defecto, false)
        } else if (nombresDisponibles.isEmpty() && presupuestoAEditar == null) {
            binding.etCategoria.setText("No hay categorías disponibles", false)
            binding.etCategoria.isEnabled = false
        }
    }

    private fun configurarDatePickers() {
        binding.btnFechaInicio.setOnClickListener { mostrarDatePicker(true) }
        binding.btnFechaFinal.setOnClickListener { mostrarDatePicker(false) }
    }

    private fun mostrarDatePicker(esInicio: Boolean) {
        val calendar = if (esInicio) calendarInicio else calendarFinal
        DatePickerDialog(
            requireContext(),
            { _, año, mes, dia ->
                calendar.set(año, mes, dia)
                val fechaFormateada = formatearFecha(calendar)
                if (esInicio) {
                    binding.etFechaInicio.text = fechaFormateada
                    binding.btnFechaInicio.text = "Cambiar"
                    if (calendarFinal.before(calendarInicio)) {
                        calendarFinal.time = calendarInicio.time
                        binding.etFechaFinal.text = fechaFormateada
                        binding.btnFechaFinal.text = "Cambiar"
                    }
                } else {
                    binding.etFechaFinal.text = fechaFormateada
                    binding.btnFechaFinal.text = "Cambiar"
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (!esInicio) datePicker.minDate = calendarInicio.timeInMillis
            show()
        }
    }

    private fun configurarBotones() {
        binding.btnGuardar.setOnClickListener { guardarPresupuesto() }
        binding.btnCancelar.setOnClickListener { dismiss() }
    }

    private fun precargarDatos(p: Presupuesto) {
        binding.etCantidad.setText(p.cantidad.toString())
        binding.etCategoria.setText(p.categoria.nombre, false)
        // Fechas ya vienen como Date, no String
        binding.etFechaInicio.text = formatearFecha(p.fechaInicio)
        binding.etFechaFinal.text = formatearFecha(p.fechaFinal)
        binding.btnFechaInicio.text = "Cambiar"
        binding.btnFechaFinal.text = "Cambiar"
    }

    private fun guardarPresupuesto() {
        if (!validarCampos()) return

        val nombreCategoria = binding.etCategoria.text.toString().trim()
        val cantidad = binding.etCantidad.text.toString().toDouble()
        val categoria = Categoria.fromNombre(nombreCategoria)

        val presupuesto = presupuestoAEditar?.copy(
            categoria = categoria,
            cantidad = cantidad,
            montoGastado = presupuestoAEditar?.montoGastado ?: 0.0
        ) ?: Presupuesto(
            categoria = categoria,
            cantidad = cantidad
        )

        onPresupuestoSaved?.invoke(presupuesto)
        dismiss()
        Toast.makeText(requireContext(), "Presupuesto guardado", Toast.LENGTH_SHORT).show()
    }

    private fun validarCampos(): Boolean {
        var valido = true

        if (binding.etCantidad.text.isNullOrBlank()) {
            binding.etCantidad.error = "Cantidad requerida"
            valido = false
        }
        val cat = binding.etCategoria.text.toString().trim()
        if (cat.isEmpty() || Categoria.fromNombre(cat) == Categoria.OTROS && cat != "Otros") {
            binding.etCategoria.error = "Categoría inválida"
            valido = false
        }

        return valido
    }

    private fun formatearFecha(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

    private fun formatearFecha(cal: Calendar): String = formatearFecha(cal.time)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}