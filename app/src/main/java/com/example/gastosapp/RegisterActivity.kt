package com.example.gastosapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gastosapp.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Crear instancia de Firebase
        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Espere por favor")
        progressDialog.setCanceledOnTouchOutside(false)

        // Evento del usuario en registro
        binding.btnRegistrar.setOnClickListener {
            validarInformacion()
        }
    }

    // Crear 4 variables para el registro
    private var nombre = " "
    private var correo = " "
    private var password = " "
    private var r_password = " "

    private fun validarInformacion(){
        nombre = binding.rNombre.text.toString().trim()
        correo = binding.rCorreo.text.toString().trim()
        password = binding.rPassword.text.toString().trim()
        r_password = binding.rRPassword.text.toString().trim()

        // Validar campos
        if (nombre.isEmpty()){
            binding.rNombre.error = "Ingrese su nombre"
            binding.rNombre.requestFocus()

        }else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()){
            binding.rCorreo.error = "Correo invalido"
            binding.rCorreo.requestFocus()

        }else if (password.isEmpty()){
            binding.rPassword.error = "Ingrese una contraseña"
            binding.rPassword.requestFocus()

        }else if (r_password.isEmpty()){
            binding.rRPassword.error = "Repita la contraseña"
            binding.rRPassword.requestFocus()

        }else if (password != r_password){
            binding.rRPassword.error = "Las contraseñas no coinciden"
            binding.rRPassword.requestFocus()

        }else{
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener {
                actualizarInformacion()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fallo la creación de la cuenta debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarInformacion() {
        progressDialog.setMessage("Guardando información")

        val uidU = firebaseAuth.uid
        val nombreU = nombre
        val correoU = firebaseAuth.currentUser!!.email
        val tiempoR = Constantes.obtenerTiempoDelD()

        // Enviar informacion a Firebase
        val datosUsuarios = HashMap<String, Any>()
        datosUsuarios["uid"] = "$uidU"
        datosUsuarios["nombre"] = "$nombreU"
        datosUsuarios["correo"] = "$correoU"
        datosUsuarios["tiempoR"] = "$tiempoR"
        datosUsuarios["proveedor"] = "Email"
        datosUsuarios["estado"] = "online"

        // Guardamos la informacion en Firebase
        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(uidU!!)
            .setValue(datosUsuarios)
            .addOnCompleteListener {
                progressDialog.dismiss()
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }

            .addOnFailureListener { e ->
                Toast.makeText(this, "Fallo la creación de la cuenta debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



}