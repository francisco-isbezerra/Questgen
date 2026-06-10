package com.example.questgen.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.questgen.databinding.FragmentProfileBinding
import com.example.questgen.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter
        adapter = HistoryAdapter(emptyList())
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        // Setup logout button
        binding.btnLogout.setOnClickListener {
            mainViewModel.logout()
        }

        // Observe Shared user profile
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.currentUser.collectLatest { user ->
                user?.let {
                    binding.tvProfileName.text = it.name
                    binding.tvProfileRank.text = "${it.rank} - Top 3.2% Global"
                }
            }
        }

        // Observe local challenge history log
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.challengeHistory.collectLatest { historyList ->
                adapter.updateData(historyList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
