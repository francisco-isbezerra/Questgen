package com.example.questgen.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.questgen.databinding.FragmentRegisterBinding
import com.example.questgen.ui.main.MainActivity
import com.example.questgen.viewmodel.AuthState
import com.example.questgen.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                binding.tvError.text = "Preencha todos os campos"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.tvError.text = "A senha deve ter pelo menos 6 caracteres"
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            viewModel.register(name, email, password)
        }

        binding.tvLoginLink.setOnClickListener {
            (activity as? AuthActivity)?.navigateToLogin()
        }

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
            binding.btnRegister.text = ""
            binding.btnRegister.isEnabled = false
            binding.progressRegister.visibility = View.VISIBLE
        } else {
            binding.btnRegister.text = "CADASTRAR JOGADOR"
            binding.btnRegister.isEnabled = true
            binding.progressRegister.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
