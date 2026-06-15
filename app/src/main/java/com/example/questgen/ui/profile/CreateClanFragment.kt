package com.example.questgen.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.questgen.databinding.FragmentCreateClanBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.CreateClanState
import com.example.questgen.viewmodel.MainViewModel

class CreateClanFragment : Fragment() {

    private var _binding: FragmentCreateClanBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateClanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = mainViewModel.currentUser.value
        if (currentUser != null) {
            if (currentUser.is_premium) {
                binding.btnCreateClanSubmit.text = "CRIAR CLÃ (GRÁTIS)"
                binding.tvPremiumFreeNotice.text = "* Status Elite Premium ativo: criação gratuita!"
            } else {
                binding.btnCreateClanSubmit.text = "CRIAR CLÃ (5.000 GC)"
                binding.tvPremiumFreeNotice.text = "* Custo de criação: 5.000 GameCoins (Grátis para Elite Premium)"
            }
        }

        binding.btnBackCreateClan.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCreateClanSubmit.setOnClickListener {
            val user = mainViewModel.currentUser.value ?: return@setOnClickListener
            val nome = binding.etClanName.text.toString().trim()
            val tag = binding.etClanTag.text.toString().trim().uppercase()
            val descricao = binding.etClanDesc.text.toString().trim()

            if (nome.isEmpty()) {
                toast("O nome do clã não pode ser vazio.")
                return@setOnClickListener
            }

            if (tag.length < 2 || tag.length > 5) {
                toast("A tag do clã deve ter entre 2 e 5 caracteres.")
                return@setOnClickListener
            }

            // Se o usuário não for premium, verificar saldo de GC
            if (!user.is_premium && user.game_coins < 5000) {
                toast("Saldo insuficiente! Você precisa de status Premium ou 5.000 GC.")
                return@setOnClickListener
            }

            mainViewModel.criarClan(nome, tag, descricao)
        }

        // Observar o estado de criação de clã
        collectLatestFlow(mainViewModel.createClanState) { state ->
            when (state) {
                is CreateClanState.Idle -> {
                    binding.progressCreateClanLoad.visibility = View.GONE
                    binding.btnCreateClanSubmit.isEnabled = true
                }
                is CreateClanState.Loading -> {
                    binding.progressCreateClanLoad.visibility = View.VISIBLE
                    binding.btnCreateClanSubmit.isEnabled = false
                }
                is CreateClanState.Success -> {
                    binding.progressCreateClanLoad.visibility = View.GONE
                    binding.btnCreateClanSubmit.isEnabled = true
                    toast(state.message)
                    mainViewModel.resetCreateClanState()
                    findNavController().popBackStack() // Fecha a tela de criação e volta
                }
                is CreateClanState.Error -> {
                    binding.progressCreateClanLoad.visibility = View.GONE
                    binding.btnCreateClanSubmit.isEnabled = true
                    toast(state.message)
                    mainViewModel.resetCreateClanState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
