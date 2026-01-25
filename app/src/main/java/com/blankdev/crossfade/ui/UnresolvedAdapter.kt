package com.blankdev.crossfade.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blankdev.crossfade.data.HistoryItem
import com.blankdev.crossfade.databinding.ItemUnresolvedHeaderBinding
import com.blankdev.crossfade.databinding.ItemUnresolvedLinkBinding

class UnresolvedAdapter(
    private val onMenuClick: (android.view.View, HistoryItem) -> Unit,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<HistoryItem> = emptyList()
    private var isExpanded: Boolean = false

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    fun submitList(newList: List<HistoryItem>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemUnresolvedHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemUnresolvedLinkBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind()
        } else if (holder is ItemViewHolder) {
            holder.bind(items[position - 1])
        }
    }

    override fun getItemCount(): Int {
        if (items.isEmpty()) return 0
        return if (isExpanded) items.size + 1 else 1
    }

    inner class HeaderViewHolder(private val binding: ItemUnresolvedHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.tvCount.text = "${items.size} unresolved links"
            binding.ivExpand.rotation = if (isExpanded) 180f else 0f
            binding.headerRoot.setOnClickListener {
                isExpanded = !isExpanded
                notifyDataSetChanged()
            }
        }
    }

    inner class ItemViewHolder(private val binding: ItemUnresolvedLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem) {
            binding.tvUrl.text = item.originalUrl
            
            binding.btnMore.setOnClickListener {
                onMenuClick(it, item)
            }
            
            binding.unresolvedRoot.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
