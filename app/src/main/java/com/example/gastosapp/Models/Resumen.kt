package com.example.gastosapp.Models

class Resumen {

    data class ResumenSemanal(
        val semana: String,
        val totalGastado: Double,
        val gastosPorCategoria: Map<String, Double>,
        val promedioDiario: Double
    )

    data class DatosGrafico(
        val etiquetas: List<String>,
        val valores: List<Double>,
        val colores: List<Int>
    )
}