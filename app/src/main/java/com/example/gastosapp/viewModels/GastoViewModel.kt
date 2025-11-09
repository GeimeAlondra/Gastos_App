// com.example.gastosapp.viewModels.GastoViewModel.kt
package com.example.gastosapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gastosapp.Models.Gasto
import com.example.gastosapp.Models.Presupuesto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GastoViewModel : ViewModel() {

    private val _gastos = MutableLiveData<List<Gasto>>(emptyList())
    val gastos: LiveData<List<Gasto>> get() = _gastos

    private val auth = FirebaseAuth.getInstance()
    private var dbRef: DatabaseReference? = null
    private var gastosListener: ValueEventListener? = null

    init { mostrarGastos() }

    private fun mostrarGastos() {
        val uid = auth.currentUser?.uid ?: return
        dbRef = FirebaseDatabase.getInstance().reference.child("gastos").child(uid)

        gastosListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = snapshot.children.mapNotNull { child ->
                    child.getValue(Gasto::class.java)?.apply { id = child.key }
                }
                _gastos.value = lista
            }
            override fun onCancelled(error: DatabaseError) {
                println("ERROR Firebase (gastos): ${error.message}")
            }
        }
        dbRef?.addValueEventListener(gastosListener!!)
    }

    fun agregarGasto(gasto: Gasto, onResultado: (Boolean, String?) -> Unit) {
        try {
            val uid = auth.currentUser?.uid ?: return onResultado(false, "No autenticado")
            val nuevoId = dbRef?.push()?.key ?: return onResultado(false, "Error ID")

            val gastoConId = gasto.copy(id = nuevoId)
            val listaTemp = _gastos.value.orEmpty().toMutableList().apply { add(gastoConId) }
            _gastos.value = listaTemp

            println("REGISTRANDO GASTO: $gastoConId")

            FirebaseDatabase.getInstance()
                .reference.child("gastos").child(uid).child(nuevoId)
                .setValue(gastoConId)
                .addOnSuccessListener {
                    descontarPresupuesto(uid, gasto.categoriaId, gasto.monto) { exito, msg ->
                        if (exito) {
                            onResultado(true, "Gasto guardado")
                        } else {
                            revertirGasto(uid, nuevoId, listaTemp, gastoConId)
                            onResultado(false, msg)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    println("ERROR Firebase (setValue): ${e.message}")
                    revertirGasto(uid, nuevoId, listaTemp, gastoConId)
                    onResultado(false, "Error de red")
                }
        } catch (e: Exception) {
            println("CRASH en registrarNuevoGasto: ${e.message}")
            e.printStackTrace()
            onResultado(false, "Error interno")
        }
    }

    fun editarGasto(gastoNuevo: Gasto, posicion: Int, onResultado: (Boolean, String?) -> Unit) {
        try {
            val uid = auth.currentUser?.uid ?: return onResultado(false, "No autenticado")
            val id = gastoNuevo.id ?: return onResultado(false, "ID nulo")
            val lista = _gastos.value?.toMutableList() ?: return onResultado(false, "Lista vacía")
            if (posicion !in lista.indices) return onResultado(false, "Índice inválido")

            val gastoOriginal = lista[posicion]
            val diferenciaMonto = gastoNuevo.monto - gastoOriginal.monto
            val cambioCategoria = gastoNuevo.categoriaId != gastoOriginal.categoriaId

            lista[posicion] = gastoNuevo
            _gastos.value = lista

            FirebaseDatabase.getInstance()
                .reference.child("gastos").child(uid).child(id)
                .setValue(gastoNuevo)
                .addOnSuccessListener {
                    when {
                        cambioCategoria -> {
                            // Devolver al anterior
                            sumarPresupuesto(uid, gastoOriginal.categoriaId, gastoOriginal.monto) { _, _ ->
                                descontarPresupuesto(uid, gastoNuevo.categoriaId, gastoNuevo.monto) { exito, msg ->
                                    if (!exito) revertirActualizacion(lista, posicion, gastoOriginal)
                                    onResultado(exito, msg)
                                }
                            }
                        }
                        diferenciaMonto > 0 -> {
                            descontarPresupuesto(uid, gastoNuevo.categoriaId, diferenciaMonto) { exito, msg ->
                                if (!exito) revertirActualizacion(lista, posicion, gastoOriginal)
                                onResultado(exito, msg)
                            }
                        }
                        diferenciaMonto < 0 -> {
                            sumarPresupuesto(uid, gastoNuevo.categoriaId, -diferenciaMonto) { exito, _ ->
                                onResultado(exito, "Actualizado")
                            }
                        }
                        else -> onResultado(true, "Actualizado")
                    }
                }
                .addOnFailureListener { e ->
                    revertirActualizacion(lista, posicion, gastoOriginal)
                    onResultado(false, "Error: ${e.message}")
                }
        } catch (e: Exception) {
            println("CRASH en actualizarGasto: ${e.message}")
            e.printStackTrace()
            onResultado(false, "Error interno")
        }
    }

    fun eliminarGasto(posicion: Int, onResultado: (Boolean, String?) -> Unit) {
        try {
            val lista = _gastos.value?.toMutableList() ?: return onResultado(false, "Lista vacía")
            if (posicion !in lista.indices) return onResultado(false, "Índice inválido")

            val gasto = lista.removeAt(posicion)
            _gastos.value = lista

            val uid = auth.currentUser?.uid ?: return onResultado(false, "No autenticado")
            gasto.id?.let { id ->
                FirebaseDatabase.getInstance()
                    .reference.child("gastos").child(uid).child(id)
                    .removeValue()
                    .addOnSuccessListener {
                        sumarPresupuesto(uid, gasto.categoriaId, gasto.monto) { exito, msg ->
                            if (!exito) {
                                lista.add(posicion, gasto)
                                _gastos.value = lista
                            }
                            onResultado(exito, msg)
                        }
                    }
                    .addOnFailureListener {
                        lista.add(posicion, gasto)
                        _gastos.value = lista
                        onResultado(false, "Error al eliminar")
                    }
            }
        } catch (e: Exception) {
            println("CRASH en eliminarGasto: ${e.message}")
            e.printStackTrace()
            onResultado(false, "Error interno")
        }
    }

    // === AJUSTAR PRESUPUESTO ===
    private fun descontarPresupuesto(uid: String, categoriaId: Int, monto: Double, callback: (Boolean, String?) -> Unit) {
        ajustarPresupuesto(uid, categoriaId, -monto, callback)
    }

    private fun sumarPresupuesto(uid: String, categoriaId: Int, monto: Double, callback: (Boolean, String?) -> Unit) {
        ajustarPresupuesto(uid, categoriaId, monto, callback)
    }

    private fun ajustarPresupuesto(uid: String, categoriaId: Int, ajuste: Double, callback: (Boolean, String?) -> Unit) {
        try {
            val ref = FirebaseDatabase.getInstance().reference.child("presupuestos").child(uid)
            ref.orderByChild("categoriaId").equalTo(categoriaId.toDouble())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            callback(false, "No hay presupuesto para esta categoría")
                            return
                        }

                        var aplicado = false
                        var mensaje = "Operación exitosa"

                        for (child in snapshot.children) {
                            val p = child.getValue(Presupuesto::class.java) ?: continue
                            val nuevoSaldo = p.cantidad + ajuste

                            if (nuevoSaldo >= 0 || ajuste > 0) {
                                child.ref.child("cantidad").setValue(nuevoSaldo)
                                aplicado = true
                                mensaje = "Saldo: $${String.format("%.2f", nuevoSaldo)}"
                                break
                            } else {
                                mensaje = "Saldo insuficiente"
                            }
                        }
                        callback(aplicado, mensaje)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(false, "Error Firebase: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            println("CRASH en ajustarPresupuesto: ${e.message}")
            e.printStackTrace()
            callback(false, "Error interno")
        }
    }

    // === REVERTIR ===
    private fun revertirGasto(uid: String, id: String, lista: MutableList<Gasto>, gasto: Gasto) {
        FirebaseDatabase.getInstance().reference.child("gastos").child(uid).child(id).removeValue()
        lista.remove(gasto)
        _gastos.value = lista
    }

    private fun revertirActualizacion(lista: MutableList<Gasto>, pos: Int, original: Gasto) {
        lista[pos] = original
        _gastos.value = lista
    }

    fun obtenerPosicionPorId(id: String): Int = _gastos.value?.indexOfFirst { it.id == id } ?: -1

    override fun onCleared() {
        dbRef?.removeEventListener(gastosListener!!)
        super.onCleared()
    }
}