package com.example.gastosapp.viewModels;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gastosapp.Models.Presupuesto;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PresupuestoViewModel extends ViewModel {

    private MutableLiveData<List<Presupuesto>> listaPresupuestos = new MutableLiveData<>();
    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private boolean initialized = false;

    /**
     * Método para inicializar con el Context (LLAMAR ESTO EN EL FRAGMENT)
     */
    public void init(Context context) {
        if (!initialized) {
            prefs = context.getSharedPreferences("presupuestos_app", Context.MODE_PRIVATE);
            cargarPresupuestosGuardados();
            initialized = true;
            System.out.println("✅ ViewModel inicializado con persistencia");
        }
    }

    /**
     * Cargar presupuestos guardados desde SharedPreferences
     */
    private void cargarPresupuestosGuardados() {
        String presupuestosJson = prefs.getString("lista_presupuestos", "[]");
        System.out.println("📂 Cargando datos: " + presupuestosJson);

        try {
            Type listType = new TypeToken<List<Presupuesto>>(){}.getType();
            List<Presupuesto> presupuestosGuardados = gson.fromJson(presupuestosJson, listType);

            if (presupuestosGuardados != null) {
                listaPresupuestos.setValue(presupuestosGuardados);
                System.out.println("✅ " + presupuestosGuardados.size() + " presupuestos cargados");
            } else {
                listaPresupuestos.setValue(new ArrayList<>());
                System.out.println("📭 No hay presupuestos guardados");
            }
        } catch (Exception e) {
            System.out.println("❌ Error al cargar presupuestos: " + e.getMessage());
            listaPresupuestos.setValue(new ArrayList<>());
        }
    }

    /**
     * Guardar presupuestos en SharedPreferences
     */
    private void guardarPresupuestos() {
        List<Presupuesto> currentList = listaPresupuestos.getValue();
        if (currentList != null && prefs != null) {
            String presupuestosJson = gson.toJson(currentList);
            prefs.edit().putString("lista_presupuestos", presupuestosJson).apply();
            System.out.println("💾 " + currentList.size() + " presupuestos guardados");
        }
    }

    public LiveData<List<Presupuesto>> getPresupuestos() {
        return listaPresupuestos;
    }

    public void agregarPresupuesto(Presupuesto presupuesto) {
        List<Presupuesto> currentList = listaPresupuestos.getValue();

        if (currentList != null) {
            List<Presupuesto> newList = new ArrayList<>(currentList);
            newList.add(presupuesto);
            listaPresupuestos.setValue(newList);

            System.out.println("✅ Presupuesto agregado: " + presupuesto.getNombre());
            guardarPresupuestos(); // ✅ GUARDAR INMEDIATAMENTE
        }
    }

    public void eliminarPresupuesto(int position) {
        List<Presupuesto> currentList = listaPresupuestos.getValue();

        if (currentList != null && position >= 0 && position < currentList.size()) {
            List<Presupuesto> newList = new ArrayList<>(currentList);
            Presupuesto eliminado = newList.remove(position);
            listaPresupuestos.setValue(newList);

            System.out.println("🗑️ Presupuesto eliminado: " + eliminado.getNombre());
            guardarPresupuestos(); // ✅ GUARDAR INMEDIATAMENTE
        }
    }

    public int getCantidadPresupuestos() {
        List<Presupuesto> currentList = listaPresupuestos.getValue();
        return currentList != null ? currentList.size() : 0;
    }

    /**
     * Limpiar todos los presupuestos (para testing)
     */
    public void limpiarTodosLosPresupuestos() {
        listaPresupuestos.setValue(new ArrayList<>());
        if (prefs != null) {
            prefs.edit().remove("lista_presupuestos").apply();
        }
        System.out.println("🧹 Todos los presupuestos eliminados");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        System.out.println("💀 ViewModel destruido - pero los datos están guardados en SharedPreferences");
    }
}