package com.example.questgen.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.questgen.R
import com.example.questgen.databinding.FragmentGameSelectionBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.GameViewModel
import com.example.questgen.viewmodel.GamesState
import com.example.questgen.viewmodel.MainViewModel

class GameSelectionFragment : Fragment() {

    private var _binding: FragmentGameSelectionBinding? = null
    private val binding get() = _binding!!

    private val gameViewModel: GameViewModel by viewModels()
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: GameAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSuggestGame.setOnClickListener {
            toast("Sugestão registrada para análise do protocolo!")
        }

        // Setup adapter
        adapter = GameAdapter(emptyList()) { game ->
            // Pass selected game info to challenge generator details
            val bundle = bundleOf(
                "game_id" to game.id,
                "game_title" to game.title
            )
            findNavController().navigate(R.id.action_gameSelectionFragment_to_challengeDetailFragment, bundle)
        }

        binding.rvGames.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGames.adapter = adapter

        // Setup retry click
        binding.btnGamesRetry.setOnClickListener {
            gameViewModel.fetchGames()
        }

        // Observe games loading / content states
        collectLatestFlow(gameViewModel.gamesState) { state ->
            when (state) {
                is GamesState.Loading -> {
                    binding.progressGamesLoading.visibility = View.VISIBLE
                    binding.layoutGamesError.visibility = View.GONE
                    binding.rvGames.visibility = View.GONE
                }
                is GamesState.Success -> {
                    binding.progressGamesLoading.visibility = View.GONE
                    binding.layoutGamesError.visibility = View.GONE
                    binding.rvGames.visibility = View.VISIBLE
                    adapter.updateData(state.list)
                }
                is GamesState.Error -> {
                    binding.progressGamesLoading.visibility = View.GONE
                    binding.layoutGamesError.visibility = View.VISIBLE
                    binding.rvGames.visibility = View.GONE
                    binding.tvErrorMsg.text = state.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
