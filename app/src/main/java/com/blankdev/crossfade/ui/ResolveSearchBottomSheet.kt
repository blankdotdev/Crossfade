package com.blankdev.crossfade.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.api.ITunesResult
import com.blankdev.crossfade.data.HistoryItem
import com.blankdev.crossfade.data.ResolveResult
import com.blankdev.crossfade.databinding.BottomSheetResolveSearchBinding
import com.blankdev.crossfade.databinding.ItemResolveResultBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ResolveSearchBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetResolveSearchBinding
    private var item: HistoryItem? = null
    private var type: String = "Song"
    
    var onResolved: ((HistoryItem) -> Unit)? = null

    companion object {
        private const val ARG_ITEM_JSON = "arg_item_json"
        private const val ARG_TYPE = "arg_type"

        fun newInstance(item: HistoryItem, type: String): ResolveSearchBottomSheet {
            val fragment = ResolveSearchBottomSheet()
            val args = Bundle()
            args.putString(ARG_ITEM_JSON, Gson().toJson(item))
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString(ARG_ITEM_JSON)
        if (json != null) {
            item = Gson().fromJson(json, HistoryItem::class.java)
        }
        type = arguments?.getString(ARG_TYPE) ?: "Song"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetResolveSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.resolveTitle.text = "Search for $type"
        
        val adapter = ResultsAdapter { selectedResult ->
            resolveItem(selectedResult)
        }
        binding.resultsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.resultsRecyclerView.adapter = adapter
        
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.searchEditText.text.toString(), adapter)
                true
            } else {
                false
            }
        }
        
        binding.btnNone.setOnClickListener {
            dismiss()
        }
        
        // Initial search if we have a title or artist
        val initialQuery = item?.let {
            if (!it.songTitle.isNullOrBlank()) {
                if (!it.artistName.isNullOrBlank()) "${it.songTitle} ${it.artistName}" else it.songTitle
            } else {
                null
            }
        }
        
        if (initialQuery != null) {
            binding.searchEditText.setText(initialQuery)
            performSearch(initialQuery, adapter)
        }
    }

    private fun performSearch(query: String, adapter: ResultsAdapter) {
        if (query.isBlank()) return
        
        binding.progressBar.isVisible = true
        binding.noResultsText.isVisible = false
        
        lifecycleScope.launch {
            val results = CrossfadeApp.instance.linkResolver.searchITunes(query, type)
            binding.progressBar.isVisible = false
            adapter.submitList(results)
            binding.noResultsText.isVisible = results.isEmpty()
        }
    }

    private fun resolveItem(result: ITunesResult) {
        val currentItem = item ?: return
        val url = result.trackViewUrl ?: result.collectionViewUrl ?: return
        
        binding.progressBar.isVisible = true
        lifecycleScope.launch {
            val resolveResult = CrossfadeApp.instance.linkResolver.resolveManual(currentItem, url)
            binding.progressBar.isVisible = false
            
            when (resolveResult) {
                is ResolveResult.Success -> {
                    Toast.makeText(context, "Resolved successfully", Toast.LENGTH_SHORT).show()
                    onResolved?.invoke(resolveResult.historyItem)
                    dismiss()
                }
                is ResolveResult.Error -> {
                    Toast.makeText(context, resolveResult.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    Toast.makeText(context, "Resolution failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private class ResultsAdapter(private val onItemClick: (ITunesResult) -> Unit) : 
        RecyclerView.Adapter<ResultsAdapter.ViewHolder>() {
        
        private var results: List<ITunesResult> = emptyList()

        fun submitList(newList: List<ITunesResult>) {
            results = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemResolveResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(results[position])
        }

        override fun getItemCount() = results.size

        inner class ViewHolder(private val binding: ItemResolveResultBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(result: ITunesResult) {
                binding.resultTitle.text = result.trackName ?: result.collectionName ?: "Unknown"
                binding.resultArtist.text = result.artistName ?: "Unknown Artist"
                binding.resultImage.load(result.artworkUrl100) {
                    placeholder(com.blankdev.crossfade.R.drawable.ic_placeholder_service)
                    error(com.blankdev.crossfade.R.drawable.ic_placeholder_service)
                }
                binding.root.setOnClickListener { onItemClick(result) }
            }
        }
    }
}
