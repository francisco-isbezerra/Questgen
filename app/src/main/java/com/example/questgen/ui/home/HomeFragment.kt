package com.example.questgen.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.questgen.R
import com.example.questgen.data.model.Challenge
import com.example.questgen.databinding.FragmentHomeBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.formatCoins
import com.example.questgen.util.showDialog
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.ActiveChallengeState
import com.example.questgen.viewmodel.ChallengeViewModel
import com.example.questgen.viewmodel.MainViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }
    private val challengeViewModel: ChallengeViewModel by viewModels()

    private lateinit var pendingAdapter: PendingChallengesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup pending challenges RecyclerView
        pendingAdapter = PendingChallengesAdapter(emptyList())
        binding.rvPendingChallenges.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingChallenges.adapter = pendingAdapter

        // Setup navigations
        binding.cardGenerateButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_gameSelectionFragment)
        }

        binding.btnQuickShop.setOnClickListener {
            findNavController().navigate(R.id.shopFragment)
        }

        binding.btnQuickRanking.setOnClickListener {
            findNavController().navigate(R.id.rankingFragment)
        }

        // Observe Shared user session
        collectLatestFlow(mainViewModel.currentUser) { user ->
            user?.let {
                binding.tvUsername.text = it.name
                val rank = it.getRankFromCoins()
                binding.tvRankBadge.text = "⚔ $rank"
                val rankColor = when (rank) {
                    "BRONZE"   -> android.graphics.Color.parseColor("#CD7F32")
                    "PRATA"    -> android.graphics.Color.parseColor("#C0C0C0")
                    "OURO"     -> android.graphics.Color.parseColor("#FFD700")
                    "PLATINA"  -> android.graphics.Color.parseColor("#00BFFF")
                    "DIAMANTE" -> android.graphics.Color.parseColor("#8A2BE2")
                    "LENDÁRIO" -> android.graphics.Color.parseColor("#FFD700")
                    else       -> resources.getColor(R.color.azul_neon, null)
                }
                binding.tvRankBadge.setTextColor(rankColor)
                binding.tvCoins.text = it.game_coins.formatCoins()

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
                        binding.cardAvatar.setCardBackgroundColor(android.graphics.Color.parseColor(borderColor))
                        val borderPadding = (3 * density).toInt()
                        binding.imgAvatar.setPadding(borderPadding, borderPadding, borderPadding, borderPadding)
                    } else {
                        binding.cardAvatar.setCardBackgroundColor(resources.getColor(R.color.bg_secundario, null))
                    }
                } else {
                    binding.cardAvatar.setCardBackgroundColor(resources.getColor(R.color.bg_secundario, null))
                }

                // Dynamic image loading via Coil
                val absoluteAvatarUrl = it.getAbsoluteImageUrl()
                if (!absoluteAvatarUrl.isNullOrEmpty()) {
                    binding.imgAvatar.imageTintList = null
                    binding.imgAvatar.colorFilter = null
                    if (it.moldura_neon.isNullOrEmpty()) {
                        binding.imgAvatar.setPadding(0, 0, 0, 0)
                    }
                    binding.imgAvatar.load(absoluteAvatarUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile)
                        error(R.drawable.ic_profile)
                        memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        diskCachePolicy(coil.request.CachePolicy.DISABLED)
                    }
                } else {
                    val paddingPx = if (!it.moldura_neon.isNullOrEmpty()) (5 * density).toInt() else (4 * density).toInt()
                    binding.imgAvatar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                    binding.imgAvatar.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.azul_neon, null))
                    binding.imgAvatar.setImageResource(R.drawable.ic_profile)
                }

                // Fetch active challenge for this user
                challengeViewModel.fetchActiveChallenge(it.id)
            }
        }

        // Observe active challenge state
        collectLatestFlow(challengeViewModel.activeChallengeState) { state ->
            when (state) {
                is ActiveChallengeState.Loading -> {
                    binding.progressHomeLoad.visibility = View.VISIBLE
                }
                is ActiveChallengeState.Success -> {
                    binding.progressHomeLoad.visibility = View.GONE
                    
                    val activeChallenge = state.activeChallenge
                    if (activeChallenge != null) {
                        showActiveChallenge(activeChallenge)
                    } else {
                        showEmptyState()
                    }

                    val pendingList = state.pendingChallenges
                    if (pendingList.isNotEmpty()) {
                        binding.tvPendingTitle.visibility = View.VISIBLE
                        binding.rvPendingChallenges.visibility = View.VISIBLE
                        pendingAdapter.updateData(pendingList)
                    } else {
                        binding.tvPendingTitle.visibility = View.GONE
                        binding.rvPendingChallenges.visibility = View.GONE
                    }
                }
                is ActiveChallengeState.AutoCompleted -> {
                    binding.progressHomeLoad.visibility = View.GONE
                    mainViewModel.updateUser(state.updatedUser)
                    toast(state.message)
                    challengeViewModel.clearActiveChallengeState()
                    val user = mainViewModel.currentUser.value
                    if (user != null) {
                        challengeViewModel.fetchActiveChallenge(user.id)
                    }
                }
                is ActiveChallengeState.Expired -> {
                    binding.progressHomeLoad.visibility = View.GONE
                    showDialog("TEMPO ESGOTADO!", state.message)

                    // Update shared session and profile history log
                    mainViewModel.updateUser(state.updatedUser)
                    mainViewModel.addHistoryEntry("Tempo Esgotado", "Falhou e perdeu 50 GC")
                    challengeViewModel.clearActiveChallengeState()
                }
                is ActiveChallengeState.Error -> {
                    binding.progressHomeLoad.visibility = View.GONE
                    showEmptyState() // Fallback
                    binding.tvPendingTitle.visibility = View.GONE
                    binding.rvPendingChallenges.visibility = View.GONE
                }
            }
        }

        // Collect countdown timer flow reactively
        collectLatestFlow(challengeViewModel.timerString) { timeStr ->
            binding.tvChallengeTimer.text = timeStr
        }

        // Collect countdown progress flow dynamically
        collectLatestFlow(challengeViewModel.timerProgress) { progress ->
            binding.progressChallenge.progress = progress
            binding.tvProgressPercent.text = "Tempo Restante: $progress%"
        }

        // Observe validation notification event to refresh lists in real-time
        collectLatestFlow(mainViewModel.challengeNotificationEvent) {
            val user = mainViewModel.currentUser.value
            if (user != null) {
                challengeViewModel.fetchActiveChallenge(user.id)
            }
        }
    }

    private fun showActiveChallenge(challenge: Challenge) {
        binding.cardNoChallenge.visibility = View.GONE
        binding.cardActiveChallenge.visibility = View.VISIBLE
        binding.tvChallengeTitle.text = challenge.title
        binding.tvChallengeDesc.text = challenge.description
        binding.tvReward.text = "+${challenge.reward_amount.formatCoins()}"
        binding.tvChallengeRarity.text = challenge.rarity

        // Style rarity borders
        when (challenge.rarity) {
            "LENDÁRIO" -> {
                binding.tvChallengeRarity.setTextColor(resources.getColor(R.color.raridade_lendario, null))
                binding.tvChallengeRarity.setBackgroundColor(resources.getColor(R.color.bg_principal, null))
            }
            "RARO" -> {
                binding.tvChallengeRarity.setTextColor(resources.getColor(R.color.raridade_raro, null))
                binding.tvChallengeRarity.setBackgroundColor(resources.getColor(R.color.bg_principal, null))
            }
            else -> {
                binding.tvChallengeRarity.setTextColor(resources.getColor(R.color.raridade_comum, null))
                binding.tvChallengeRarity.setBackgroundColor(resources.getColor(R.color.bg_principal, null))
            }
        }

        if (challenge.status == "PENDING_VALIDATION") {
            binding.tvChallengeTimer.text = "PENDENTE"
            binding.tvChallengeTimer.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            binding.progressChallenge.visibility = View.GONE
            binding.tvProgressPercent.text = "Aguardando validação"
        } else {
            binding.tvChallengeTimer.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            binding.progressChallenge.visibility = View.VISIBLE
        }

        // Open details on click
        binding.cardActiveChallenge.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_challengeDetailFragment)
        }
    }

    private fun showEmptyState() {
        binding.cardActiveChallenge.visibility = View.GONE
        binding.cardNoChallenge.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Explicitly cancel countdown timer to mitigate memory leaks/CPU load
        challengeViewModel.stopCountdown()
        _binding = null
    }
}
