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

            println("=== REGISTRANDO GASTO ===")
            println("Categoría ID: ${gasto.categoriaId}")
            println("Monto: ${gasto.monto}")
            println("Usuario UID: $uid")

            FirebaseDatabase.getInstance()
                .reference.child("gastos").child(uid).child(nuevoId)
                .setValue(gastoConId)
                .addOnSuccessListener {
                    println("  Gasto guardado en Firebase, ahora actualizando presupuesto...")
                    descontarPresupuesto(uid, gasto.categoriaId, gasto.monto) { exito, msg ->
                        println("  Resultado actualización presupuesto: $exito - $msg")
                        if (exito) {
                            onResultado(true, "Gasto guardado")
                        } else {
                            println("  Error en presupuesto, revirtiendo gasto...")
                            revertirGasto(uid, nuevoId, listaTemp, gastoConId)
                            onResultado(false, msg)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    println("  ERROR Firebase (setValue): ${e.message}")
                    revertirGasto(uid, nuevoId, listaTemp, gastoConId)
                    onResultado(false, "Error de red: ${e.message}")
                }
        } catch (e: Exception) {
            println("  CRASH en registrarNuevoGasto: ${e.message}")
            e.printStackTrace()
            onResultado(false, "Error interno: ${e.message}")
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
    // En GastoViewModel.kt - VERIFICA que estos métodos estén así
    private fun descontarPresupuesto(uid: String, categoriaId: Int, monto: Double, callback: (Boolean, String?) -> Unit) {
        println("  DESCONTANDO del presupuesto: $monto")
        ajustarPresupuesto(uid, categoriaId, monto, callback)
    }

    private fun sumarPresupuesto(uid: String, categoriaId: Int, monto: Double, callback: (Boolean, String?) -> Unit) {
        println("  SUMANDO al presupuesto: $monto")
        ajustarPresupuesto(uid, categoriaId, -monto, callback)
    }

    // En GastoViewModel.kt

    private fun ajustarPresupuesto(uid: String, categoriaId: Int, ajuste: Double, callback: (Boolean, String?) -> Unit) {
        try {
            println("🔧 AJUSTANDO PRESUPUESTO - Categoría: $categoriaId, Ajuste: $ajuste")

            val ref = FirebaseDatabase.getInstance().reference.child("presupuestos").child(uid)
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    println("  Presupuestos encontrados: ${snapshot.childrenCount}")

                    var presupuestoEncontrado: DataSnapshot? = null
                    for (child in snapshot.children) {
                        val presupuesto = child.getValue(Presupuesto::class.java)
                        println("  Revisando presupuesto: ${presupuesto?.categoriaId} vs $categoriaId")
                        if (presupuesto?.categoriaId == categoriaId) {
                            presupuestoEncontrado = child
                            println("  Presupuesto encontrado: ${child.key}")
                            break
                        }
                    }

                    if (presupuestoEncontrado == null) {
                        println("  No hay presupuesto para la categoría $categoriaId")
                        callback(false, "No hay presupuesto para esta categoría")
                        return
                    }

                    val presupuesto = presupuestoEncontrado.getValue(Presupuesto::class.java)

                    // CORRECCIÓN: Para DESCONTAR gastos, el ajuste debe ser POSITIVO (sumar al montoGastado)
                    val nuevoMontoGastado = (presupuesto?.montoGastado ?: 0.0) + ajuste

                    println("  Monto actual: ${presupuesto?.montoGastado}, Nuevo: $nuevoMontoGastado")
                    println("  Presupuesto total: ${presupuesto?.cantidad}")
                    println("  Saldo disponible después: ${(presupuesto?.cantidad ?: 0.0) - nuevoMontoGastado}")

                    // Validar que no exceda el presupuesto
                    if (nuevoMontoGastado > (presupuesto?.cantidad ?: 0.0)) {
                        println("  Saldo insuficiente")
                        callback(false, "Saldo insuficiente en el presupuesto")
                        return
                    }

                    presupuestoEncontrado.ref.child("montoGastado").setValue(nuevoMontoGastado)
                        .addOnSuccessListener {
                            println("  Presupuesto actualizado exitosamente")
                            val saldoRestante = (presupuesto?.cantidad ?: 0.0) - nuevoMontoGastado
                            callback(true, "Presupuesto actualizado. Saldo restante: $${String.format("%.2f", saldoRestante)}")
                        }
                        .addOnFailureListener { e ->
                            println("  Error al actualizar presupuesto: ${e.message}")
                            callback(false, "Error al actualizar el presupuesto")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    println("  Error Firebase: ${error.message}")
                    callback(false, "Error Firebase: ${error.message}")
                }
            })
        } catch (e: Exception) {
            println("  CRASH en ajustarPresupuesto: ${e.message}")
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