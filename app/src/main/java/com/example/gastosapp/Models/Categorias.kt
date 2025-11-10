package com.example.gastosapp.Models

// Archivo: Categorias.kt
object Categorias {
    val lista = listOf(
        Categoria(1, "Alimentación"),
        Categoria(2, "Transporte"),
        Categoria(3, "Entretenimiento"),
        Categoria(4, "Salud"),
        Categoria(5, "Educación"),
        Categoria(6, "Compras"),
        Categoria(7, "Hogar"),
        Categoria(8, "Otros")
    )

    fun getNombrePorId(id: Int): String {
        return lista.find { it.idCategoria == id }?.nombre ?: "Desconocida"
    }

    fun getIdPorNombre(nombre: String): Int {
        return lista.find { it.nombre == nombre }?.idCategoria ?: 9
    }
}
