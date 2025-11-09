package com.example.gastosapp.Models

data class Presupuesto(
    var id: String? = null,
    var nombre: String? = null,
    var cantidad: Double = 0.0,
    var fechaInicio: String? = null,
    var fechaFinal: String? = null,
    var categoriaId: Int = 9, // 9 = "Otros" por defecto
    var timestamp: Long = 0L
) {
    constructor() : this(null, null, 0.0, null, null, 9, 0L)

    constructor(
        nombre: String,
        cantidad: Double,
        fechaInicio: String,
        fechaFinal: String,
        categoriaId: Int
    ) : this(
        id = null,
        nombre = nombre,
        cantidad = cantidad,
        fechaInicio = fechaInicio,
        fechaFinal = fechaFinal,
        categoriaId = categoriaId,
        timestamp = System.currentTimeMillis()
    )
}