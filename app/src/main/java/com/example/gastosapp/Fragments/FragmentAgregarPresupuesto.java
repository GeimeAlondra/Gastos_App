package com.example.gastosapp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.gastosapp.R;

public class FragmentAgregarPresupuesto extends DialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialogTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agregar_presupuesto, container, false);

        view.findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarPresupuesto());
        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());

        return view;
    }

    private void guardarPresupuesto() {
        System.out.println("💾 Guardando presupuesto...");
        dismiss();
    }
}