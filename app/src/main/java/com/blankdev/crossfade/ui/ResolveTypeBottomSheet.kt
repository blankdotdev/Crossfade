package com.blankdev.crossfade.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blankdev.crossfade.databinding.BottomSheetResolveTypeBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ResolveTypeBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetResolveTypeBinding
    
    var onTypeSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetResolveTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnSong.setOnClickListener {
            onTypeSelected?.invoke("Song")
            dismiss()
        }
        
        binding.btnAlbum.setOnClickListener {
            onTypeSelected?.invoke("Album")
            dismiss()
        }
        
        binding.btnPodcast.setOnClickListener {
            onTypeSelected?.invoke("Podcast")
            dismiss()
        }
    }
}
