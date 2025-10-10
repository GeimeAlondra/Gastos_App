package com.example.gastosapp

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gastosapp.Models.Registro
import com.example.gastosapp.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var refBase: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d("LoginActivity", "Google Sign-In exitoso: ${account.email}")
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.e("LoginActivity", "Error Google Sign-In", e)
            Toast.makeText(this, "Error al iniciar sesión con Google: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización
        auth = FirebaseAuth.getInstance()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        refBase = FirebaseDatabase.getInstance().getReference("usuarios")

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Redirigir al registro desde login
        binding.sinCuenta.setText(Html.fromHtml(getString(R.string.sin_cuenta), Html.FROM_HTML_MODE_LEGACY))
        binding.sinCuenta.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Referencias a los EditText
        val textCorreo = findViewById<EditText>(R.id.text_correo)
        val textPassword = findViewById<EditText>(R.id.text_password)

        // Configurar Google Sign-In entes de usarlo
        configurarGoogleSignIn()

        // Login normal
        binding.btnLogin.setOnClickListener {
            val correo = textCorreo.text.toString().trim()
            val password = textPassword.text.toString().trim()

            // Validación
            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingrese correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Login con Firebase Auth
            auth.signInWithEmailAndPassword(correo, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        Log.d("LoginActivity", "Login exitoso: $uid")

                        // Verificar datos en Realtime Database
                        refBase.child(uid).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    // Ir al Dashboard
                                    val intent = Intent(this, DashboardActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("LoginActivity", "Error al obtener datos", e)
                                Toast.makeText(this, "Error al obtener datos", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("LoginActivity", "Error login", task.exception)
                        Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Login con Google
        binding.btnLoginGoogle.setOnClickListener {
            Log.d("LoginActivity", "Iniciando Google Sign-In...")
            iniciarSesionConGoogle()
        }
    }

    private fun configurarGoogleSignIn() {
        try {
            val webClientId = getString(R.string.default_web_client_id)
            Log.d("LoginActivity", "Web Client ID: $webClientId")

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)
            Log.d("LoginActivity", "Google Sign-In configurado correctamente")
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error configurando Google Sign-In", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun iniciarSesionConGoogle() {
        // Cerrar sesión previa para forzar selector de cuenta
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    val database = FirebaseDatabase.getInstance().reference.child("usuarios")

                    if (uid != null) {
                        Log.d("LoginActivity", "Autenticación exitosa. UID: $uid")

                        // Verificar si existe en la base de datos
                        database.child(uid).get().addOnSuccessListener { snapshot ->
                            if (snapshot.exists()) {
                                // El usuario ya existe
                                Log.d("LoginActivity", "Usuario existente encontrado")
                                startActivity(Intent(this, DashboardActivity::class.java))
                                Toast.makeText(this, "Bienvenido de nuevo", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                // El usuario no existe - crear registro básico
                                Log.d("LoginActivity", "Nuevo usuario, creando registro...")
                                val registro = Registro(
                                    nombre = user.displayName ?: "Usuario",
                                    correo = user.email ?: ""
                                )

                                database.child(uid).setValue(registro).addOnSuccessListener {
                                    // Mandar al registro para completar datos
                                    val intent = Intent(this, RegisterActivity::class.java)
                                    intent.putExtra("fromGoogleSignIn", true)
                                    startActivity(intent)
                                    Toast.makeText(this, "Complete su registro", Toast.LENGTH_SHORT).show()
                                    finish()
                                }.addOnFailureListener { e ->
                                    Log.e("LoginActivity", "Error guardando datos", e)
                                    Toast.makeText(this, "Error guardando datos", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("LoginActivity", "Error al verificar usuario", e)
                            Toast.makeText(this, "Error al verificar usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("LoginActivity", "Error Firebase Auth", task.exception)
                    Toast.makeText(this, "Error al autenticar con Google", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // Verificar si el usuario ya está autenticado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("LoginActivity", "Usuario ya autenticado: ${currentUser.uid}")
            // Redirigir automáticamente
            // startActivity(Intent(this, InicioActivity::class.java))
            // finish()
        }
    }
}