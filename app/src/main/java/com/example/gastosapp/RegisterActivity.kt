package com.example.gastosapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gastosapp.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("RegisterActivity", "Inicio de sesión exitoso: ${account.email}")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("RegisterActivity", "Error de inicio de sesión", e)
            Toast.makeText(this, "Error al iniciar sesión con Google: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

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

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this).apply {
            setTitle("Espere por favor")
            setCanceledOnTouchOutside(false)
        }

        // Configurar Google Sign-In
        configurarGoogleSignIn()

        // Boton oara completar registro con nombre y contraseña
        binding.btnRegistrar.setOnClickListener {
            validarInformacion()
        }

        // Boton para registrarse con Google
        binding.registerGoogle.setOnClickListener {
            iniciarSesionConGoogle()
        }
    }

    private fun configurarGoogleSignIn() {
        val webClientId = getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun iniciarSesionConGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        progressDialog.setMessage("Autenticando con Google")
        progressDialog.show()

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val uid = user?.uid
                    val database = FirebaseDatabase.getInstance().reference.child("usuarios")
                    val correo = user?.email ?: ""

                    if (uid != null) {
                        // Verificar si el correo ya está en la base de datos
                        database.orderByChild("correo").equalTo(correo).get()
                            .addOnSuccessListener { snapshot ->
                                Log.d("RegisterActivity", "Consulta de correo en base de datos: ${snapshot.exists()}")
                                if (snapshot.exists()) {
                                    progressDialog.dismiss()
                                    firebaseAuth.signOut()
                                    Toast.makeText(this, "El correo $correo ya está registrado. Intente iniciar sesión.", Toast.LENGTH_LONG).show()
                                } else {
                                    // Verificar si el correo está en Firebase Authentication
                                    firebaseAuth.fetchSignInMethodsForEmail(correo)
                                        .addOnCompleteListener { authTask ->
                                            if (authTask.isSuccessful) {
                                                val signInMethods = authTask.result?.signInMethods
                                                Log.d("RegisterActivity", "Métodos de inicio para $correo: $signInMethods")
                                                if (signInMethods?.isNotEmpty() == true) {
                                                    progressDialog.dismiss()
                                                    firebaseAuth.signOut()
                                                    Toast.makeText(this, "El correo $correo ya está registrado. Intente iniciar sesión.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    // Si no existe, registrar al usuario
                                                    progressDialog.setMessage("Guardando información")
                                                    val nombreU = user.displayName ?: ""
                                                    val tiempoR = Constantes.obtenerTiempoDelD()

                                                    val datosUsuarios = hashMapOf(
                                                        "uid" to uid,
                                                        "nombre" to nombreU,
                                                        "correo" to correo,
                                                        "tiempoR" to tiempoR,
                                                        "proveedor" to "Google",
                                                        "estado" to "online"
                                                    )

                                                    database.child(uid).setValue(datosUsuarios)
                                                        .addOnSuccessListener {
                                                            Log.d("RegisterActivity", "Usuario guardado exitosamente en usuarios con UID: $uid")
                                                            progressDialog.dismiss()
                                                            startActivity(Intent(this, DashboardActivity::class.java))
                                                            finish()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("RegisterActivity", "Error al guardar datos en usuarios: ${e.message}", e)
                                                            progressDialog.dismiss()
                                                            Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                            } else {
                                                Log.e("RegisterActivity", "Error al verificar métodos de inicio de sesión: ${authTask.exception?.message}", authTask.exception)
                                                progressDialog.dismiss()
                                                Toast.makeText(this, "Error al verificar el correo: ${authTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("RegisterActivity", "Fallo en fetchSignInMethodsForEmail: ${e.message}", e)
                                            progressDialog.dismiss()
                                            Toast.makeText(this, "Error al verificar el correo: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("RegisterActivity", "Error de verificación de correo en base de datos: ${e.message}", e)
                                progressDialog.dismiss()
                                Toast.makeText(this, "Error al verificar el correo en la base de datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    val exception = task.exception as? FirebaseAuthException
                    if (exception?.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                        progressDialog.dismiss()
                        firebaseAuth.signOut()
                        Toast.makeText(this, "El correo ya está registrado. Intente iniciar sesión.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e("RegisterActivity", "Error de FirebaseAuth: ${task.exception?.message}", task.exception)
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error al autenticar con Google: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private var nombre = ""
    private var correo = ""
    private var password = ""
    private var r_password = ""

    private fun validarInformacion() {
        nombre = binding.rNombre.text.toString().trim()
        correo = binding.rCorreo.text.toString().trim()
        password = binding.rPassword.text.toString().trim()
        r_password = binding.rRPassword.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.rNombre.error = "Ingrese su nombre"
            binding.rNombre.requestFocus()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.rCorreo.error = "Correo inválido"
            binding.rCorreo.requestFocus()
        } else if (password.isEmpty()) {
            binding.rPassword.error = "Ingrese una contraseña"
            binding.rPassword.requestFocus()
        } else if (r_password.isEmpty()) {
            binding.rRPassword.error = "Repita la contraseña"
            binding.rRPassword.requestFocus()
        } else if (password != r_password) {
            binding.rRPassword.error = "Las contraseñas no coinciden"
            binding.rRPassword.requestFocus()
        } else {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        progressDialog.setMessage("Creando cuenta")
        progressDialog.show()

        firebaseAuth.createUserWithEmailAndPassword(correo, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    actualizarInformacion()
                } else {
                    val exception = it.exception as? FirebaseAuthException
                    if (exception?.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                        progressDialog.dismiss()
                        Toast.makeText(this, "El correo $correo ya está registrado. Intente iniciar sesión.", Toast.LENGTH_LONG).show()
                    } else {
                        progressDialog.dismiss()
                        Log.e("RegisterActivity", "Error al crear usuario: ${it.exception?.message}", it.exception)
                        Toast.makeText(this, "Fallo la creación de la cuenta debido a ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("RegisterActivity", "Fallo en registro: ${e.message}", e)
                Toast.makeText(this, "Fallo la creación de la cuenta debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarInformacion() {
        progressDialog.setMessage("Guardando información")

        val uidU = firebaseAuth.uid
        val nombreU = nombre
        val correoU = firebaseAuth.currentUser!!.email
        val tiempoR = Constantes.obtenerTiempoDelD()

        val datosUsuarios = hashMapOf(
            "uid" to (uidU ?: ""),
            "nombre" to nombreU,
            "correo" to (correoU ?: ""),
            "tiempoR" to tiempoR,
            "proveedor" to "Email",
            "estado" to "online"
        )

        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(uidU!!)
            .setValue(datosUsuarios)
            .addOnCompleteListener {
                progressDialog.dismiss()
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Fallo la creación de la cuenta debido a ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}