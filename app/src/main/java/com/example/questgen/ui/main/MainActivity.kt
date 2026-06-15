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

        // Safe navigation selection wrapper to mitigate NullPointerException crashes during swift navigation state shifts
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            try {
                if (navController.currentDestination != null) {
                    androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        // Handle item reselection (e.g. clicking Home tab again from details to pop back to HomeFragment)
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            try {
                if (navController.currentDestination != null) {
                    navController.popBackStack(item.itemId, false)
                }
            } catch (e: Exception) {
                try {
                    navController.navigate(item.itemId)
                } catch (ex: Exception) {}
            }
        }

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

        // Global notification loop
        lifecycleScope.launch {
            while (true) {
                val user = sharedViewModel.currentUser.value
                if (user != null) {
                    try {
                        val repository = com.example.questgen.data.repository.ChallengeRepository(applicationContext)
                        val response = repository.verificarNotificacoes(user.id)
                        if (response.status == "success" && response.houveMudanca) {
                            showGlobalNotificationDialog(response)
                        }
                    } catch (e: Exception) {
                        // Ignore background network checker anomalies
                    }
                }
                kotlinx.coroutines.delay(7000) // check every 7 seconds
            }
        }
    }

    private fun showGlobalNotificationDialog(response: com.example.questgen.data.model.ChallengeNotificationResponse) {
        val titleText = if (response.resultado == "COMPLETED") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml("<font color='#00FF7F'>🏆 DESAFIO APROVADO!</font>", android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml("<font color='#00FF7F'>🏆 DESAFIO APROVADO!</font>")
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml("<font color='#FF4C4C'>❌ DESAFIO RECUSADO</font>", android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml("<font color='#FF4C4C'>❌ DESAFIO RECUSADO</font>")
            }
        }

        val message = if (response.resultado == "COMPLETED") {
            "Sua jogada no desafio \"${response.tituloDesafio}\" foi validada com sucesso pela IA. +${response.recompensa} GameCoins creditadas!"
        } else {
            "Não conseguimos validar o seu comprovante para o desafio \"${response.tituloDesafio}\". Tente novamente!"
        }

        val positiveButtonText = if (response.resultado == "COMPLETED") "IR PARA O HISTÓRICO" else "FECHAR"

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(titleText)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                sharedViewModel.loadCurrentUser()
                sharedViewModel.notifyChallengeStatusChanged()
                if (response.resultado == "COMPLETED") {
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                    navHostFragment?.navController?.navigate(R.id.historyFragment)
                }
                dialog.dismiss()
            }

        if (response.resultado == "COMPLETED") {
            builder.setNegativeButton("FECHAR") { dialog, _ ->
                sharedViewModel.loadCurrentUser()
                sharedViewModel.notifyChallengeStatusChanged()
                dialog.dismiss()
            }
        }

        builder.show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh session on dashboard resume
        sharedViewModel.loadCurrentUser()
    }
}
