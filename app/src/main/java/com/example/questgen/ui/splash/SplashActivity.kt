package com.example.questgen.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.questgen.data.repository.UserRepository
import com.example.questgen.databinding.ActivitySplashBinding
import com.example.questgen.ui.auth.AuthActivity
import com.example.questgen.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            // Simulate protocol activation loading
            delay(2000)
            
            // Check session
            val userRepository = UserRepository(applicationContext)
            val currentUser = userRepository.getSavedUser()

            if (currentUser != null) {
                // Navigate directly to home
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // Navigate to login/registration
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
            }
            finish()
        }
    }
}
