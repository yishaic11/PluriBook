package com.example.pluribook.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.pluribook.PluribookApplication
import com.example.pluribook.R
import com.example.pluribook.data.repository.AuthRepository
import com.example.pluribook.utils.AuthState
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class SignUpFragment : Fragment(R.layout.fragment_signup) {
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

    private var selectedImageUri: Uri? = null
    private var imgProfile: ShapeableImageView? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                imgProfile?.let { view ->
                    Picasso.get().load(it).fit().centerCrop().into(view)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etUsername = view.findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignup = view.findViewById<MaterialButton>(R.id.btnSignup)
        val tvGoToLogin = view.findViewById<TextView>(R.id.tvGoToLogin)
        val btnSelectPhoto = view.findViewById<ExtendedFloatingActionButton>(R.id.btnSelectPhoto)
        imgProfile = view.findViewById(R.id.imgProfile)

        btnSelectPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSignup.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signup(email, password, username, selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        tvGoToLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    btnSignup.isEnabled = false
                    btnSignup.text = "Creating account & uploading photo..."
                }

                is AuthState.Success -> {
                    btnSignup.isEnabled = true
                    btnSignup.text = "Sign up"
                    Toast.makeText(
                        requireContext(),
                        "Account created successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                }

                is AuthState.Error -> {
                    btnSignup.isEnabled = true
                    btnSignup.text = "Sign up"
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imgProfile = null
    }
}