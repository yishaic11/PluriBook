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
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editTextEmail = view.findViewById<TextInputEditText>(R.id.edit_text_signup_email)
        val editTextPassword = view.findViewById<TextInputEditText>(R.id.edit_text_signup_password)
        val buttonLogin = view.findViewById<MaterialButton>(R.id.button_login)
        val textViewGoToSignup = view.findViewById<TextView>(R.id.text_view_go_to_signup)

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        textViewGoToSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    buttonLogin.isEnabled = false
                    buttonLogin.setText(R.string.loading_logging_in)
                }

                is AuthState.Success -> {
                    buttonLogin.isEnabled = true
                    buttonLogin.setText(R.string.login_button)
                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()

                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }

                is AuthState.Error -> {
                    buttonLogin.isEnabled = true
                    buttonLogin.setText(R.string.login_button)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}