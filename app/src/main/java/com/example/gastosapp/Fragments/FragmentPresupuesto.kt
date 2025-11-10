// com.example.gastosapp.Fragments.FragmentPresupuesto
package com.example.gastosapp.Fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gastosapp.Models.Categorias
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.example.gastosapp.viewModels.PresupuestoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FragmentPresupuesto : Fragment() {

    private lateinit var viewModel: PresupuestoViewModel
    private lateinit var container: LinearLayout
    private lateinit var btnAdd: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[PresupuestoViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_presupuesto, container, false)
        this.container = view.findViewById(R.id.containerPresupuestos)
        btnAdd = view.findViewById(R.id.agregarPresupuesto)

        viewModel.presupuestos.observe(viewLifecycleOwner) { actualizarUI(it) }

        btnAdd.setOnClickListener {
            btnAdd.playAnimation()
            Handler(Looper.getMainLooper()).postDelayed({ abrirAgregar() }, 500)
        }
        return view
    }

    /**
     * Obtiene las categorías de los presupuestos activos y abre el diálogo para agregar.
     */
    private fun abrirAgregar() {
        val dialog = FragmentAgregarPresupuesto().apply {
            // 1. Obtener y pasar las categorías ocupadas
            arguments = Bundle().apply {
                putStringArrayList("categorias_ocupadas", ArrayList(obtenerCategoriasOcupadas()))
            }

            // 2. Configurar el listener para guardar el nuevo presupuesto
            setPresupuestoGuardadoListener(object : FragmentAgregarPresupuesto.PresupuestoGuardadoListener {
                override fun onPresupuestoGuardado(p: Presupuesto) {
                    viewModel.agregarPresupuesto(p)
                }
            })
        }
        dialog.show(parentFragmentManager, "agregar")
    }

    /**
     * Abre el diálogo para editar un presupuesto existente, pasando las categorías ocupadas
     * (excluyendo la del presupuesto que se está editando).
     */
    private fun abrirEdicion(p: Presupuesto) {
        val dialog = FragmentAgregarPresupuesto().apply {
            // 1. Obtener y pasar las categorías ocupadas (excluyendo la actual)
            arguments = Bundle().apply {
                putStringArrayList("categorias_ocupadas", ArrayList(obtenerCategoriasOcupadas(p.id)))
            }

            // 2. Configurar el presupuesto a editar y el listener de edición
            setPresupuestoAEditar(p, object : FragmentAgregarPresupuesto.PresupuestoEditadoListener {
                override fun onPresupuestoEditado(editado: Presupuesto) {
                    val pos = viewModel.getPositionById(editado.id!!)
                    if (pos != -1) viewModel.editarPresupuesto(editado, pos)
                }
            })
        }
        dialog.show(parentFragmentManager, "editar")
    }

    /**
     * Revisa la lista actual de presupuestos y devuelve los nombres de las categorías
     * que pertenecen a presupuestos activos.
     * @param excluirIdPresupuesto Opcional. Si se provee, la categoría de este presupuesto será ignorada. Útil para el modo edición.
     * @return Una lista de nombres de categorías que ya están en uso.
     */
    private fun obtenerCategoriasOcupadas(excluirIdPresupuesto: String? = null): List<String> {
        val categoriasOcupadas = mutableListOf<String>()
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoyCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fechaHoy = hoyCalendar.time

        // Usamos la lista de presupuestos actual del ViewModel
        val listaActual = viewModel.presupuestos.value ?: emptyList()

        for (presupuesto in listaActual) {
            // Si estamos editando, saltamos el presupuesto actual en la comprobación
            if (presupuesto.id == excluirIdPresupuesto) {
                continue
            }

            val fechaFinalStr = presupuesto.fechaFinal
            if (fechaFinalStr != null) {
                try {
                    val fechaFinal = formatoFecha.parse(fechaFinalStr)
                    // Si la fecha final del presupuesto es hoy o en el futuro, está activo
                    if (fechaFinal != null && !fechaFinal.before(fechaHoy)) {
                        val nombreCategoria = Categorias.getNombrePorId(presupuesto.categoriaId)
                        if (nombreCategoria.isNotEmpty()) {
                            categoriasOcupadas.add(nombreCategoria)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // Error de formato de fecha
                }
            }
        }
        return categoriasOcupadas
    }

    private fun actualizarUI(lista: List<Presupuesto>) {
        container.removeAllViews()
        if (lista.isEmpty()) {
            container.addView(TextView(context).apply {
                text = "No hay presupuestos. ¡Agrega uno!"
                textSize = 16f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 100, 0, 100)
            })
            return
        }
        lista.forEachIndexed { i, p -> container.addView(crearItem(p, i)) }
    }

    // En FragmentPresupuesto.kt - MODIFICA el método crearItem
    private fun crearItem(p: Presupuesto, pos: Int): View {
        val v = LayoutInflater.from(context).inflate(R.layout.item_presupuesto, container, false)

        v.findViewById<TextView>(R.id.tvNombrePresupuesto).text = p.nombre
        v.findViewById<TextView>(R.id.tvCantidad).text = String.format("$%.2f", p.cantidad)
        v.findViewById<TextView>(R.id.tvFechaInicio).text = p.fechaInicio
        v.findViewById<TextView>(R.id.tvFechaFinal).text = p.fechaFinal
        v.findViewById<TextView>(R.id.tvCategoria).text = Categorias.getNombrePorId(p.categoriaId)

        // NUEVO: Mostrar el saldo disponible
        val saldoDisponible = p.cantidad - p.montoGastado
        v.findViewById<TextView>(R.id.tvSaldoDisponible).text =
            String.format("Saldo disponible: $%.2f", saldoDisponible)

        // Cambiar color según el saldo
        val tvSaldo = v.findViewById<TextView>(R.id.tvSaldoDisponible)
        if (saldoDisponible < 0) {
            tvSaldo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        } else if (saldoDisponible < p.cantidad * 0.2) {
            tvSaldo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
        } else {
            tvSaldo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        }

        v.findViewById<View>(R.id.btnEditar)?.setOnClickListener { abrirEdicion(p) }
        v.findViewById<View>(R.id.btnEliminar)?.setOnClickListener {
            viewModel.eliminarPresupuesto(pos)
            Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
        }
        return v
    }}
