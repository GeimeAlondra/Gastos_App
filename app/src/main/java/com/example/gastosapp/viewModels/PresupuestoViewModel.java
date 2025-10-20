package com.example.gastosapp.viewModels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.gastosapp.Models.Presupuesto;

import java.util.ArrayList;
import java.util.List;

public class PresupuestoViewModel extends ViewModel {

    private MutableLiveData<List<Presupuesto>> listaPresupuestos = new MutableLiveData<>();

    public PresupuestoViewModel() {
        listaPresupuestos.setValue(new ArrayList<>());
        System.out.println("ViewModel creado");
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

            System.out.println("ViewModel: Presupuesto agregado - " + presupuesto.getNombre());
        }
    }

    //  AGREGA ESTE MÉTODO NUEVO
    public void eliminarPresupuesto(int position) {
        List<Presupuesto> currentList = listaPresupuestos.getValue();

        if (currentList != null && position >= 0 && position < currentList.size()) {
            List<Presupuesto> newList = new ArrayList<>(currentList);
            Presupuesto eliminado = newList.remove(position);
            listaPresupuestos.setValue(newList);

            System.out.println("ViewModel: Presupuesto eliminado - " + eliminado.getNombre());
            System.out.println("   Total presupuestos ahora: " + newList.size());
        } else {
            System.out.println("ViewModel: No se pudo eliminar - posición inválida: " + position);
        }
    }

    public int getCantidadPresupuestos() {
        List<Presupuesto> currentList = listaPresupuestos.getValue();
        return currentList != null ? currentList.size() : 0;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        System.out.println("ViewModel destruido");
    }
}