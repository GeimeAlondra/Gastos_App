package com.example.gastosapp.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.example.gastosapp.Models.Presupuesto;
import com.example.gastosapp.R;
import com.example.gastosapp.viewModels.PresupuestoViewModel; // ✅ IMPORT CORREGIDO

import java.util.List;

public class FragmentPresupuesto extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    // ✅ VARIABLE CORREGIDA: PresupuestoViewModel (con P mayúscula)
    private PresupuestoViewModel viewModel;
    private LinearLayout containerPresupuestos;

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

        // ✅ INICIALIZACIÓN CORREGIDA
        viewModel = new ViewModelProvider(this).get(PresupuestoViewModel.class);
        System.out.println("✅ ViewModel inicializado");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_presupuesto, container, false);

        // Inicializar vistas
        containerPresupuestos = view.findViewById(R.id.containerPresupuestos);
        LottieAnimationView btnAddCategory = view.findViewById(R.id.agregarPresupuesto);

        System.out.println("🔍 Vistas inicializadas");

        // Configurar Observer para los presupuestos
        viewModel.getPresupuestos().observe(getViewLifecycleOwner(), new Observer<List<Presupuesto>>() {
            @Override
            public void onChanged(List<Presupuesto> presupuestos) {
                System.out.println("👀 Observer ejecutado - " + presupuestos.size() + " presupuestos");
                actualizarVistaPresupuestos(presupuestos);
            }
        });

        // Configurar click listener del botón
        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("🎯 Botón presionado");
                btnAddCategory.playAnimation();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showFloatingWindow();
                    }
                }, 500);
            }
        });

        return view;
    }

    private void actualizarVistaPresupuestos(List<Presupuesto> presupuestos) {
        System.out.println("🎨 Actualizando vista con " + presupuestos.size() + " presupuestos");

        // Limpiar el contenedor
        containerPresupuestos.removeAllViews();

        if (presupuestos.isEmpty()) {
            // Mostrar estado vacío
            TextView tvEmpty = new TextView(requireContext());
            tvEmpty.setText("No hay presupuestos. ¡Agrega uno nuevo! 📝");
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(View.TEXT_ALIGNMENT_CENTER);
            tvEmpty.setPadding(0, 50, 0, 50);
            tvEmpty.setTextColor(getResources().getColor(android.R.color.darker_gray));
            containerPresupuestos.addView(tvEmpty);

            System.out.println("📭 Mostrando estado vacío");
        } else {
            // Agregar cada presupuesto a la vista
            for (int i = 0; i < presupuestos.size(); i++) {
                Presupuesto presupuesto = presupuestos.get(i);
                View itemView = crearItemPresupuesto(presupuesto, i);
                containerPresupuestos.addView(itemView);
            }
            System.out.println("✅ " + presupuestos.size() + " presupuestos mostrados");
        }
    }

    private void showFloatingWindow() {
        System.out.println("🪟 Mostrando diálogo de agregar presupuesto");

        try {
            FragmentAgregarPresupuesto dialogFragment = new FragmentAgregarPresupuesto();

            // Configurar el listener para cuando se guarde un presupuesto
            dialogFragment.setPresupuestoGuardadoListener(new FragmentAgregarPresupuesto.PresupuestoGuardadoListener() {
                @Override
                public void onPresupuestoGuardado(Presupuesto presupuesto) {
                    System.out.println("🎉 Presupuesto guardado recibido: " + presupuesto.getNombre());

                    // Agregar el presupuesto al ViewModel
                    viewModel.agregarPresupuesto(presupuesto);

                    Toast.makeText(requireContext(), "¡Presupuesto agregado!", Toast.LENGTH_SHORT).show();
                }
            });

            // Mostrar el diálogo
            dialogFragment.show(getParentFragmentManager(), "presupuesto_dialog");

        } catch (Exception e) {
            System.out.println("❌ Error al mostrar diálogo: " + e.getMessage());
            Toast.makeText(requireContext(), "Error al abrir formulario", Toast.LENGTH_SHORT).show();
        }
    }

    private View crearItemPresupuesto(Presupuesto presupuesto, int position) {
        System.out.println("🛠️ Creando item para: " + presupuesto.getNombre());

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View itemView = inflater.inflate(R.layout.item_presupuesto, containerPresupuestos, false);

        try {
            // Configurar las vistas del item
            TextView tvNombre = itemView.findViewById(R.id.tvNombrePresupuesto);
            TextView tvCantidad = itemView.findViewById(R.id.tvCantidad);
            TextView tvFechaInicio = itemView.findViewById(R.id.tvFechaInicio);
            TextView tvFechaFinal = itemView.findViewById(R.id.tvFechaFinal);
            TextView tvEstado = itemView.findViewById(R.id.tvEstado);

            // Establecer los datos
            tvNombre.setText(presupuesto.getNombre());
            tvCantidad.setText(String.format("$%.2f", presupuesto.getCantidad()));
            tvFechaInicio.setText(presupuesto.getFechaInicio());
            tvFechaFinal.setText(presupuesto.getFechaFinal());
            tvEstado.setText("Activo");

            // Configurar botón de eliminar
            View btnEliminar = itemView.findViewById(R.id.btnEliminar);
            if (btnEliminar != null) {
                btnEliminar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        eliminarPresupuesto(position);
                    }
                });
            }

        } catch (Exception e) {
            System.out.println("❌ Error al configurar item: " + e.getMessage());
        }

        return itemView;
    }

    private void eliminarPresupuesto(int position) {
        System.out.println("🗑️ Eliminando presupuesto en posición: " + position);

        // ✅ AHORA SÍ EXISTE este método en el ViewModel
        viewModel.eliminarPresupuesto(position);
        Toast.makeText(requireContext(), "Presupuesto eliminado", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("🔄 FragmentPresupuesto: onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        System.out.println("⏸️ FragmentPresupuesto: onPause");
    }
}