package com.example.questgen.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.questgen.data.model.Product
import com.example.questgen.databinding.FragmentShopBinding
import com.example.questgen.util.collectLatestFlow
import com.example.questgen.util.toast
import com.example.questgen.viewmodel.MainViewModel
import com.example.questgen.viewmodel.ShopState
import com.example.questgen.viewmodel.ShopViewModel
import com.google.android.material.tabs.TabLayout

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!

    private val shopViewModel: ShopViewModel by viewModels()
    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private lateinit var adapter: ProductAdapter
    private var allProductsList: List<Product> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter with 2 column grid
        adapter = ProductAdapter(emptyList()) { product ->
            simulateRedemption(product)
        }

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = adapter

        // Trigger initial data load (cached in ViewModel)
        shopViewModel.fetchShopItems()

        // Setup retry click
        binding.btnShopRetry.setOnClickListener {
            shopViewModel.fetchShopItems(forceRefresh = true)
        }

        // Setup Tab filter selection
        binding.tabShopCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val tabName = tab?.text?.toString() ?: "PERIFÉRICOS"
                shopViewModel.selectTab(tabName)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Observe raw product load list
        collectLatestFlow(shopViewModel.shopState) { state ->
            when (state) {
                is ShopState.Loading -> {
                    binding.progressShopLoading.visibility = View.VISIBLE
                    binding.layoutShopError.visibility = View.GONE
                    binding.rvProducts.visibility = View.GONE
                }
                is ShopState.Success -> {
                    binding.progressShopLoading.visibility = View.GONE
                    binding.layoutShopError.visibility = View.GONE
                    binding.rvProducts.visibility = View.VISIBLE
                    allProductsList = state.list
                    filterProducts(shopViewModel.currentTab.value)
                }
                is ShopState.Error -> {
                    binding.progressShopLoading.visibility = View.GONE
                    binding.layoutShopError.visibility = View.VISIBLE
                    binding.rvProducts.visibility = View.GONE
                    binding.tvShopErrorMsg.text = state.message
                }
            }
        }

        // Observe category tab and re-filter
        collectLatestFlow(shopViewModel.currentTab) { tabName ->
            filterProducts(tabName)
        }

        // Observe buyCosmeticState for dynamic feedback
        collectLatestFlow(mainViewModel.buyCosmeticState) { state ->
            when (state) {
                is com.example.questgen.viewmodel.BuyCosmeticState.Loading -> {
                    binding.progressShopLoading.visibility = View.VISIBLE
                }
                is com.example.questgen.viewmodel.BuyCosmeticState.Success -> {
                    binding.progressShopLoading.visibility = View.GONE
                    toast(state.message)
                    mainViewModel.resetBuyCosmeticState()
                }
                is com.example.questgen.viewmodel.BuyCosmeticState.Error -> {
                    binding.progressShopLoading.visibility = View.GONE
                    toast(state.message)
                    mainViewModel.resetBuyCosmeticState()
                }
                else -> {}
            }
        }
    }

    private fun filterProducts(tabName: String) {
        val filtered = allProductsList.filter { product ->
            when (tabName) {
                "SKINS" -> product.category.equals("Skins", ignoreCase = true)
                "CUPONS" -> product.category.equals("Gift Cards", ignoreCase = true) || product.category.equals("Prêmios Digitais", ignoreCase = true)
                "COSMÉTICOS" -> product.category.equals("Cosméticos", ignoreCase = true)
                else -> product.category.equals("Periféricos", ignoreCase = true)
            }
        }
        adapter.updateData(filtered)
    }

    private fun simulateRedemption(product: Product) {
        val currentUser = mainViewModel.currentUser.value
        if (currentUser == null) {
            toast("Faça login para realizar trocas")
            return
        }

        if (currentUser.game_coins < product.price) {
            val missing = product.price - currentUser.game_coins
            toast("Saldo insuficiente! Faltam $missing GC")
            return
        }

        // Se for um cosmético (moldura), comprar de forma persistente e segura no banco
        if (product.category.equals("Cosméticos", ignoreCase = true)) {
            mainViewModel.comprarCosmetico(product.id)
        } else {
            // Deduct coins and update Shared ViewModel locally
            val updatedUser = currentUser.copy(game_coins = currentUser.game_coins - product.price)
            mainViewModel.updateUser(updatedUser)
            toast("Resgate efetuado! ${product.name} enviado ao inventário")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
