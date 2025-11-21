package com.example.gastosapp.Models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Presupuesto(
    val id: String = "",
    val categoria: Categoria = Categoria.OTROS,
    val cantidad: Double = 0.0,
    val fechaInicio: Date = Date(),
    val fechaFinal: Date = Date(),
    val montoGastado: Double = 0.0,
    @ServerTimestamp
    val creadoEn: Date? = null
)