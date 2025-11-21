package com.example.gastosapp.Models

enum class Categoria(val id: Int, val nombre: String) {
    ALIMENTACION(1, "Alimentación"),
    TRANSPORTE(2, "Transporte"),
    ENTRETENIMIENTO(3, "Entretenimiento"),
    SALUD(4, "Salud"),
    EDUCACION(5, "Educación"),
    COMPRAS(6, "Compras"),
    HOGAR(7, "Hogar"),
    OTROS(8, "Otros");

    companion object {
        fun fromId(id: Int): Categoria = values().find { it.id == id } ?: OTROS
        fun fromNombre(nombre: String): Categoria = values().find { it.nombre == nombre } ?: OTROS
    }
}