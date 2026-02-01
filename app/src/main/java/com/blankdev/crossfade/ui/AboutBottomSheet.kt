package com.blankdev.crossfade.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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

        setupOdesliLink()

        binding.btnGithub.setOnClickListener {
            openUrl("https://github.com/blankdotdev")
        }
    }

    private fun setupOdesliLink() {
        val text = binding.txtOdesliThankYou.text.toString()
        val spannableString = SpannableString(text)
        val odesli = "Odesli"
        val start = text.indexOf(odesli)
        
        if (start != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(view: View) {
                    openUrl("https://odesli.co/")
                }
            }
            spannableString.setSpan(clickableSpan, start, start + odesli.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.txtOdesliThankYou.text = spannableString
        binding.txtOdesliThankYou.movementMethod = LinkMovementMethod.getInstance()
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
