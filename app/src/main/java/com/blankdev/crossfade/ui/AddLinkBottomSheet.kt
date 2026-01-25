package com.blankdev.crossfade.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.blankdev.crossfade.databinding.DialogPasteUrlBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddLinkBottomSheet(private val onUrlEntered: (String) -> Unit) : BottomSheetDialogFragment() {

    private var _binding: DialogPasteUrlBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPasteUrlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnResolve.setOnClickListener {
            submitUrl()
        }

        binding.inputUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitUrl()
                true
            } else {
                false
            }
        }
        
        // Focus input and show keyboard
        binding.inputUrl.requestFocus()
    }

    private fun submitUrl() {
        val url = binding.inputUrl.text?.toString()
        if (!url.isNullOrBlank()) {
            onUrlEntered(url)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
