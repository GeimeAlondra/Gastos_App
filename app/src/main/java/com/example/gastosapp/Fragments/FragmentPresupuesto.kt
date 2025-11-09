// com.example.gastosapp.Fragments.FragmentPresupuesto
package com.example.gastosapp.Fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.gastosapp.Models.Categorias
import com.example.gastosapp.Models.Presupuesto
import com.example.gastosapp.R
import com.example.gastosapp.viewModels.PresupuestoViewModel

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

    private fun abrirAgregar() {
        val dialog = FragmentAgregarPresupuesto().apply {
            setPresupuestoGuardadoListener(object : FragmentAgregarPresupuesto.PresupuestoGuardadoListener {
                override fun onPresupuestoGuardado(p: Presupuesto) {
                    viewModel.agregarPresupuesto(p)
                }
            })
        }
        dialog.show(parentFragmentManager, "agregar")
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

    private fun crearItem(p: Presupuesto, pos: Int): View {
        val v = LayoutInflater.from(context).inflate(R.layout.item_presupuesto, container, false)

        v.findViewById<TextView>(R.id.tvNombrePresupuesto).text = p.nombre
        v.findViewById<TextView>(R.id.tvCantidad).text = String.format("$%.2f", p.cantidad)
        v.findViewById<TextView>(R.id.tvFechaInicio).text = p.fechaInicio
        v.findViewById<TextView>(R.id.tvFechaFinal).text = p.fechaFinal
        v.findViewById<TextView>(R.id.tvCategoria).text = Categorias.getNombrePorId(p.categoriaId)

        v.findViewById<View>(R.id.btnEditar)?.setOnClickListener { abrirEdicion(p) }
        v.findViewById<View>(R.id.btnEliminar)?.setOnClickListener {
            viewModel.eliminarPresupuesto(pos)
            Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
        }
        return v
    }

    private fun abrirEdicion(p: Presupuesto) {
        val dialog = FragmentAgregarPresupuesto().apply {
            setPresupuestoAEditar(p, object : FragmentAgregarPresupuesto.PresupuestoEditadoListener {
                override fun onPresupuestoEditado(editado: Presupuesto) {
                    val pos = viewModel.getPositionById(editado.id!!)
                    if (pos != -1) viewModel.editarPresupuesto(editado, pos)
                }
            })
        }
        dialog.show(parentFragmentManager, "editar")
    }
}