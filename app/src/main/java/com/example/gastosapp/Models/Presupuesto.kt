package com.example.gastosapp.Models;

public class Presupuesto {
    private String id;
    private String nombre;
    private double cantidad;
    private String fechaInicio;
    private String fechaFinal;
    private long timestamp;

    public Presupuesto() {
        // Constructor vacío para Firebase
    }

    public Presupuesto(String nombre, double cantidad, String fechaInicio, String fechaFinal) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.fechaInicio = fechaInicio;
        this.fechaFinal = fechaFinal;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFinal() { return fechaFinal; }
    public void setFechaFinal(String fechaFinal) { this.fechaFinal = fechaFinal; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
