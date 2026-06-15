package com.example.questgen.ui.challenge

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.questgen.R
import com.example.questgen.data.model.Challenge
import com.example.questgen.databinding.FragmentChallengeDetailBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.formatCoins
import com.example.questgen.util.showDialog
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.*
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
    private var selectedImageUri: Uri? = null
    private var pollingJob: kotlinx.coroutines.Job? = null

    private fun startPollingActiveChallenge() {
        stopPollingActiveChallenge()
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000)
                challengeViewModel.fetchActiveChallenge(userId)
            }
        }
    }

    private fun stopPollingActiveChallenge() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.cardProofPreview.visibility = View.VISIBLE
            binding.imgProofPreview.setImageURI(uri)
            binding.btnClaim.text = "CARREGAR COMPROVANTE"
        } else {
            toast("Nenhuma imagem selecionada.")
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

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
            toast("Sessão expirada. Refaça login")
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
            val challenge = currentChallenge
            if (challenge != null) {
                val uri = selectedImageUri
                if (uri == null) {
                    openGallery()
                } else {
                    challengeViewModel.uploadComprovante(userId, challenge.id, uri)
                }
            }
        }

        binding.btnForfeit.setOnClickListener {
            currentChallenge?.let {
                challengeViewModel.giveUpChallenge(userId, it.id)
            }
        }

        binding.btnApproveModerator.setOnClickListener {
            currentChallenge?.let {
                challengeViewModel.approveChallenge(userId, it.id)
            }
        }

        // Observe generated challenge
        collectLatestFlow(challengeViewModel.generateChallengeState) { state ->
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
                    toast(state.message)
                    findNavController().popBackStack()
                }
                else -> {}
            }
        }

        // Observe active challenge state fetch
        collectLatestFlow(challengeViewModel.activeChallengeState) { state ->
            when (state) {
                is ActiveChallengeState.Loading -> {
                    binding.progressChallengeAction.visibility = View.VISIBLE
                }
                is ActiveChallengeState.Success -> {
                    binding.progressChallengeAction.visibility = View.GONE
                    state.activeChallenge?.let {
                        bindChallengeData(it)
                    } ?: run {
                        // If no active challenge but we opened without generator arguments, fallback
                        if (gameId == -1) {
                            toast("Nenhum desafio ativo encontrado")
                            findNavController().popBackStack()
                        }
                    }
                }
                is ActiveChallengeState.AutoCompleted -> {
                    binding.progressChallengeAction.visibility = View.GONE
                    stopPollingActiveChallenge()
                    toast(state.message)
                    mainViewModel.updateUser(state.updatedUser)
                    currentChallenge?.let { challenge ->
                        mainViewModel.addHistoryEntry(challenge.title, "+${challenge.reward_amount.formatCoins()}, Aprovado automaticamente")
                    }
                    findNavController().popBackStack()
                }
                is ActiveChallengeState.Expired -> {
                    binding.progressChallengeAction.visibility = View.GONE
                    stopPollingActiveChallenge()
                    showDialog("TEMPO ESGOTADO!", state.message)

                    mainViewModel.updateUser(state.updatedUser)
                    mainViewModel.addHistoryEntry("Tempo Esgotado", "Falhou e perdeu 50 GC")
                    challengeViewModel.clearActiveChallengeState()
                    findNavController().popBackStack()
                }
                is ActiveChallengeState.Error -> {
                    binding.progressChallengeAction.visibility = View.GONE
                    stopPollingActiveChallenge()
                    toast(state.message)
                    findNavController().popBackStack()
                }
            }
        }

        // Observe countdown timer flow reactively
        collectLatestFlow(challengeViewModel.timerString) { timeStr ->
            binding.tvTimer.text = timeStr
        }

        // Observe countdown progress flow dynamically
        collectLatestFlow(challengeViewModel.timerProgress) { progress ->
            binding.progressChallengeDetail.progress = progress
            binding.tvProgressPercentDetail.text = "Tempo Restante: $progress%"
        }

        // Observe transaction actions state (accept, claim, forfeit)
        collectLatestFlow(challengeViewModel.actionState) { state ->
            when (state) {
                is ChallengeActionState.Loading -> {
                    binding.progressChallengeAction.visibility = View.VISIBLE
                    setButtonsEnabled(false)
                }
                is ChallengeActionState.Success -> {
                    binding.progressChallengeAction.visibility = View.GONE
                    setButtonsEnabled(true)
                    toast(state.message)

                    // If user balance was updated returned by backend (on Claim or Forfeit)
                    state.updatedUser?.let {
                        mainViewModel.updateUser(it)
                        // Append to profile history log
                        currentChallenge?.let { challenge ->
                            mainViewModel.addHistoryEntry(challenge.title, "+${challenge.reward_amount.formatCoins()}, Resgatado agora")
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
                    toast(state.message)
                    challengeViewModel.resetActionState()
                }
                else -> {}
            }
        }

        // Observe game challenges list to dynamically populate Next Expeditions list
        collectLatestFlow(challengeViewModel.gameChallenges) { challenges ->
            val current = currentChallenge
            if (current != null && challenges.isNotEmpty()) {
                val nextList = challengeViewModel.getNextChallenges(current, challenges)
                if (nextList.isNotEmpty()) {
                    val sb = StringBuilder()
                    nextList.forEach { next ->
                        sb.append("• ${next.title} (+${next.reward_amount.formatCoins()})\n")
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

    private fun bindChallengeData(challenge: Challenge) {
        currentChallenge = challenge
        binding.tvChallengeDetailTitle.text = challenge.title.uppercase()
        binding.tvDetailDesc.text = challenge.description
        binding.tvDetailReward.text = "+${challenge.reward_amount.formatCoins()}"
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

        // Reset upload preview/state when loading new challenge details
        selectedImageUri = null
        binding.cardProofPreview.visibility = View.GONE
        binding.btnClaim.text = "SELECIONAR COMPROVANTE"

        // Toggle action buttons visibility based on status
        when (challenge.status) {
            "ACTIVE" -> {
                stopPollingActiveChallenge()
                binding.btnAccept.visibility = View.GONE
                binding.btnClaim.visibility = View.VISIBLE
                binding.btnForfeit.visibility = View.VISIBLE
                binding.progressChallengeDetail.visibility = View.VISIBLE
                binding.tvProgressPercentDetail.visibility = View.VISIBLE
                binding.layoutPendingValidation.visibility = View.GONE
            }
            "PENDING_VALIDATION" -> {
                binding.btnAccept.visibility = View.GONE
                binding.btnClaim.visibility = View.GONE
                binding.btnForfeit.visibility = View.GONE
                binding.progressChallengeDetail.visibility = View.GONE
                binding.tvProgressPercentDetail.visibility = View.GONE
                binding.layoutPendingValidation.visibility = View.VISIBLE
                binding.tvTimer.text = "PENDENTE"
                binding.tvTimer.setTextColor(android.graphics.Color.parseColor("#FFD700"))
                startPollingActiveChallenge()
            }
            else -> {
                stopPollingActiveChallenge()
                binding.btnAccept.visibility = View.VISIBLE
                binding.btnClaim.visibility = View.GONE
                binding.btnForfeit.visibility = View.GONE
                binding.progressChallengeDetail.visibility = View.GONE
                binding.tvProgressPercentDetail.visibility = View.GONE
                binding.layoutPendingValidation.visibility = View.GONE

                // Set static display of total time for available challenge
                val totalSec = challenge.tempo_total_segundos ?: challenge.tempo_restante_segundos ?: 0
                binding.tvTimer.text = challengeViewModel.formatTime(totalSec)
                binding.tvTimer.setTextColor(android.graphics.Color.parseColor("#FFD700"))
            }
        }

        // Fetch actual game challenges from backend dynamically
        challengeViewModel.fetchChallengesByGame(challenge.game_id)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnAccept.isEnabled = enabled
        binding.btnClaim.isEnabled = enabled
        binding.btnForfeit.isEnabled = enabled
        binding.btnApproveModerator.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPollingActiveChallenge()
        // Explicitly cancel countdown timer on view destruction to mitigate memory leaks
        challengeViewModel.stopCountdown()
        _binding = null
    }
}
