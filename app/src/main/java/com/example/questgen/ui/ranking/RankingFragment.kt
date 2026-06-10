package com.example.questgen.ui.ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.LeaderboardRow
import com.example.questgen.databinding.FragmentRankingBinding
import com.example.questgen.viewmodel.MainViewModel
import com.example.questgen.viewmodel.RankingState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: LeaderboardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter
        adapter = LeaderboardAdapter(emptyList())
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter

        // Toggle clicks
        binding.btnScopeGlobal.setOnClickListener {
            toggleScope(isGlobal = true)
        }

        binding.btnScopeFriends.setOnClickListener {
            toggleScope(isGlobal = false)
        }

        // Retry click
        binding.btnRankingRetry.setOnClickListener {
            mainViewModel.fetchRanking()
        }

        // Observe Ranking state
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.rankingState.collectLatest { state ->
                when (state) {
                    is RankingState.Loading -> {
                        binding.progressRankingLoading.visibility = View.VISIBLE
                        binding.layoutRankingError.visibility = View.GONE
                        binding.rvLeaderboard.visibility = View.GONE
                        binding.layoutPodium.visibility = View.INVISIBLE
                    }
                    is RankingState.Success -> {
                        binding.progressRankingLoading.visibility = View.GONE
                        binding.layoutRankingError.visibility = View.GONE
                        binding.rvLeaderboard.visibility = View.VISIBLE
                        binding.layoutPodium.visibility = View.VISIBLE
                        
                        populateLeaderboard(state.list)
                    }
                    is RankingState.Error -> {
                        binding.progressRankingLoading.visibility = View.GONE
                        binding.layoutRankingError.visibility = View.VISIBLE
                        binding.rvLeaderboard.visibility = View.GONE
                        binding.layoutPodium.visibility = View.INVISIBLE
                        binding.tvRankingErrorMsg.text = state.message
                    }
                }
            }
        }
    }

    private fun toggleScope(isGlobal: Boolean) {
        if (isGlobal) {
            binding.btnScopeGlobal.setTextColor(resources.getColor(R.color.bg_principal, null))
            binding.btnScopeGlobal.setBackgroundResource(R.drawable.bg_button_neon)
            binding.btnScopeFriends.setTextColor(resources.getColor(R.color.gray_text, null))
            binding.btnScopeFriends.background = null
            mainViewModel.fetchRanking()
        } else {
            binding.btnScopeFriends.setTextColor(resources.getColor(R.color.bg_principal, null))
            binding.btnScopeFriends.setBackgroundResource(R.drawable.bg_button_neon)
            binding.btnScopeGlobal.setTextColor(resources.getColor(R.color.gray_text, null))
            binding.btnScopeGlobal.background = null
            
            // Mock empty friends filter just for demonstration
            Toast.makeText(requireContext(), "Sem conexões de amigos ativas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateLeaderboard(list: List<LeaderboardRow>) {
        if (list.isEmpty()) {
            adapter.updateData(emptyList())
            return
        }

        // Separate top 3 podium if list has enough elements
        val podiumSize = Math.min(list.size, 3)
        val podiumList = list.subList(0, podiumSize)
        val remainingList = if (list.size > 3) list.subList(3, list.size) else emptyList()

        // Bind podium elements dynamically
        val density = resources.displayMetrics.density
        if (podiumSize >= 1) {
            val gold = podiumList[0]
            binding.tvNameGold.text = gold.name
            binding.tvCoinsGold.text = "${gold.game_coins} GC"
            
            if (!gold.image_url.isNullOrEmpty()) {
                binding.imgGoldRank.imageTintList = null
                binding.imgGoldRank.colorFilter = null
                binding.imgGoldRank.setPadding(0, 0, 0, 0)
                binding.imgGoldRank.load(gold.image_url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } else {
                val paddingPx = (5 * density).toInt()
                binding.imgGoldRank.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                binding.imgGoldRank.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.raridade_lendario, null))
                binding.imgGoldRank.setImageResource(R.drawable.ic_profile)
            }
        }
        if (podiumSize >= 2) {
            val silver = podiumList[1]
            binding.tvNameSilver.text = silver.name
            binding.tvCoinsSilver.text = "${silver.game_coins} GC"
            
            if (!silver.image_url.isNullOrEmpty()) {
                binding.imgSilverRank.imageTintList = null
                binding.imgSilverRank.colorFilter = null
                binding.imgSilverRank.setPadding(0, 0, 0, 0)
                binding.imgSilverRank.load(silver.image_url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } else {
                val paddingPx = (4 * density).toInt()
                binding.imgSilverRank.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                binding.imgSilverRank.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C0C0C0"))
                binding.imgSilverRank.setImageResource(R.drawable.ic_profile)
            }
        }
        if (podiumSize >= 3) {
            val bronze = podiumList[2]
            binding.tvNameBronze.text = bronze.name
            binding.tvCoinsBronze.text = "${bronze.game_coins} GC"
            
            if (!bronze.image_url.isNullOrEmpty()) {
                binding.imgBronzeRank.imageTintList = null
                binding.imgBronzeRank.colorFilter = null
                binding.imgBronzeRank.setPadding(0, 0, 0, 0)
                binding.imgBronzeRank.load(bronze.image_url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                }
            } else {
                val paddingPx = (4 * density).toInt()
                binding.imgBronzeRank.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                binding.imgBronzeRank.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#CD7F32"))
                binding.imgBronzeRank.setImageResource(R.drawable.ic_profile)
            }
        }

        adapter.updateData(remainingList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
