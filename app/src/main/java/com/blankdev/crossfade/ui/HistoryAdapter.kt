package com.blankdev.crossfade.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import coil.transform.RoundedCornersTransformation
import com.blankdev.crossfade.R
import com.blankdev.crossfade.data.HistoryItem
import com.blankdev.crossfade.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onMenuClick: (android.view.View, HistoryItem) -> Unit,
    private val onItemClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentItem: HistoryItem? = null
        
        init {
            // Set click listeners once in init to avoid repeated allocation
            binding.btnMore.setOnClickListener { 
                currentItem?.let { item -> onMenuClick(binding.btnMore, item) }
            }
            binding.root.setOnClickListener { 
                currentItem?.let { item -> onItemClick(item) }
            }
        }
        
        fun bind(item: HistoryItem) {
            // Avoid unnecessary rebinding
            if (currentItem?.id == item.id && 
                currentItem?.songTitle == item.songTitle &&
                currentItem?.artistName == item.artistName &&
                currentItem?.thumbnailUrl == item.thumbnailUrl) {
                return
            }
            
            currentItem = item
            
            binding.songTitle.text = item.songTitle ?: "Unknown Title"
            binding.artistName.text = item.artistName ?: "Unknown Artist"
            
            // Optimized image loading with Coil
            binding.albumArt.load(item.thumbnailUrl) {
                crossfade(150)
                scale(Scale.FILL)
                size(192) // 64dp * 3 (for xxxhdpi)
                allowHardware(true) // Use hardware bitmaps for better performance
                placeholder(R.drawable.placeholder_bg)
                error(R.drawable.placeholder_bg)
                transformations(RoundedCornersTransformation(12f))
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            // More efficient content comparison
            return oldItem.songTitle == newItem.songTitle &&
                   oldItem.artistName == newItem.artistName &&
                   oldItem.thumbnailUrl == newItem.thumbnailUrl &&
                   oldItem.timestamp == newItem.timestamp
        }
    }
}

