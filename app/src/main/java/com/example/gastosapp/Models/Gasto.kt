package com.example.gastosapp.Models

data class Gasto(
    var id: String? = null,
    var nombre: String? = null,
    var descripcion: String? = null,
    var monto: Double = 0.0,
    var categoriaId: Int = 9,
    var fecha: String? = null,
    var timestamp: Long = 0L
) {
    constructor() : this(null, null, null, 0.0, 9, null, 0L)

    constructor(
        nombre: String?,
        descripcion: String?,
        monto: Double,
        categoriaId: Int,
        fecha: String?
    ) : this(null, nombre, descripcion, monto, categoriaId, fecha, System.currentTimeMillis())
}