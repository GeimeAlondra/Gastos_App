package com.example.gastosapp.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.gastosapp.Models.Presupuestos
import com.example.gastosapp.R
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class FragmentAgregarPresupuesto : DialogFragment() {

    private lateinit var etNombreGasto: TextInputEditText
    private lateinit var etCantidad: TextInputEditText
    private lateinit var btnFechaInicio: Button
    private lateinit var btnFechaFinal: Button
    private lateinit var tvFechaInicio: TextView
    private lateinit var tvFechaFinal: TextView
    private lateinit var rootView: View
    private lateinit var calendarInicio: Calendar
    private lateinit var calendarFinal: Calendar

    private var listener: PresupuestoGuardadoListener? = null

    fun setPresupuestoGuardadoListener(listener: PresupuestoGuardadoListener) {
        this.listener = listener
    }

    interface PresupuestoGuardadoListener {
        fun onPresupuestoGuardado(presupuesto: Presupuestos)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        rootView = inflater.inflate(R.layout.fragment_agregar_presupuesto, container, false)

        // Inicializar vistas
        initViews()

        // Configurar listeners
        setupListeners()

        return rootView
    }

    private fun initViews() {
        etNombreGasto = rootView.findViewById(R.id.etNombreGasto)
        etCantidad = rootView.findViewById(R.id.etCantidad)
        btnFechaInicio = rootView.findViewById(R.id.btnFechaInicio)
        btnFechaFinal = rootView.findViewById(R.id.btnFechaFinal)
        tvFechaInicio = rootView.findViewById(R.id.tvFechaInicio)
        tvFechaFinal = rootView.findViewById(R.id.tvFechaFinal)

        // Inicializar calendarios
        calendarInicio = Calendar.getInstance()
        calendarFinal = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
        }

        // Establecer texto inicial (si no está configurado en XML)
        tvFechaInicio.text = "No seleccionada"
        tvFechaFinal.text = "No seleccionada"
        btnFechaInicio.text = "Seleccionar"
        btnFechaFinal.text = "Seleccionar"
    }

    private fun setupListeners() {
        // Botón Fecha Inicio
        btnFechaInicio.setOnClickListener { showDatePicker(true) }

        // Botón Fecha Final
        btnFechaFinal.setOnClickListener { showDatePicker(false) }

        // Botón Guardar
        rootView.findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarPresupuesto() }

        // Botón Cancelar
        rootView.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dismiss() }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) calendarInicio else calendarFinal

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                updateDateDisplay(isStartDate, year, month, dayOfMonth)

                if (isStartDate && calendarFinal.before(calendarInicio)) {
                    calendarFinal.time = calendarInicio.time
                    updateDateDisplay(false,
                        calendarFinal.get(Calendar.YEAR),
                        calendarFinal.get(Calendar.MONTH),
                        calendarFinal.get(Calendar.DAY_OF_MONTH))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.apply {
                if (isStartDate) {
                    minDate = System.currentTimeMillis() - 1000
                } else {
                    minDate = calendarInicio.timeInMillis
                }
            }
            show()
        }
    }

    private fun updateDateDisplay(isStartDate: Boolean, year: Int, month: Int, day: Int) {
        val dateString = String.format("%02d/%02d/%d", day, month + 1, year)
        val (textView, button) = if (isStartDate) {
            tvFechaInicio to btnFechaInicio
        } else {
            tvFechaFinal to btnFechaFinal
        }
        textView.text = dateString
        button.text = "Cambiar"
    }

    private fun guardarPresupuesto() {
        println("DEBUG: guardarPresupuesto() INICIADO")

        if (!validarCampos()) {
            println("DEBUG: Validación falló")
            return
        }

        val nombre = etNombreGasto.text.toString().trim()
        val cantidad = etCantidad.text.toString().trim().toDoubleOrNull() ?: 0.0
        val fechaInicio = tvFechaInicio.text.toString()
        val fechaFinal = tvFechaFinal.text.toString()

        println("   DEBUG: Datos capturados:")
        println("   Nombre: $nombre")
        println("   Cantidad: $cantidad")
        println("   Fecha Inicio: $fechaInicio")
        println("   Fecha Final: $fechaFinal")

        val presupuesto = Presupuestos(nombre, cantidad, fechaInicio, fechaFinal)
        println("DEBUG: Objeto Presupuesto creado")

        // Verificar y llamar al listener
        listener?.onPresupuestoGuardado(presupuesto) ?: run {
            println("DEBUG: Listener ES null - NO se llamará onPresupuestoGuardado")
            Toast.makeText(requireContext(), "Error: Listener no configurado", Toast.LENGTH_LONG).show()
        }

        println("DEBUG: guardarPresupuesto() FINALIZADO")
        dismiss()
        Toast.makeText(requireContext(), "Presupuesto guardado!", Toast.LENGTH_SHORT).show()
    }

    private fun validarCampos(): Boolean {
        val nombre = etNombreGasto.text.toString().trim()
        val cantidadStr = etCantidad.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombreGasto.error = "Ingresa un nombre para el gasto"
            return false
        }

        if (cantidadStr.isEmpty()) {
            etCantidad.error = "Ingresa la cantidad"
            return false
        }

        if (tvFechaInicio.text.toString() == "No seleccionada") {
            Toast.makeText(requireContext(), "Selecciona la fecha de inicio", Toast.LENGTH_SHORT).show()
            return false
        }

        if (tvFechaFinal.text.toString() == "No seleccionada") {
            Toast.makeText(requireContext(), "Selecciona la fecha final", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}