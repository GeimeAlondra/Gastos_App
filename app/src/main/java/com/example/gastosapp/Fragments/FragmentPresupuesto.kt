package com.example.gastosapp.Fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gastosapp.Models.Presupuestos
import com.example.gastosapp.R
import com.example.gastosapp.viewModels.PresupuestoViewModel
import com.google.firebase.database.FirebaseDatabase

class FragmentPresupuesto : Fragment() {

    private val ARG_PARAM1 = "param1"
    private val ARG_PARAM2 = "param2"
    private var mParam1: String? = null
    private var mParam2: String? = null

    private lateinit var viewModel: PresupuestoViewModel
    private lateinit var containerPresupuestos: LinearLayout
    private val database = FirebaseDatabase.getInstance().reference.child("presupuestos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mParam1 = it.getString(ARG_PARAM1)
            mParam2 = it.getString(ARG_PARAM2)
        }

        viewModel = ViewModelProvider(this)[PresupuestoViewModel::class.java]
        println("ViewModel inicializado")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presupuesto, container, false)

        // Inicializar vistas
        containerPresupuestos = view.findViewById(R.id.containerPresupuestos)
        val btnAddCategory: LottieAnimationView = view.findViewById(R.id.agregarPresupuesto)

        println("Vistas inicializadas")

        // Configurar Observer para los presupuestos
        viewModel.presupuestos.observe(viewLifecycleOwner) { presupuestos ->
            println("Observer ejecutado - ${presupuestos.size} presupuestos")
            actualizarVistaPresupuestos(presupuestos)
        }

        // Configurar click listener del botón
        btnAddCategory.setOnClickListener {
            println("Botón presionado")
            btnAddCategory.playAnimation()

            Handler(Looper.getMainLooper()).postDelayed({
                showFloatingWindow()
            }, 500)
        }

        return view
    }

    private fun actualizarVistaPresupuestos(presupuestos: List<Presupuestos>) {
        println("Actualizando vista con ${presupuestos.size} presupuestos")

        // Limpiar el contenedor
        containerPresupuestos.removeAllViews()

        if (presupuestos.isEmpty()) {
            // Mostrar estado vacío
            val tvEmpty = TextView(requireContext()).apply {
                text = "No hay presupuestos. ¡Agrega uno nuevo!"
                textSize = 16f
                gravity = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 50, 0, 50)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }
            containerPresupuestos.addView(tvEmpty)
            println("Mostrando estado vacío")
        } else {
            // Agregar cada presupuesto a la vista
            presupuestos.forEachIndexed { index, presupuesto ->
                val itemView = crearItemPresupuesto(presupuesto, index)
                containerPresupuestos.addView(itemView)
            }
            println("${presupuestos.size} presupuestos mostrados")
        }
    }

    private fun showFloatingWindow() {
        println("Mostrando diálogo de agregar presupuesto")

        try {
            val dialogFragment = FragmentAgregarPresupuesto()

            // Configurar el listener para cuando se guarde un presupuesto
            dialogFragment.setPresupuestoGuardadoListener(object : FragmentAgregarPresupuesto.PresupuestoGuardadoListener {
                override fun onPresupuestoGuardado(presupuesto: Presupuestos) {
                    println("Presupuesto guardado recibido: ${presupuesto.nombre}")

                    // Guardar en Firebase
                    val presupuestoId = database.push().key ?: return
                    presupuesto.id = presupuestoId
                    database.child(presupuestoId).setValue(presupuesto)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "¡Presupuesto agregado!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error al guardar en Firebase", Toast.LENGTH_SHORT).show()
                        }
                }
            })

            // Mostrar el diálogo
            dialogFragment.show(parentFragmentManager, "presupuesto_dialog")

        } catch (e: Exception) {
            println("Error al mostrar diálogo: ${e.message}")
            Toast.makeText(requireContext(), "Error al abrir formulario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearItemPresupuesto(presupuesto: Presupuestos, position: Int): View {
        println("Creando item para: ${presupuesto.nombre}")

        val inflater = LayoutInflater.from(requireContext())
        val itemView = inflater.inflate(R.layout.item_presupuesto, containerPresupuestos, false)

        try {
            // Configurar las vistas del item
            itemView.findViewById<TextView>(R.id.tvNombrePresupuesto).text = presupuesto.nombre
            itemView.findViewById<TextView>(R.id.tvCantidad).text = String.format("$%.2f", presupuesto.cantidad)
            itemView.findViewById<TextView>(R.id.tvFechaInicio).text = presupuesto.fechaInicio
            itemView.findViewById<TextView>(R.id.tvFechaFinal).text = presupuesto.fechaFinal
            itemView.findViewById<TextView>(R.id.tvEstado).text = "Activo"

            // Configurar botón de eliminar
            itemView.findViewById<View>(R.id.btnEliminar)?.setOnClickListener {
                eliminarPresupuesto(position)
            }

        } catch (e: Exception) {
            println("Error al configurar item: ${e.message}")
        }

        return itemView
    }

    private fun eliminarPresupuesto(position: Int) {
        println("🗑 Eliminando presupuesto en posición: $position")

        viewModel.eliminarPresupuesto(position)
        Toast.makeText(requireContext(), "Presupuesto eliminado", Toast.LENGTH_SHORT).show()

        // Opcional: Eliminar de Firebase si ya está guardado
        viewModel.presupuestos.value?.getOrNull(position)?.id?.let { id ->
            database.child(id).removeValue()
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al eliminar de Firebase", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        println("FragmentPresupuesto: onResume")
    }

    override fun onPause() {
        super.onPause()
        println("FragmentPresupuesto: onPause")
    }
}