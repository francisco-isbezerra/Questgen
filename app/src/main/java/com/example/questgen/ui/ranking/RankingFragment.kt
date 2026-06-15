package com.example.questgen.ui.ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.LeaderboardRow
import com.example.questgen.databinding.FragmentRankingBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.formatCoins
import com.example.questgen.viewmodel.MainViewModel
import com.example.questgen.viewmodel.RankingState

class RankingFragment : Fragment() {

    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: LeaderboardAdapter
    private lateinit var clanAdapter: ClanAdapter
    private var isShowingPlayers = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapters
        adapter = LeaderboardAdapter(emptyList())
        clanAdapter = ClanAdapter(emptyList())

        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter // Default

        // Trigger initial data load (cached in ViewModel)
        mainViewModel.fetchRanking()

        // Toggle clicks
        binding.btnScopeGlobal.setOnClickListener {
            toggleScope(showPlayers = true)
        }

        binding.btnScopeFriends.setOnClickListener {
            toggleScope(showPlayers = false)
        }

        // Retry click
        binding.btnRankingRetry.setOnClickListener {
            if (isShowingPlayers) {
                mainViewModel.fetchRanking(forceRefresh = true)
            } else {
                mainViewModel.obterRankingClans(forceRefresh = true)
            }
        }

        // Observe Players Ranking state
        collectLatestFlow(mainViewModel.rankingState) { state ->
            if (isShowingPlayers) {
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

        // Observe Clans Ranking state
        collectLatestFlow(mainViewModel.clanRankingState) { state ->
            if (!isShowingPlayers) {
                when (state) {
                    is com.example.questgen.viewmodel.ClanRankingState.Loading -> {
                        binding.progressRankingLoading.visibility = View.VISIBLE
                        binding.layoutRankingError.visibility = View.GONE
                        binding.rvLeaderboard.visibility = View.GONE
                        binding.layoutPodium.visibility = View.GONE
                    }
                    is com.example.questgen.viewmodel.ClanRankingState.SuccessList -> {
                        binding.progressRankingLoading.visibility = View.GONE
                        binding.layoutRankingError.visibility = View.GONE
                        binding.rvLeaderboard.visibility = View.VISIBLE
                        binding.layoutPodium.visibility = View.GONE
                        clanAdapter.updateData(state.list)
                    }
                    is com.example.questgen.viewmodel.ClanRankingState.Error -> {
                        binding.progressRankingLoading.visibility = View.GONE
                        binding.layoutRankingError.visibility = View.VISIBLE
                        binding.rvLeaderboard.visibility = View.GONE
                        binding.layoutPodium.visibility = View.GONE
                        binding.tvRankingErrorMsg.text = state.message
                    }
                    else -> {}
                }
            }
        }
    }

    private fun toggleScope(showPlayers: Boolean) {
        isShowingPlayers = showPlayers
        if (showPlayers) {
            binding.btnScopeGlobal.setTextColor(resources.getColor(R.color.bg_principal, null))
            binding.btnScopeGlobal.setBackgroundResource(R.drawable.bg_button_neon)
            binding.btnScopeFriends.setTextColor(resources.getColor(R.color.gray_text, null))
            binding.btnScopeFriends.background = null

            binding.rvLeaderboard.adapter = adapter
            binding.layoutPodium.visibility = View.VISIBLE
            mainViewModel.fetchRanking()
        } else {
            binding.btnScopeFriends.setTextColor(resources.getColor(R.color.bg_principal, null))
            binding.btnScopeFriends.setBackgroundResource(R.drawable.bg_button_neon)
            binding.btnScopeGlobal.setTextColor(resources.getColor(R.color.gray_text, null))
            binding.btnScopeGlobal.background = null

            binding.rvLeaderboard.adapter = clanAdapter
            binding.layoutPodium.visibility = View.GONE
            mainViewModel.obterRankingClans()
        }
    }

    private fun stylePodiumAvatar(
        cardWrapper: androidx.cardview.widget.CardView,
        imageView: android.widget.ImageView,
        moldura: String?,
        defaultPadding: Int
    ) {
        val density = resources.displayMetrics.density
        if (!moldura.isNullOrEmpty()) {
            val borderColor = when (moldura) {
                "neon_ciano" -> "#00BFFF"
                "neon_roxo" -> "#8A2BE2"
                "ouro_lendario" -> "#FFD700"
                else -> null
            }
            if (borderColor != null) {
                cardWrapper.setCardBackgroundColor(android.graphics.Color.parseColor(borderColor))
                val borderPadding = (3 * density).toInt()
                imageView.setPadding(borderPadding, borderPadding, borderPadding, borderPadding)
                return
            }
        }
        cardWrapper.setCardBackgroundColor(resources.getColor(R.color.bg_secundario, null))
        imageView.setPadding(defaultPadding, defaultPadding, defaultPadding, defaultPadding)
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
            binding.tvCoinsGold.text = gold.game_coins.formatCoins()

            stylePodiumAvatar(binding.cardGoldRankWrapper, binding.imgGoldRank, gold.moldura_neon, (5 * density).toInt())

            val goldUrl = gold.getAbsoluteImageUrl()
            if (!goldUrl.isNullOrEmpty()) {
                binding.imgGoldRank.imageTintList = null
                binding.imgGoldRank.colorFilter = null
                if (gold.moldura_neon.isNullOrEmpty()) {
                    binding.imgGoldRank.setPadding(0, 0, 0, 0)
                }
                binding.imgGoldRank.load(goldUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                    memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    diskCachePolicy(coil.request.CachePolicy.DISABLED)
                }
            } else {
                binding.imgGoldRank.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.raridade_lendario, null))
                binding.imgGoldRank.setImageResource(R.drawable.ic_profile)
            }
        }
        if (podiumSize >= 2) {
            val silver = podiumList[1]
            binding.tvNameSilver.text = silver.name
            binding.tvCoinsSilver.text = silver.game_coins.formatCoins()

            stylePodiumAvatar(binding.cardSilverRankWrapper, binding.imgSilverRank, silver.moldura_neon, (4 * density).toInt())

            val silverUrl = silver.getAbsoluteImageUrl()
            if (!silverUrl.isNullOrEmpty()) {
                binding.imgSilverRank.imageTintList = null
                binding.imgSilverRank.colorFilter = null
                if (silver.moldura_neon.isNullOrEmpty()) {
                    binding.imgSilverRank.setPadding(0, 0, 0, 0)
                }
                binding.imgSilverRank.load(silverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                    memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    diskCachePolicy(coil.request.CachePolicy.DISABLED)
                }
            } else {
                binding.imgSilverRank.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C0C0C0"))
                binding.imgSilverRank.setImageResource(R.drawable.ic_profile)
            }
        }
        if (podiumSize >= 3) {
            val bronze = podiumList[2]
            binding.tvNameBronze.text = bronze.name
            binding.tvCoinsBronze.text = bronze.game_coins.formatCoins()

            stylePodiumAvatar(binding.cardBronzeRankWrapper, binding.imgBronzeRank, bronze.moldura_neon, (4 * density).toInt())

            val bronzeUrl = bronze.getAbsoluteImageUrl()
            if (!bronzeUrl.isNullOrEmpty()) {
                binding.imgBronzeRank.imageTintList = null
                binding.imgBronzeRank.colorFilter = null
                if (bronze.moldura_neon.isNullOrEmpty()) {
                    binding.imgBronzeRank.setPadding(0, 0, 0, 0)
                }
                binding.imgBronzeRank.load(bronzeUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile)
                    error(R.drawable.ic_profile)
                    memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    diskCachePolicy(coil.request.CachePolicy.DISABLED)
                }
            } else {
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
