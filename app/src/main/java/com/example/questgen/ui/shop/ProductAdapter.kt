package com.example.questgen.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.questgen.R
import com.example.questgen.data.model.Product
import com.example.questgen.databinding.ItemProductBinding
import com.example.questgen.util.formatCoins

class ProductAdapter(
    private var products: List<Product>,
    private val onRedeemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.tvProductName.text = product.name
        holder.binding.tvProductPrice.text = product.price.formatCoins()

        // Rarity Tag dynamic details
        val mockTags = listOf("LENDÁRIO", "RARO", "FINAL BOSS", "CUPOM")
        val index = (position) % mockTags.size
        val tagText = mockTags[index]
        holder.binding.tvProductTag.text = tagText

        // Dynamic styling for tag badge
        when (tagText) {
            "LENDÁRIO" -> {
                holder.binding.tvProductTag.setTextColor(holder.itemView.resources.getColor(R.color.raridade_lendario, null))
                holder.binding.tvProductTag.setBackgroundColor(holder.itemView.resources.getColor(R.color.bg_principal, null))
            }
            "RARO" -> {
                holder.binding.tvProductTag.setTextColor(holder.itemView.resources.getColor(R.color.raridade_raro, null))
                holder.binding.tvProductTag.setBackgroundColor(holder.itemView.resources.getColor(R.color.bg_principal, null))
            }
            "FINAL BOSS" -> {
                holder.binding.tvProductTag.setTextColor(holder.itemView.resources.getColor(R.color.roxo_neon, null))
                holder.binding.tvProductTag.setBackgroundColor(holder.itemView.resources.getColor(R.color.bg_principal, null))
            }
            else -> {
                holder.binding.tvProductTag.setTextColor(holder.itemView.resources.getColor(R.color.raridade_comum, null))
                holder.binding.tvProductTag.setBackgroundColor(holder.itemView.resources.getColor(R.color.bg_principal, null))
            }
        }

        // Load product image
        holder.binding.imgProduct.load(product.image_url) {
            crossfade(true)
            placeholder(R.drawable.ic_games)
            error(R.drawable.ic_games)
        }

        holder.binding.btnBuyProduct.setOnClickListener {
            onRedeemClick(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
