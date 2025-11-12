package com.example.gastosapp.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.gastosapp.Models.Categorias
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.google.android.material.textfield.TextInputEditText
import java.util.*

class FragmentAgregarPresupuesto : DialogFragment() {

    private lateinit var etCantidad: TextInputEditText
    private lateinit var etCategoria: AutoCompleteTextView
    private lateinit var btnFechaInicio: Button
    private lateinit var btnFechaFinal: Button
    private lateinit var etFechaInicio: TextView
    private lateinit var etFechaFinal: TextView
    private lateinit var rootView: View

    private var calendarInicio = Calendar.getInstance()
    private var calendarFinal = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

    private var listenerCrear: PresupuestoGuardadoListener? = null
    private var listenerEditar: PresupuestoEditadoListener? = null
    private var presupuestoAEditar: Presupuesto? = null

    // CAMBIO 1: Variable para guardar las categorías no disponibles
    private var categoriasOcupadas = listOf<String>()

    fun setPresupuestoGuardadoListener(listener: PresupuestoGuardadoListener) {
        this.listenerCrear = listener
    }

    fun setPresupuestoAEditar(presupuesto: Presupuesto, listener: PresupuestoEditadoListener) {
        this.presupuestoAEditar = presupuesto
        this.listenerEditar = listener
    }

    interface PresupuestoGuardadoListener { fun onPresupuestoGuardado(p: Presupuesto) }
    interface PresupuestoEditadoListener { fun onPresupuestoEditado(p: Presupuesto) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CAMBIO 2: Recibir la lista de categorías ocupadas desde los arguments
        arguments?.let {
            categoriasOcupadas = it.getStringArrayList("categorias_ocupadas") ?: listOf()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_agregar_presupuesto, container, false)

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setDimAmount(0.3f)

        initViews()
        // Esta función ahora filtrará las categorías
        seleccionarCategoria()
        setupListeners()

        presupuestoAEditar?.let { precargarDatos(it) }
        return rootView
    }

    private fun initViews() {
        // ... (esta función no tiene cambios)
        etCantidad = rootView.findViewById(R.id.etCantidad)
        etCategoria = rootView.findViewById(R.id.etCategoria)
        btnFechaInicio = rootView.findViewById(R.id.btnFechaInicio)
        btnFechaFinal = rootView.findViewById(R.id.btnFechaFinal)
        etFechaInicio = rootView.findViewById(R.id.etFechaInicio)
        etFechaFinal = rootView.findViewById(R.id.etFechaFinal)
        etFechaInicio.text = "No seleccionada"
        etFechaFinal.text = "No seleccionada"
        btnFechaInicio.text = "Seleccionar"
        btnFechaFinal.text = "Seleccionar"
    }

    // CAMBIO 3: Modificar la función para que filtre las categorías
    private fun seleccionarCategoria() {
        // 1. Obtenemos la lista completa de nombres de categorías
        val todosLosNombres = Categorias.lista.map { it.nombre }

        // 2. Si estamos editando, permitimos que la categoría actual del presupuesto aparezca en la lista
        val categoriaActualEnEdicion = presupuestoAEditar?.let { Categorias.getNombrePorId(it.categoriaId) }

        // 3. Filtramos la lista:
        //    - Mantenemos una categoría si NO está en la lista de 'ocupadas'
        //    - O si es la categoría que estamos editando actualmente
        val nombresDisponibles = todosLosNombres.filter {
            it !in categoriasOcupadas || it == categoriaActualEnEdicion
        }

        // 4. Creamos el adaptador solo con las categorías disponibles
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresDisponibles)
        etCategoria.setAdapter(adapter)

        // 5. Establecemos un valor por defecto solo si estamos creando y hay opciones disponibles
        if (presupuestoAEditar == null && nombresDisponibles.isNotEmpty()) {
            // Intentamos poner "Otros" por defecto si está disponible, si no, la primera de la lista
            val categoriaDefecto = if ("Otros" in nombresDisponibles) "Otros" else nombresDisponibles[0]
            etCategoria.setText(categoriaDefecto, false)
        } else if (presupuestoAEditar == null && nombresDisponibles.isEmpty()) {
            // Si no hay categorías disponibles, lo indicamos
            etCategoria.setText("No hay categorías disponibles", false)
            etCategoria.isEnabled = false // Deshabilitamos el campo
        }
    }

    // ... (el resto de tus funciones: setupListeners, showDatePicker, updateDateDisplay, etc. no necesitan cambios)
    private fun setupListeners() {
        btnFechaInicio.setOnClickListener { showDatePicker(true) }
        btnFechaFinal.setOnClickListener { showDatePicker(false) }
        rootView.findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarPresupuesto() }
        rootView.findViewById<Button>(R.id.btnCancelar).setOnClickListener { dismiss() }
    }

    private fun showDatePicker(isInicio: Boolean) {
        val cal = if (isInicio) calendarInicio else calendarFinal
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            updateDateDisplay(isInicio, y, m, d)
            if (isInicio && calendarFinal.before(calendarInicio)) {
                calendarFinal.time = calendarInicio.time
                updateDateDisplay(false, y, m, d)
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            if (!isInicio) datePicker.minDate = calendarInicio.timeInMillis
            show()
        }
    }

    private fun updateDateDisplay(isInicio: Boolean, y: Int, m: Int, d: Int) {
        val fecha = String.format("%02d/%02d/%d", d, m + 1, y)
        if (isInicio) {
            etFechaInicio.text = fecha
            btnFechaInicio.text = "Cambiar"
        } else {
            etFechaFinal.text = fecha
            btnFechaFinal.text = "Cambiar"
        }
    }

    private fun precargarDatos(p: Presupuesto) {
        etCantidad.setText(p.cantidad.toString())
        p.fechaInicio?.let { parseAndSetDate(it, true) }
        p.fechaFinal?.let { parseAndSetDate(it, false) }
        etCategoria.setText(Categorias.getNombrePorId(p.categoriaId), false)
    }

    private fun parseAndSetDate(fecha: String, isInicio: Boolean) {
        val partes = fecha.split("/")
        if (partes.size == 3) {
            val cal = if (isInicio) calendarInicio else calendarFinal
            cal.set(partes[2].toInt(), partes[1].toInt() - 1, partes[0].toInt())
            updateDateDisplay(isInicio, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
    }

    private fun guardarPresupuesto() {
        if (!validarCampos()) return

        val nombre = etCategoria.text.toString().trim()
        val cantidad = etCantidad.text.toString().toDouble()
        val fInicio = etFechaInicio.text.toString()
        val fFinal = etFechaFinal.text.toString()
        val catId = Categorias.getIdPorNombre(etCategoria.text.toString())

        if (presupuestoAEditar != null) {
            val editado = presupuestoAEditar!!.copy(
                nombre = nombre, cantidad = cantidad,
                fechaInicio = fInicio, fechaFinal = fFinal, categoriaId = catId
            )
            listenerEditar?.onPresupuestoEditado(editado)
        } else {
            val nuevo = Presupuesto(nombre, cantidad, fInicio, fFinal, catId)
            listenerCrear?.onPresupuestoGuardado(nuevo)
        }
        dismiss()
        Toast.makeText(requireContext(), "Guardado", Toast.LENGTH_SHORT).show()
    }

    private fun validarCampos(): Boolean {
        val categoriaSeleccionada = etCategoria.text.toString().trim()

        if (etCantidad.text.isNullOrBlank()) {
            etCantidad.error = "Cantidad requerida"
            return false
        }
        if (etFechaInicio.text == "No seleccionada") {
            Toast.makeText(context, "Selecciona una fecha de inicio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etFechaFinal.text == "No seleccionada") {
            Toast.makeText(context, "Selecciona una fecha final", Toast.LENGTH_SHORT).show()
            return false
        }
        // Validamos que la categoría sea una de las válidas y no el mensaje de "no disponibles"
        if (categoriaSeleccionada.isEmpty() || categoriaSeleccionada !in Categorias.lista.map { it.nombre }) {
            etCategoria.error = "Selecciona una categoría válida"
            return false
        }
        return true
    }
}
