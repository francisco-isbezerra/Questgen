package com.example.questgen.ui.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.questgen.R
import com.example.questgen.data.model.Challenge
import com.example.questgen.databinding.FragmentChallengeDetailBinding
import com.example.questgen.viewmodel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChallengeDetailFragment : Fragment() {

    private var _binding: FragmentChallengeDetailBinding? = null
    private val binding get() = _binding!!

    private val challengeViewModel: ChallengeViewModel by viewModels()
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private var currentChallenge: Challenge? = null
    private var userId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get user session details
        val user = mainViewModel.currentUser.value
        if (user == null) {
            Toast.makeText(requireContext(), "Sessão expirada. Refaça login", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        userId = user.id

        // Check argument triggers
        val gameId = arguments?.getInt("game_id", -1) ?: -1
        val gameTitle = arguments?.getString("game_title", "") ?: ""

        if (gameId != -1) {
            // Came from game selection: generate a new challenge for this game
            binding.tvChallengeDetailTitle.text = "GERANDO DESAFIO DE ${gameTitle.uppercase()}"
            challengeViewModel.generateChallenge(userId, gameId)
        } else {
            // Came from active challenge card: fetch it
            binding.tvChallengeDetailTitle.text = "CARREGANDO DESAFIO ATIVO..."
            challengeViewModel.fetchActiveChallenge(userId)
        }

        // Setup actions click listeners
        binding.btnAccept.setOnClickListener {
            currentChallenge?.let {
                challengeViewModel.acceptChallenge(userId, it.id)
            }
        }

        binding.btnClaim.setOnClickListener {
            currentChallenge?.let {
                challengeViewModel.claimReward(userId, it.id)
            }
        }

        binding.btnForfeit.setOnClickListener {
            currentChallenge?.let {
                challengeViewModel.giveUpChallenge(userId, it.id)
            }
        }

        // Observe generated challenge
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.generateChallengeState.collectLatest { state ->
                when (state) {
                    is GenerateChallengeState.Loading -> {
                        binding.progressChallengeAction.visibility = View.VISIBLE
                    }
                    is GenerateChallengeState.Success -> {
                        binding.progressChallengeAction.visibility = View.GONE
                        bindChallengeData(state.challenge)
                    }
                    is GenerateChallengeState.Error -> {
                        binding.progressChallengeAction.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }
                    else -> {}
                }
            }
        }

        // Observe active challenge state fetch
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.activeChallengeState.collectLatest { state ->
                when (state) {
                    is ActiveChallengeState.Loading -> {
                        binding.progressChallengeAction.visibility = View.VISIBLE
                    }
                    is ActiveChallengeState.Success -> {
                        binding.progressChallengeAction.visibility = View.GONE
                        state.challenge?.let {
                            bindChallengeData(it)
                        } ?: run {
                            // If no active challenge but we opened without generator arguments, fallback
                            if (gameId == -1) {
                                Toast.makeText(requireContext(), "Nenhum desafio ativo encontrado", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                        }
                    }
                    is ActiveChallengeState.Error -> {
                        binding.progressChallengeAction.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }

        // Observe countdown timer flow reactively
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.timerString.collectLatest { timeStr ->
                binding.tvTimer.text = timeStr
            }
        }

        // Observe transaction actions state (accept, claim, forfeit)
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.actionState.collectLatest { state ->
                when (state) {
                    is ChallengeActionState.Loading -> {
                        binding.progressChallengeAction.visibility = View.VISIBLE
                        setButtonsEnabled(false)
                    }
                    is ChallengeActionState.Success -> {
                        binding.progressChallengeAction.visibility = View.GONE
                        setButtonsEnabled(true)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()

                        // If user balance was updated returned by backend (on Claim or Forfeit)
                        state.updatedUser?.let {
                            mainViewModel.updateUser(it)
                            // Append to profile history log
                            currentChallenge?.let { challenge ->
                                mainViewModel.addHistoryEntry(challenge.title, "+${challenge.reward_amount} GC, Resgatado agora")
                            }
                            // Return back
                            findNavController().popBackStack()
                        } ?: run {
                            // If accepted, state changes to ACTIVE
                            challengeViewModel.fetchActiveChallenge(userId)
                        }
                        challengeViewModel.resetActionState()
                    }
                    is ChallengeActionState.Error -> {
                        binding.progressChallengeAction.visibility = View.GONE
                        setButtonsEnabled(true)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        challengeViewModel.resetActionState()
                    }
                    else -> {}
                }
            }
        }
        // Observe game challenges list to dynamically populate Next Expeditions list
        viewLifecycleOwner.lifecycleScope.launch {
            challengeViewModel.gameChallenges.collectLatest { challenges ->
                val current = currentChallenge
                if (current != null && challenges.isNotEmpty()) {
                    val nextList = challengeViewModel.getNextChallenges(current, challenges)
                    if (nextList.isNotEmpty()) {
                        val sb = StringBuilder()
                        nextList.forEach { next ->
                            sb.append("• ${next.title} (+${next.reward_amount} GC)\n")
                        }
                        binding.tvNextExpeditionsList.text = sb.toString().trim()
                    } else {
                        binding.tvNextExpeditionsList.text = "Nenhuma expedição futura disponível"
                    }
                } else {
                    binding.tvNextExpeditionsList.text = "Nenhuma expedição futura disponível"
                }
            }
        }
    }

    private fun bindChallengeData(challenge: Challenge) {
        currentChallenge = challenge
        binding.tvChallengeDetailTitle.text = challenge.title.uppercase()
        binding.tvDetailDesc.text = challenge.description
        binding.tvDetailReward.text = "+${challenge.reward_amount} GameCoins"
        binding.tvDetailRarity.text = challenge.rarity
        binding.tvDifficultyText.text = "DIFICULDADE: ${challenge.difficulty_level}/5"

        // Set card rarity border backgrounds
        when (challenge.rarity) {
            "LENDÁRIO" -> {
                binding.layoutChallengeBorder.setBackgroundResource(R.drawable.bg_card_lendario)
                binding.tvDetailRarity.setTextColor(resources.getColor(R.color.raridade_lendario, null))
            }
            "RARO" -> {
                binding.layoutChallengeBorder.setBackgroundResource(R.drawable.bg_card_raro)
                binding.tvDetailRarity.setTextColor(resources.getColor(R.color.raridade_raro, null))
            }
            else -> {
                binding.layoutChallengeBorder.setBackgroundResource(R.drawable.bg_card_comum)
                binding.tvDetailRarity.setTextColor(resources.getColor(R.color.raridade_comum, null))
            }
        }

        // Toggle action buttons visibility based on acceptance status
        if (challenge.status == "ACTIVE") {
            binding.btnAccept.visibility = View.GONE
            binding.btnClaim.visibility = View.VISIBLE
            binding.btnForfeit.visibility = View.VISIBLE
        } else {
            binding.btnAccept.visibility = View.VISIBLE
            binding.btnClaim.visibility = View.GONE
            binding.btnForfeit.visibility = View.GONE
        }

        // Fetch actual game challenges from backend dynamically
        challengeViewModel.fetchChallengesByGame(challenge.game_id)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnAccept.isEnabled = enabled
        binding.btnClaim.isEnabled = enabled
        binding.btnForfeit.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Explicitly cancel countdown timer on view destruction to mitigate memory leaks
        challengeViewModel.stopCountdown()
        _binding = null
    }
}
