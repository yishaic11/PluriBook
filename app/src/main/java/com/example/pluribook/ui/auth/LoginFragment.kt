package com.example.pluribook.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.PluribookApplication
import com.example.pluribook.R
import com.example.pluribook.data.repository.AuthRepository
import com.example.pluribook.utils.AuthState
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class LoginFragment : Fragment(R.layout.fragment_login) {
    private val viewModel: AuthViewModel by viewModels {
        val application = requireActivity().application as PluribookApplication
        val repository = AuthRepository(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance(),
            FirebaseStorage.getInstance(),
            application.database.userDao()
        )
        AuthViewModelFactory(repository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoToSignup = view.findViewById<TextView>(R.id.tvGoToSignup)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        tvGoToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    btnLogin.isEnabled = false
                    btnLogin.text = "Logging in..."
                }

                is AuthState.Success -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Log in"
                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }

                is AuthState.Error -> {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Log in"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}