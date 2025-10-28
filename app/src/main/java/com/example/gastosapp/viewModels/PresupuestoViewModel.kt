package com.example.gastosapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.gastosapp.Models.Presupuestos
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PresupuestoViewModel : ViewModel() {

    private val _listaPresupuestos = MutableLiveData<List<Presupuestos>>(emptyList())
    val presupuestos: LiveData<List<Presupuestos>> get() = _listaPresupuestos

    private val database = FirebaseDatabase.getInstance().reference.child("presupuestos")

    init {
        println("ViewModel creado")
        cargarPresupuestosDesdeFirebase()
    }

    private fun cargarPresupuestosDesdeFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presupuestosList = mutableListOf<Presupuestos>()
                for (childSnapshot in snapshot.children) {
                    val presupuesto = childSnapshot.getValue(Presupuestos::class.java)
                    presupuesto?.id = childSnapshot.key // Asegura que el ID se asigne
                    presupuesto?.let { presupuestosList.add(it) }
                }
                // Solo actualiza si la lista ha cambiado
                if (_listaPresupuestos.value != presupuestosList) {
                    _listaPresupuestos.value = presupuestosList
                    println("DEBUG: Lista de presupuestos actualizada desde Firebase: ${presupuestosList.size}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error al cargar presupuestos desde Firebase: ${error.message}")
            }
        })
    }

    fun agregarPresupuesto(presupuesto: Presupuestos) {
        val currentList = _listaPresupuestos.value?.toMutableList() ?: mutableListOf()
        val presupuestoId = database.push().key ?: return
        //presupuesto.id = presupuestoId
        currentList.add(presupuesto)
        _listaPresupuestos.value = currentList

        database.child(presupuestoId).setValue(presupuesto)
            .addOnSuccessListener {
                println("ViewModel: Presupuesto agregado - ${presupuesto.nombre}")
            }
            .addOnFailureListener {
                println("Error al guardar en Firebase: ${it.message}")
                // Opcional: Revertir la lista si falla
                currentList.remove(presupuesto)
                _listaPresupuestos.value = currentList
            }
    }

    fun eliminarPresupuesto(position: Int) {
        val currentList = _listaPresupuestos.value?.toMutableList() ?: return
        if (position in currentList.indices) {
            val presupuesto = currentList.removeAt(position)
            _listaPresupuestos.value = currentList

            presupuesto.id?.let { id ->
                database.child(id).removeValue()
                    .addOnSuccessListener {
                        println("ViewModel: Presupuesto eliminado - ${presupuesto.nombre}")
                        println("   Total presupuestos ahora: ${currentList.size}")
                    }
                    .addOnFailureListener {
                        println("Error al eliminar de Firebase: ${it.message}")
                        // Opcional: Revertir la eliminación si falla
                        currentList.add(position, presupuesto)
                        _listaPresupuestos.value = currentList
                    }
            } ?: println("ViewModel: No se pudo eliminar - ID no encontrado")
        } else {
            println("ViewModel: No se pudo eliminar - posición inválida: $position")
        }
    }

    fun getCantidadPresupuestos(): Int = _listaPresupuestos.value?.size ?: 0

    override fun onCleared() {
        super.onCleared()
        println("ViewModel destruido")
    }
}