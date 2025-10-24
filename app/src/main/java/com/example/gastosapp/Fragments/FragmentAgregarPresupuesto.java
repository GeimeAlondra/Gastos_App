package com.example.gastosapp.Fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.gastosapp.Models.Presupuesto;
import com.example.gastosapp.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class FragmentAgregarPresupuesto extends DialogFragment {

    private TextInputEditText etNombreGasto, etCantidad;
    private Button btnFechaInicio, btnFechaFinal;
    private TextView tvFechaInicio, tvFechaFinal;
    private Calendar calendarInicio, calendarFinal;
    private View rootView;

    // Interface para comunicación
    public interface PresupuestoGuardadoListener {
        void onPresupuestoGuardado(Presupuesto presupuesto);
    }

    private PresupuestoGuardadoListener listener;

    public void setPresupuestoGuardadoListener(PresupuestoGuardadoListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agregar_presupuesto, container, false);
        this.rootView = view; // Guardar la referencia

        // Inicializar vistas
        initViews();

        // Configurar listeners
        setupListeners();

        return view;
    }

    private void initViews() {
        // Usar rootView en lugar de view
        etNombreGasto = rootView.findViewById(R.id.etNombreGasto);
        etCantidad = rootView.findViewById(R.id.etCantidad);
        btnFechaInicio = rootView.findViewById(R.id.btnFechaInicio);
        btnFechaFinal = rootView.findViewById(R.id.btnFechaFinal);
        tvFechaInicio = rootView.findViewById(R.id.tvFechaInicio);
        tvFechaFinal = rootView.findViewById(R.id.tvFechaFinal);

        // Inicializar calendarios
        calendarInicio = Calendar.getInstance();
        calendarFinal = Calendar.getInstance();
        calendarFinal.add(Calendar.MONTH, 1);
    }

    private void setupListeners() {
        // Botón Fecha Inicio
        btnFechaInicio.setOnClickListener(v -> showDatePicker(true));

        // Botón Fecha Final
        btnFechaFinal.setOnClickListener(v -> showDatePicker(false));

        // Usar rootView
        rootView.findViewById(R.id.btnGuardar).setOnClickListener(v -> guardarPresupuesto());

        // Usar rootView
        rootView.findViewById(R.id.btnCancelar).setOnClickListener(v -> dismiss());
    }

    private void showDatePicker(final boolean isStartDate) {
        Calendar calendar = isStartDate ? calendarInicio : calendarFinal;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    updateDateDisplay(isStartDate, year, month, dayOfMonth);

                    if (isStartDate && calendarFinal.before(calendarInicio)) {
                        calendarFinal.setTime(calendarInicio.getTime());
                        updateDateDisplay(false,
                                calendarFinal.get(Calendar.YEAR),
                                calendarFinal.get(Calendar.MONTH),
                                calendarFinal.get(Calendar.DAY_OF_MONTH));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        if (isStartDate) {
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        } else {
            datePickerDialog.getDatePicker().setMinDate(calendarInicio.getTimeInMillis());
        }

        datePickerDialog.show();
    }

    private void updateDateDisplay(boolean isStartDate, int year, int month, int day) {
        String dateString = String.format("%02d/%02d/%d", day, month + 1, year);

        if (isStartDate) {
            tvFechaInicio.setText(dateString);
            btnFechaInicio.setText("Cambiar");
        } else {
            tvFechaFinal.setText(dateString);
            btnFechaFinal.setText("Cambiar");
        }
    }

    private void guardarPresupuesto() {
        System.out.println("DEBUG: guardarPresupuesto() INICIADO");

        if (!validarCampos()) {
            System.out.println("DEBUG: Validación falló");
            return;
        }

        String nombre = etNombreGasto.getText().toString().trim();
        double cantidad = Double.parseDouble(etCantidad.getText().toString().trim());
        String fechaInicio = tvFechaInicio.getText().toString();
        String fechaFinal = tvFechaFinal.getText().toString();

        System.out.println("   DEBUG: Datos capturados:");
        System.out.println("   Nombre: " + nombre);
        System.out.println("   Cantidad: " + cantidad);
        System.out.println("   Fecha Inicio: " + fechaInicio);
        System.out.println("   Fecha Final: " + fechaFinal);

        Presupuesto presupuesto = new Presupuesto(nombre, cantidad, fechaInicio, fechaFinal);
        System.out.println("DEBUG: Objeto Presupuesto creado");

        // VERIFICAR SI EL LISTENER ESTÁ CONFIGURADO
        if (listener != null) {
            System.out.println("DEBUG: Listener NO es null - llamando onPresupuestoGuardado");
            listener.onPresupuestoGuardado(presupuesto);
        } else {
            System.out.println("DEBUG: Listener ES null - NO se llamará onPresupuestoGuardado");
            Toast.makeText(requireContext(), "Error: Listener no configurado", Toast.LENGTH_LONG).show();
        }

        System.out.println("DEBUG: guardarPresupuesto() FINALIZADO");
        dismiss();
        Toast.makeText(requireContext(), "Presupuesto guardado!", Toast.LENGTH_SHORT).show();
    }

    private boolean validarCampos() {
        String nombre = etNombreGasto.getText().toString().trim();
        String cantidadStr = etCantidad.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombreGasto.setError("Ingresa un nombre para el gasto");
            return false;
        }

        if (cantidadStr.isEmpty()) {
            etCantidad.setError("Ingresa la cantidad");
            return false;
        }

        if (tvFechaInicio.getText().toString().equals("No seleccionada")) {
            Toast.makeText(requireContext(), "Selecciona la fecha de inicio", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (tvFechaFinal.getText().toString().equals("No seleccionada")) {
            Toast.makeText(requireContext(), "Selecciona la fecha final", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
