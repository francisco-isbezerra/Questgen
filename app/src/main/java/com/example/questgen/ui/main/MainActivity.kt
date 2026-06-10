package com.example.questgen.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.questgen.R
import com.example.questgen.databinding.ActivityMainBinding
import com.example.questgen.ui.auth.AuthActivity
import com.example.questgen.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val sharedViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Jetpack Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Observe session state, redirect to login if session cleared
        lifecycleScope.launch {
            sharedViewModel.currentUser.collectLatest { user ->
                if (user == null) {
                    val intent = Intent(this@MainActivity, AuthActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh session on dashboard resume
        sharedViewModel.loadCurrentUser()
    }
}
