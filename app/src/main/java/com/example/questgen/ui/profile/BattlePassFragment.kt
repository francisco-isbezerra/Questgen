package com.example.questgen.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.questgen.R
import com.example.questgen.data.model.BattlePassReward
import com.example.questgen.databinding.FragmentBattlePassBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.MainViewModel

class BattlePassFragment : Fragment() {

    private var _binding: FragmentBattlePassBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var battlePassAdapter: BattlePassAdapter

    // Create the mockup list of rewards
    private val rewardsList = listOf(
        BattlePassReward(1, 1, false, "+50 GameCoins", "Moedas grátis para gastar na loja de prêmios."),
        BattlePassReward(2, 1, true, "Borda Ciano Neon", "Uma moldura neon ciano especial para o seu avatar."),
        BattlePassReward(3, 2, false, "+20 XP Extra", "Bônus de XP para subir de nível mais rápido."),
        BattlePassReward(4, 2, true, "+150 GameCoins", "Moedas bônus exclusivas do Passe Premium."),
        BattlePassReward(5, 3, false, "+100 GameCoins", "Mais saldo para a sua carteira global."),
        BattlePassReward(6, 3, true, "Moldura Roxo Neon", "Uma moldura neon roxo luxuosa para destacar seu perfil."),
        BattlePassReward(7, 4, false, "+30 XP Extra", "Impulsione seu progresso na arena."),
        BattlePassReward(8, 4, true, "+250 GameCoins", "Aproveite a recompensa premium do nível 4."),
        BattlePassReward(9, 5, false, "+150 GameCoins", "Recompensa gratuita de nível 5."),
        BattlePassReward(10, 5, true, "Título 'Final Boss'", "Título exclusivo exibido em dourado no seu perfil."),
        BattlePassReward(11, 6, false, "+40 XP Extra", "Falta pouco para os níveis avançados!"),
        BattlePassReward(12, 6, true, "+400 GameCoins", "Recompensa de moedas de alta patente."),
        BattlePassReward(13, 7, false, "+200 GameCoins", "Crédito gratuito para itens raros na loja."),
        BattlePassReward(14, 7, true, "Moldura Ouro Lendário", "A moldura dourada definitiva dos campeões."),
        BattlePassReward(15, 8, false, "+50 XP Extra", "Alcance novos horizontes de pontuação."),
        BattlePassReward(16, 8, true, "+600 GameCoins", "Moedas premium para acelerar suas conquistas."),
        BattlePassReward(17, 9, false, "+250 GameCoins", "Quase no nível máximo do passe!"),
        BattlePassReward(18, 9, true, "Ícone de Perfil Exclusivo", "Avatar customizado 'Gamer Ciano' desbloqueado."),
        BattlePassReward(19, 10, false, "+100 XP Extra", "Recompensa final gratuita!"),
        BattlePassReward(20, 10, true, "+1.000 GameCoins", "O prêmio máximo em dinheiro do Passe Premium.")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBattlePassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Back Button
        binding.btnBackBattlePass.setOnClickListener {
            findNavController().popBackStack()
        }

        // Initialize Adapter & RecyclerView
        val user = mainViewModel.currentUser.value
        val currentLvl = user?.nivel_atual ?: 1
        val isPremium = user?.is_premium ?: false

        battlePassAdapter = BattlePassAdapter(
            rewards = rewardsList,
            userLevel = currentLvl,
            isUserPremium = isPremium,
            onClaimClick = { reward ->
                // Simulate Claim
                reward.isClaimed = true
                battlePassAdapter.notifyDataSetChanged()
                toast("Recompensa resgatada com sucesso: ${reward.title}!")
            }
        )

        binding.rvBattlePassRewards.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = battlePassAdapter
        }

        // Observe currentUser for real-time level/XP and premium details
        collectLatestFlow(mainViewModel.currentUser) { userModel ->
            userModel?.let {
                binding.tvBpCurrentLevel.text = "NÍVEL ${it.nivel_atual}"
                
                if (it.is_premium) {
                    binding.tvBpLevelBadge.text = "PASSE PREMIUM ATIVO"
                    binding.tvBpLevelBadge.setTextColor(resources.getColor(R.color.raridade_lendario, null))
                    binding.tvBpLevelBadge.setBackgroundColor(android.graphics.Color.parseColor("#2E2800"))
                } else {
                    binding.tvBpLevelBadge.text = "PASSE GRATUITO"
                    binding.tvBpLevelBadge.setTextColor(resources.getColor(R.color.azul_neon, null))
                    binding.tvBpLevelBadge.setBackgroundColor(android.graphics.Color.parseColor("#122438"))
                }

                val progressXp = it.xp_total % 100
                binding.progressBpBar.progress = progressXp
                binding.tvBpXpDetails.text = "$progressXp / 100 XP para o próximo nível"

                // Update adapter data dynamically
                battlePassAdapter.updateUserData(it.nivel_atual, it.is_premium)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
