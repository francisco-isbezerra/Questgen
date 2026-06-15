package com.example.questgen.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.questgen.R
import com.example.questgen.databinding.FragmentProfileBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.DeleteAccountState
import com.example.questgen.viewmodel.EditProfileState
import com.example.questgen.viewmodel.MainViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup options click listeners
        binding.layoutOptionHistory.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_historyFragment)
        }

        binding.layoutOptionLogout.setOnClickListener {
            mainViewModel.logout()
        }

        binding.layoutOptionDeleteAccount.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("EXCLUIR CONTA")
                .setMessage("ATENÇÃO: Esta ação é permanente e apagará todo o seu progresso de moedas e patentes na arena. Deseja continuar?")
                .setPositiveButton("EXCLUIR") { _, _ ->
                    mainViewModel.excluirConta()
                }
                .setNegativeButton("CANCELAR", null)
                .show()
        }

        // Setup edit profile floating button
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        // Setup premium button click listener
        binding.btnUpgradePremium.setOnClickListener {
            mainViewModel.comprarPremium()
        }

        // Setup create clan button click listener
        binding.btnCriarClan.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_createClanFragment)
        }

        binding.cardBattlePass.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_battlePassFragment)
        }

        // Observe Shared user profile
        collectLatestFlow(mainViewModel.currentUser) { user ->
            user?.let {
                binding.tvProfileName.text = it.name
                val rank = it.getRankFromCoins()
                binding.tvProfileRank.text = "⚔ $rank — Top 3.2% Global"
                val rankColor = when (rank) {
                    "BRONZE"   -> android.graphics.Color.parseColor("#CD7F32")
                    "PRATA"    -> android.graphics.Color.parseColor("#C0C0C0")
                    "OURO"     -> android.graphics.Color.parseColor("#FFD700")
                    "PLATINA"  -> android.graphics.Color.parseColor("#00BFFF")
                    "DIAMANTE" -> android.graphics.Color.parseColor("#8A2BE2")
                    "LENDÁRIO" -> android.graphics.Color.parseColor("#FFD700")
                    else       -> resources.getColor(R.color.azul_neon, null)
                }
                binding.tvProfileRank.setTextColor(rankColor)
                binding.tvProfileDescription.text = it.description ?: "Sua biografia aparecerá aqui."
                
                // Bind level, battle pass and XP progress
                binding.tvProfileLevel.text = "NÍVEL ${it.nivel_atual}"
                val xpInCurrentLevel = it.xp_total % 100
                binding.progressBattlePass.progress = xpInCurrentLevel
                binding.tvProfileXp.text = "$xpInCurrentLevel / 100 XP"

                // Handle premium status UI
                if (it.is_premium) {
                    binding.tvPremiumBadge.visibility = View.VISIBLE
                    binding.btnUpgradePremium.visibility = View.GONE
                } else {
                    binding.tvPremiumBadge.visibility = View.GONE
                    binding.btnUpgradePremium.visibility = View.VISIBLE
                }

                // Handle clan status UI
                if (it.clan_id != null && it.clan_id > 0) {
                    binding.layoutUserNoClan.visibility = View.GONE
                    binding.layoutUserClanDetails.visibility = View.VISIBLE
                    binding.tvProfileClanName.text = "Membro do Clã (ID: ${it.clan_id})"
                } else {
                    binding.layoutUserNoClan.visibility = View.VISIBLE
                    binding.layoutUserClanDetails.visibility = View.GONE
                }

                // Apply neon border based on moldura_neon
                val density = resources.displayMetrics.density
                if (!it.moldura_neon.isNullOrEmpty()) {
                    val borderColor = when (it.moldura_neon) {
                        "neon_ciano" -> "#00BFFF"
                        "neon_roxo" -> "#8A2BE2"
                        "ouro_lendario" -> "#FFD700"
                        else -> null
                    }
                    if (borderColor != null) {
                        binding.cardProfileAvatarWrapper.setCardBackgroundColor(android.graphics.Color.parseColor(borderColor))
                        val borderPadding = (4 * density).toInt()
                        binding.imgProfileAvatar.setPadding(borderPadding, borderPadding, borderPadding, borderPadding)
                    } else {
                        binding.cardProfileAvatarWrapper.setCardBackgroundColor(resources.getColor(R.color.bg_secundario, null))
                    }
                } else {
                    binding.cardProfileAvatarWrapper.setCardBackgroundColor(resources.getColor(R.color.bg_secundario, null))
                }

                // Dynamic image loading via Coil
                val absoluteAvatarUrl = it.getAbsoluteImageUrl()
                if (!absoluteAvatarUrl.isNullOrEmpty()) {
                    binding.imgProfileAvatar.imageTintList = null
                    binding.imgProfileAvatar.colorFilter = null
                    if (it.moldura_neon.isNullOrEmpty()) {
                        binding.imgProfileAvatar.setPadding(0, 0, 0, 0)
                    }
                    binding.imgProfileAvatar.load(absoluteAvatarUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile)
                        error(R.drawable.ic_profile)
                        memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        diskCachePolicy(coil.request.CachePolicy.DISABLED)
                    }
                } else {
                    val paddingPx = if (!it.moldura_neon.isNullOrEmpty()) (10 * density).toInt() else (8 * density).toInt()
                    binding.imgProfileAvatar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                    binding.imgProfileAvatar.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.azul_neon, null))
                    binding.imgProfileAvatar.setImageResource(R.drawable.ic_profile)
                }
            }
        }

        // Observe Premium Upgrade State
        collectLatestFlow(mainViewModel.premiumState) { state ->
            when (state) {
                is com.example.questgen.viewmodel.PremiumState.Loading -> {
                    // loading state
                }
                is com.example.questgen.viewmodel.PremiumState.Success -> {
                    toast(state.message)
                    mainViewModel.resetPremiumState()
                }
                is com.example.questgen.viewmodel.PremiumState.Error -> {
                    toast(state.message)
                    mainViewModel.resetPremiumState()
                }
                else -> {}
            }
        }

        // Observe Create Clan State
        collectLatestFlow(mainViewModel.createClanState) { state ->
            when (state) {
                is com.example.questgen.viewmodel.CreateClanState.Loading -> {
                    // loading state
                }
                is com.example.questgen.viewmodel.CreateClanState.Success -> {
                    toast(state.message)
                    mainViewModel.resetCreateClanState()
                }
                is com.example.questgen.viewmodel.CreateClanState.Error -> {
                    toast(state.message)
                    mainViewModel.resetCreateClanState()
                }
                else -> {}
            }
        }

        // Observe Profile Edit State
        collectLatestFlow(mainViewModel.editProfileState) { state ->
            when (state) {
                is EditProfileState.Loading -> {
                    // Optional progress indicators
                }
                is EditProfileState.Success -> {
                    toast(state.message)
                    mainViewModel.resetEditProfileState()
                }
                is EditProfileState.Error -> {
                    toast(state.message)
                    mainViewModel.resetEditProfileState()
                }
                else -> {}
            }
        }

        // Observe Delete Account State
        collectLatestFlow(mainViewModel.deleteAccountState) { state ->
            when (state) {
                is DeleteAccountState.Loading -> {
                    // Optional progress indicators
                }
                is DeleteAccountState.Success -> {
                    toast(state.message)
                    mainViewModel.resetDeleteAccountState()
                }
                is DeleteAccountState.Error -> {
                    toast(state.message)
                    mainViewModel.resetDeleteAccountState()
                }
                else -> {}
            }
        }

    }

    private fun showCriarClanDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etNome = EditText(context).apply {
            hint = "Nome do Clã (ex: Alpha Esports)"
            maxLines = 1
        }

        val etTag = EditText(context).apply {
            hint = "Tag do Clã (ex: ALP)"
            maxLines = 1
        }

        layout.addView(etNome)
        layout.addView(etTag)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle("CRIAR CLÃ")
            .setMessage("Criar um clã custa 5.000 GC (Grátis para jogadores Elite Premium).")
            .setView(layout)
            .setPositiveButton("CRIAR") { _, _ ->
                val nome = etNome.text.toString().trim()
                val tag = etTag.text.toString().trim()
                if (nome.isNotEmpty() && tag.isNotEmpty()) {
                    if (tag.length > 4) {
                        toast("A tag deve ter no máximo 4 caracteres")
                    } else {
                        mainViewModel.criarClan(nome, tag, "")
                    }
                } else {
                    toast("Nome e Tag não podem ser vazios")
                }
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
