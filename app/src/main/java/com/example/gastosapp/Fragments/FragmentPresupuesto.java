package com.example.gastosapp.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.gastosapp.R;

public class FragmentPresupuesto extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public FragmentPresupuesto() {
        // Required empty public constructor
    }

    public static FragmentPresupuesto newInstance(String param1, String param2) {
        FragmentPresupuesto fragment = new FragmentPresupuesto();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_presupuesto, container, false);

        LottieAnimationView btnAddCategory = view.findViewById(R.id.agregarPresupuesto);

        System.out.println("🔍 DEBUG: onCreateView ejecutado");
        System.out.println("🔍 DEBUG: Botón encontrado: " + (btnAddCategory != null));

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("🎯 CLICK DETECTADO: El botón SÍ está funcionando");

                btnAddCategory.playAnimation();
                System.out.println("🎬 ANIMACIÓN: Debería estar reproduciéndose");

                // Método modificado para ventana flotante
                showFloatingWindow();
            }
        });

        return view;
    }

    private void showFloatingWindow() {
        System.out.println("🪟 ABRIENDO: DialogFragment");

        try {
            FragmentAgregarPresupuesto dialogFragment = new FragmentAgregarPresupuesto();

            // Usar el FragmentManager de la Activity, no el ChildFragmentManager
            dialogFragment.show(getParentFragmentManager(), "presupuesto_dialog");

            System.out.println("🎉 ÉXITO: Dialog abierto");

        } catch (Exception e) {
            System.out.println("💥 ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para cerrar la ventana (llamado desde el fragmento hijo)
    public void closeFloatingWindow() {
        try {
            System.out.println("🚪 CERRANDO: Ventana flotante");

            // 1. Ocultar el contenedor
            View containerView = getView().findViewById(R.id.child_fragment_container);
            if (containerView != null) {
                containerView.setVisibility(View.GONE);
                System.out.println("✅ CONTENEDOR: Ocultado");
            }

            // 2. Remover del back stack
            getChildFragmentManager().popBackStack();
            System.out.println("✅ VENTANA: Cerrada correctamente");

        } catch (Exception e) {
            System.out.println("❌ ERROR al cerrar: " + e.getMessage());
        }
    }

    // Método para cuando el usuario presiona back
    public boolean onBackPressed() {
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            closeFloatingWindow();
            return true; // Indica que manejó el back press
        }
        return false; // No manejó el back press
    }
}