package com.example.gastosapp.Fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.gastosapp.LoginActivity
import com.example.gastosapp.MainActivity
import com.example.gastosapp.Models.Registro
import com.example.gastosapp.R
import com.example.gastosapp.databinding.FragmentPerfilBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class FragmentPerfil : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid != null) {
            val database = FirebaseDatabase.getInstance().reference

            binding.pNombre.visibility = View.GONE
            binding.pCorreo.visibility = View.GONE

            database.child("usuarios").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val usuario = snapshot.getValue(Registro::class.java)
                        usuario?.let {
                            binding.pNombre.text = it.nombre
                            binding.pCorreo.text = it.correo
                            binding.pNombre.visibility = View.VISIBLE
                            binding.pCorreo.visibility = View.VISIBLE
                        }

                        val user = FirebaseAuth.getInstance().currentUser
                        user?.photoUrl?.let { uri ->
                            Picasso.get().load(uri).into(binding.imgPerfil)
                        }
                    } else {
                        Toast.makeText(requireContext(), "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al obtener datos", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "No hay usuario logueado", Toast.LENGTH_SHORT).show()
        }

        binding.btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

    }

    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut()

        val intent = Intent(requireActivity(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }

}