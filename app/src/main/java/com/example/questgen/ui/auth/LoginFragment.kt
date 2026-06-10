package com.example.questgen.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.questgen.databinding.FragmentLoginBinding
import com.example.questgen.ui.main.MainActivity
import com.example.questgen.viewmodel.AuthState
import com.example.questgen.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(requireContext(), "Login com Google indisponível offline", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                binding.tvError.text = "Preencha todos os campos"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.tvRegisterLink.setOnClickListener {
            (activity as? AuthActivity)?.navigateToRegister()
        }

        // Observe flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Idle -> {
                        setLoading(false)
                        binding.tvError.visibility = View.GONE
                    }
                    is AuthState.Loading -> {
                        setLoading(true)
                        binding.tvError.visibility = View.GONE
                    }
                    is AuthState.Success -> {
                        setLoading(false)
                        // Launch Main Dashboard
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is AuthState.Error -> {
                        setLoading(false)
                        binding.tvError.text = state.message
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.btnLogin.text = ""
            binding.btnLogin.isEnabled = false
            binding.progressLogin.visibility = View.VISIBLE
        } else {
            binding.btnLogin.text = "ENTRAR NA ARENA"
            binding.btnLogin.isEnabled = true
            binding.progressLogin.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
