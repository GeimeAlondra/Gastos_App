package com.example.gastosapp.Models

// Archivo: Categorias.kt
object Categorias {
    val lista = listOf(
        Categoria(1, "Alimentación"),
        Categoria(2, "Transporte"),
        Categoria(3, "Comida"),
        Categoria(4, "Entretenimiento"),
        Categoria(5, "Salud"),
        Categoria(6, "Educación"),
        Categoria(7, "Compras"),
        Categoria(8, "Hogar"),
        Categoria(9, "Otros")
    )

    fun getNombrePorId(id: Int): String {
        return lista.find { it.idCategoria == id }?.nombre ?: "Desconocida"
    }

    fun getIdPorNombre(nombre: String): Int {
        return lista.find { it.nombre == nombre }?.idCategoria ?: 9
    }
}
