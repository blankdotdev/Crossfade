package com.blankdev.crossfade.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blankdev.crossfade.BuildConfig
import com.blankdev.crossfade.databinding.BottomSheetAboutBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AboutBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtVersion.text = "Version ${BuildConfig.VERSION_NAME}"

        binding.btnOdesli.setOnClickListener {
            openUrl("https://odesli.co/")
        }

        binding.btnGithub.setOnClickListener {
            openUrl("https://github.com/blankdotdev")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback or error handling
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
