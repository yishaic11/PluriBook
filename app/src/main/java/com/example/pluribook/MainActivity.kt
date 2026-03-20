package com.example.pluribook

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.nav_graph)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            navGraph.setStartDestination(R.id.home_fragment)
            viewModel.syncCurrentUser(currentUser.uid)
        } else {
            navGraph.setStartDestination(R.id.login_fragment)
        }
        navController.graph = navGraph

        bottomNavigationView.setOnItemSelectedListener { item ->
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(false)
                .setPopUpTo(
                    destinationId = navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = false
                )
                .build()

            navController.navigate(item.itemId, null, options)
            true
        }

        bottomNavigationView.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.login_fragment, R.id.signup_fragment -> {
                    bottomNavigationView.visibility = View.GONE
                }

                else -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }

            if (destination.id == R.id.home_fragment ||
                destination.id == R.id.create_post_fragment ||
                destination.id == R.id.profile_fragment
            ) {
                bottomNavigationView.menu.findItem(destination.id)?.isChecked = true
            }
        }
    }
}