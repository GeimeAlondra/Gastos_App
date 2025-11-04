package com.example.gastosapp.Models

data class Presupuesto(
    var id: String? = null,
    var nombre: String? = null,
    var cantidad: Double = 0.0,
    var fechaInicio: String? = null,
    var fechaFinal: String? = null,
    var timestamp: Long = 0L
) {
    // Constructor vacío para Firebase
    constructor() : this(null, null, 0.0, null, null, 0L)

    // Constructor con parámetros
    constructor(nombre: String, cantidad: Double, fechaInicio: String, fechaFinal: String) : this(
        id = null,
        nombre = nombre,
        cantidad = cantidad,
        fechaInicio = fechaInicio,
        fechaFinal = fechaFinal,
        timestamp = System.currentTimeMillis()
    )
}