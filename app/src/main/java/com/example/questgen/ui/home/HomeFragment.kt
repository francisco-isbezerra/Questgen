package com.example.questgen.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.Challenge
import com.example.questgen.databinding.FragmentHomeBinding
import com.example.questgen.viewmodel.ActiveChallengeState
import com.example.questgen.viewmodel.ChallengeViewModel
import com.example.questgen.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }
    private val challengeViewModel: ChallengeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.currentUser.collectLatest { user ->
                user?.let {
                    binding.tvUsername.text = it.name
                    binding.tvCoins.text = "${it.game_coins} GC"

                    // Dynamic image loading via Coil
                    if (!it.image_url.isNullOrEmpty()) {
                        binding.imgAvatar.imageTintList = null
                        binding.imgAvatar.colorFilter = null
                        binding.imgAvatar.setPadding(0, 0, 0, 0)
                        binding.imgAvatar.load(it.image_url) {
                            crossfade(true)
                            placeholder(R.drawable.ic_profile)
                            error(R.drawable.ic_profile)
                        }
                    } else {
                        val paddingPx = (4 * resources.displayMetrics.density).toInt()
                        binding.imgAvatar.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                        binding.imgAvatar.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.azul_neon, null))
                        binding.imgAvatar.setImageResource(R.drawable.ic_profile)
                    }

                    // Fetch active challenge for this user
                    challengeViewModel.fetchActiveChallenge(it.id)
                }
            }
        }

        // Observe active challenge state
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.activeChallengeState.collectLatest { state ->
                when (state) {
                    is ActiveChallengeState.Loading -> {
                        binding.progressHomeLoad.visibility = View.VISIBLE
                    }
                    is ActiveChallengeState.Success -> {
                        binding.progressHomeLoad.visibility = View.GONE
                        val challenge = state.challenge
                        if (challenge != null) {
                            showActiveChallenge(challenge)
                        } else {
                            showEmptyState()
                        }
                    }
                    is ActiveChallengeState.Expired -> {
                        binding.progressHomeLoad.visibility = View.GONE
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("TEMPO ESGOTADO!")
                            .setMessage(state.message)
                            .setPositiveButton("OK", null)
                            .show()

                        // Update shared session and profile history log
                        mainViewModel.updateUser(state.updatedUser)
                        mainViewModel.addHistoryEntry("Tempo Esgotado", "Falhou e perdeu 50 GC")
                        challengeViewModel.clearActiveChallengeState()
                    }
                    is ActiveChallengeState.Error -> {
                        binding.progressHomeLoad.visibility = View.GONE
                        showEmptyState() // Fallback
                    }
                }
            }
        }

        // Collect countdown timer flow reactively
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.timerString.collectLatest { timeStr ->
                binding.tvChallengeTimer.text = timeStr
            }
        }

        // Collect countdown progress flow dynamically
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.timerProgress.collectLatest { progress ->
                binding.progressChallenge.progress = progress
                binding.tvProgressPercent.text = "Tempo Restante: $progress%"
            }
        }
    }

    private fun showActiveChallenge(challenge: Challenge) {
        binding.cardNoChallenge.visibility = View.GONE
        binding.cardActiveChallenge.visibility = View.VISIBLE
        binding.tvChallengeTitle.text = challenge.title
        binding.tvChallengeDesc.text = challenge.description
        binding.tvReward.text = "+${challenge.reward_amount} GC"
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
