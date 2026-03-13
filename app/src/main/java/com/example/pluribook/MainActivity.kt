package com.example.pluribook

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.pluribook.data.model.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setupWithNavController(navController)

        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.nav_graph)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            navGraph.setStartDestination(R.id.homeFragment)
            syncCurrentUserToRoom(currentUser.uid)
        } else {
            navGraph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = navGraph

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.signUpFragment) {
                bottomNavigationView.visibility = View.GONE
            } else {
                bottomNavigationView.visibility = View.VISIBLE
            }
        }
    }

    private fun syncCurrentUserToRoom(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userDao = (application as PluribookApplication).database.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val document = db.collection("users").document(uid).get().await()

                if (document.exists()) {
                    val updatedUser = User(
                        uid = uid,
                        email = document.getString("email") ?: "",
                        username = document.getString("username") ?: "Unknown",
                        photoUrl = document.getString("photoUrl") ?: ""
                    )

                    userDao.saveUser(updatedUser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}