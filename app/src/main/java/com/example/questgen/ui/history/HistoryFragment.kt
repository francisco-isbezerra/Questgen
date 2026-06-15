package com.example.questgen.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.questgen.databinding.FragmentHistoryBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.ChallengeViewModel
import com.example.questgen.viewmodel.MainViewModel

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val challengeViewModel: ChallengeViewModel by viewModels()
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup back navigation
        binding.btnBackHistory.setOnClickListener {
            findNavController().popBackStack()
        }

        // Setup adapter
        adapter = HistoryAdapter(emptyList())
        binding.rvHistoryList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistoryList.adapter = adapter

        // Fetch history data for current user
        val user = mainViewModel.currentUser.value
        if (user != null) {
            challengeViewModel.fetchHistory(user.id)
        } else {
            toast("Sessão expirada. Refaça login.")
        }

        // Observe history state flow
        collectLatestFlow(challengeViewModel.historyState) { historyList ->
            adapter.updateData(historyList)
            if (historyList.isEmpty()) {
                binding.tvHistoryEmptyState.visibility = View.VISIBLE
            } else {
                binding.tvHistoryEmptyState.visibility = View.GONE
            }
        }

        // Observe validation notification event to refresh history in real-time
        collectLatestFlow(mainViewModel.challengeNotificationEvent) {
            val user = mainViewModel.currentUser.value
            if (user != null) {
                challengeViewModel.fetchHistory(user.id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
