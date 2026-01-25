package com.blankdev.crossfade.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.R
import com.blankdev.crossfade.databinding.ActivityOnboardingBinding
import com.blankdev.crossfade.databinding.ItemOnboardingStep1Binding
import com.blankdev.crossfade.databinding.ItemOnboardingStep2Binding
import com.blankdev.crossfade.utils.SettingsManager
import com.google.android.material.color.MaterialColors

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val settings by lazy { CrossfadeApp.instance.settingsManager }

    override fun onResume() {
        super.onResume()
        // Re-check button state (especially for Step 2 returning from settings)
        updateNavigationState(binding.viewPager.currentItem)
        
        // Check for conflicts when returning from OS settings (Step 2)
        if (binding.viewPager.currentItem == 1) {
            checkAndWarnConflict()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupListeners()
        
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.currentItem > 0) {
                    binding.viewPager.currentItem = binding.viewPager.currentItem - 1
                } else {
                    // If at step 1, standard back behavior (exit app or activity)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(
            onSpotifySelected = { /* No-op, legacy */ },
            onAppleSelected = { /* No-op */ },
            onTidalSelected = { /* No-op */ },
            onYoutubeSelected = { /* No-op */ },
            onAmazonSelected = { /* No-op */ },
            onSelectionChange = { target -> 
                highlightSelection(target)
                updateNavigationState(binding.viewPager.currentItem)
            },
            onSetDefaultClicked = { openSystemSettings() }
        )
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // Prevent swiping, enforce "Continue" button or selection
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavigationState(position)
                if (position == 1) {
                    adapter.notifyItemChanged(1)
                }
            }
        })
        
        // Initial setup for indicators (simple implementation)
        setupIndicators(2)
        updateIndicators(0)
    }

    private fun setupListeners() {
        binding.btnContinue.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem == 0) {
                binding.viewPager.setCurrentItem(1, true)
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateNavigationState(position: Int) {
        updateIndicators(position)
        if (position == 0) {
            // Step 1: Check if a service is selected
            val isServiceSelected = settings.targetApp != SettingsManager.TARGET_UNIVERSAL
            binding.btnContinue.text = if (isServiceSelected) "Continue" else "Skip, ask everytime"
        } else {
            // Step 2: Check if Crossfade is default handler
            val isDefault = checkDefaultHandlerStatus()
            binding.btnContinue.text = if (isDefault) "Finish" else "Not now"
        }
    }
    
    private fun checkDefaultHandlerStatus(): Boolean {
        // Check if we are the default handler for ANY of the supported domains
        val sampleUrls = listOf(
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC",
            "https://music.apple.com/us/album/never-gonna-give-you-up/123",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://tidal.com/browse/track/12345",
            "https://music.amazon.com/albums/B0012345",
            "https://www.deezer.com/track/12345",
            "https://soundcloud.com/user/track",
            "https://web.napster.com/track/123",
            "https://www.pandora.com/artist/123",
            "https://audiomack.com/song/123",
            "https://play.anghami.com/song/123",
            "https://www.boomplay.com/songs/123",
            "https://music.yandex.ru/album/123/track/456",
            "https://audius.co/track/123",
            "https://bandcamp.com/track/123",
            "https://odesli.co/test"
        )
        
        return sampleUrls.any { url ->
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == packageName
        }
    }

    private fun finishOnboarding() {
        settings.hasCompletedOnboarding = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    // Simple indicator logic
    private val indicators = mutableListOf<View>()
    
    private fun setupIndicators(count: Int) {
        binding.indicatorContainer.removeAllViews()
        indicators.clear()
        
        for (i in 0 until count) {
            val view = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(24, 24).apply {
                    setMargins(12, 0, 12, 0)
                }
                setBackgroundResource(R.drawable.indicator_dot)
            }
            // Use simple shapes or just view modification
             // Actually let's just use alpha/color
            binding.indicatorContainer.addView(view)
            indicators.add(view)
        }
    }

    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, view ->
            val layoutParams = view.layoutParams as android.widget.LinearLayout.LayoutParams
            val color = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSurface)
            if (index == position) {
                 layoutParams.width = 60
                 view.alpha = 1.0f
                 view.background.setTint(color)
            } else {
                layoutParams.width = 24
                view.alpha = 0.3f
                view.background.setTint(color)
            }
            view.layoutParams = layoutParams
        }
    }
    
    private fun checkAndWarnConflict() {
        val currentTarget = settings.targetApp
        if (com.blankdev.crossfade.utils.DefaultHandlerChecker.hasConflict(this, currentTarget)) {
            val platformName = com.blankdev.crossfade.utils.DefaultHandlerChecker.getPlatformDisplayName(currentTarget)
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Conflict Detected")
                .setMessage("You've set Crossfade as the default handler for $platformName links, " +
                        "but $platformName is also your preferred music app. This will cause an infinite loop.\n\n" +
                        "Links will automatically open in Odesli instead to avoid this issue.")
                .setPositiveButton("OK") { _, _ ->
                    settings.useOdesliForConflicts = true
                }
                .setNegativeButton("Change Preferred App") { _, _ ->
                    // Go back to step 1
                    binding.viewPager.setCurrentItem(0, true)
                }
                .show()
        }
    }

    private fun openSystemSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                // Fallback
            }
        }
    }
    
    // Selection logic would ideally be passed to the adapter to update the view, 
    // but for simplicity we are handling selection state in Settings and just updating UI locally in the fragment/item if possible.
    // However, with ViewPager2 + RecyclerView, the views are inside the Viewholder.
    // simpler approach: The adapter handles the click and updates the UI inside the ViewHolder.
    private fun highlightSelection(target: String) {
        settings.targetApp = target
        // Visual feedback is handled in the Adapter
    }

    inner class OnboardingAdapter(
        private val onSpotifySelected: () -> Unit,
        private val onAppleSelected: () -> Unit,
        private val onTidalSelected: () -> Unit,
        private val onYoutubeSelected: () -> Unit,
        private val onAmazonSelected: () -> Unit,
        private val onSelectionChange: (String) -> Unit, // Unified callback
        private val onSetDefaultClicked: () -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_STEP1 = 0
        private val VIEW_TYPE_STEP2 = 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_STEP1) {
                Step1ViewHolder(ItemOnboardingStep1Binding.inflate(LayoutInflater.from(parent.context), parent, false))
            } else {
                Step2ViewHolder(ItemOnboardingStep2Binding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is Step1ViewHolder) {
                holder.bind()
            } else if (holder is Step2ViewHolder) {
                holder.bind()
            }
        }

        override fun getItemViewType(position: Int): Int = if (position == 0) VIEW_TYPE_STEP1 else VIEW_TYPE_STEP2
        override fun getItemCount(): Int = 2

        inner class Step1ViewHolder(private val binding: ItemOnboardingStep1Binding) : RecyclerView.ViewHolder(binding.root) {
            
            fun bind() {
                // Map of include ID -> (Platform Key, Icon Res, Label)
                val items = listOf(
                    Triple(binding.itemSpotify, SettingsManager.TARGET_SPOTIFY, R.drawable.ic_spotify to "Spotify"),
                    Triple(binding.itemApple, SettingsManager.TARGET_APPLE_MUSIC, R.drawable.ic_apple_music to "Apple Music"),
                    Triple(binding.itemYoutube, SettingsManager.PLATFORM_YOUTUBE, R.drawable.ic_youtube to "YouTube"),
                    Triple(binding.itemYoutubeMusic, SettingsManager.TARGET_YOUTUBE_MUSIC, R.drawable.ic_youtube_music to "YT Music"),
                    Triple(binding.itemAmazon, SettingsManager.TARGET_AMAZON_MUSIC, R.drawable.ic_amazon_music to "Amazon"),
                    Triple(binding.itemTidal, SettingsManager.TARGET_TIDAL, R.drawable.ic_tidal to "Tidal"),
                    Triple(binding.itemDeezer, SettingsManager.PLATFORM_DEEZER, R.drawable.ic_deezer to "Deezer"),
                    Triple(binding.itemSoundcloud, SettingsManager.PLATFORM_SOUNDCLOUD, R.drawable.ic_soundcloud to "SoundCloud"),
                    Triple(binding.itemNapster, SettingsManager.PLATFORM_NAPSTER, R.drawable.ic_napster to "Napster"),
                    Triple(binding.itemPandora, SettingsManager.PLATFORM_PANDORA, R.drawable.ic_pandora to "Pandora"),
                    Triple(binding.itemAudiomack, SettingsManager.PLATFORM_AUDIOMACK, R.drawable.ic_audiomack to "Audiomack"),
                    Triple(binding.itemAnghami, SettingsManager.PLATFORM_ANGHAMI, R.drawable.ic_anghami to "Anghami"),
                    Triple(binding.itemBoomplay, SettingsManager.PLATFORM_BOOMPLAY, R.drawable.ic_boomplay to "Boomplay"),
                    Triple(binding.itemYandex, SettingsManager.PLATFORM_YANDEX, R.drawable.ic_yandex_music to "Yandex"),
                    Triple(binding.itemBandcamp, SettingsManager.PLATFORM_BANDCAMP, R.drawable.ic_bandcamp to "Bandcamp")
                )

                // Initialize Views
                items.forEachIndexed { index, (viewBinding, key, res) ->
                    val (iconRes, labelText) = res
                    val iconView = viewBinding.root.findViewById<ImageView>(R.id.icon)
                    val labelView = viewBinding.root.findViewById<android.widget.TextView>(R.id.label)
                    
                    iconView.setImageResource(iconRes)
                    labelView.text = labelText
                    
                    // Sizing logic
                     val isMajor = key in listOf(
                        SettingsManager.TARGET_SPOTIFY,
                        SettingsManager.TARGET_TIDAL,
                        SettingsManager.TARGET_YOUTUBE_MUSIC,
                        SettingsManager.TARGET_AMAZON_MUSIC,
                        SettingsManager.PLATFORM_SOUNDCLOUD
                    )
                    
                    // Reduced sizes to fit better on screen
                    val size = if (isMajor) 80 else 56
                    val padding = if (isMajor) 14 else 10
                    
                    // Update layout params
                    val params = iconView.layoutParams
                    val density = viewBinding.root.context.resources.displayMetrics.density
                    params.width = (size * density).toInt()
                    params.height = (size * density).toInt()
                    iconView.layoutParams = params
                    iconView.setPadding((padding * density).toInt(), (padding * density).toInt(), (padding * density).toInt(), (padding * density).toInt())
                    
                    // Specific fix for Apple Music square icon
                    if (key == SettingsManager.TARGET_APPLE_MUSIC) {
                        iconView.clipToOutline = true
                        iconView.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                    } else {
                        iconView.clipToOutline = false
                        iconView.outlineProvider = null
                    }
                    
                    viewBinding.root.setOnClickListener {
                        selectIcon(items.map { it.first.root.findViewById(R.id.icon) }, iconView)
                        onSelectionChange(key)
                    }
                    
                    // Staggered animation
                    startFloatingAnimation(viewBinding.root, (index * 200).toLong())
                }
                
                // Set initial selection
                val currentTarget = settings.targetApp
                val selectedItem = items.find { it.second == currentTarget }
                // If selectedItem is null (e.g. TARGET_UNIVERSAL), selectIcon handles null to clear selection
                selectIcon(items.map { it.first.root.findViewById(R.id.icon) }, selectedItem?.first?.root?.findViewById(R.id.icon))
            }

            private fun selectIcon(allIcons: List<ImageView>, selectedIcon: ImageView?) {
                // Reset styling
                allIcons.forEach { icon ->
                    icon.animate().scaleX(1.0f).scaleY(1.0f).alpha(0.6f).setDuration(200).start()
                    icon.background.setTintList(null)
                    icon.clearColorFilter()
                }
                
                // Highlight selected if exists
                if (selectedIcon != null) {
                    selectedIcon.animate().scaleX(1.1f).scaleY(1.1f).alpha(1.0f).setDuration(200).start()
                    val typedValue = android.util.TypedValue()
                    binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
                    selectedIcon.background.setTint(typedValue.data)
                }
            }

            private fun startFloatingAnimation(view: View, delay: Long) {
                val animation = TranslateAnimation(0f, 0f, 0f, -15f).apply {
                    duration = 3000 + (Math.random() * 1000).toLong()
                    startOffset = delay
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                view.startAnimation(animation)
            }
        }

        inner class Step2ViewHolder(private val binding: ItemOnboardingStep2Binding) : RecyclerView.ViewHolder(binding.root) {
            fun bind() {
                binding.btnSetDefault.setOnClickListener { onSetDefaultClicked() }
                
                // Set target icon based on selection
                 val currentTarget = settings.targetApp
                 
                 if (currentTarget == SettingsManager.TARGET_UNIVERSAL) {
                     // Globe -> Arrow -> Crossfade
                     binding.iconArrow2.visibility = View.GONE
                     binding.iconTarget.visibility = View.GONE
                 } else {
                     // Globe -> Arrow -> Crossfade -> Arrow -> Service
                     binding.iconArrow2.visibility = View.VISIBLE
                     binding.iconTarget.visibility = View.VISIBLE
                     
                     val iconRes = when(currentTarget) {
                        SettingsManager.TARGET_SPOTIFY -> R.drawable.ic_spotify
                        SettingsManager.TARGET_APPLE_MUSIC -> R.drawable.ic_apple_music
                        SettingsManager.TARGET_YOUTUBE_MUSIC -> R.drawable.ic_youtube_music
                        SettingsManager.TARGET_AMAZON_MUSIC -> R.drawable.ic_amazon_music
                        SettingsManager.TARGET_TIDAL -> R.drawable.ic_tidal
                        SettingsManager.PLATFORM_DEEZER -> R.drawable.ic_deezer
                        SettingsManager.PLATFORM_SOUNDCLOUD -> R.drawable.ic_soundcloud
                        SettingsManager.PLATFORM_NAPSTER -> R.drawable.ic_napster
                        SettingsManager.PLATFORM_PANDORA -> R.drawable.ic_pandora
                        SettingsManager.PLATFORM_AUDIOMACK -> R.drawable.ic_audiomack
                        SettingsManager.PLATFORM_ANGHAMI -> R.drawable.ic_anghami
                        SettingsManager.PLATFORM_BOOMPLAY -> R.drawable.ic_boomplay
                        SettingsManager.PLATFORM_YANDEX -> R.drawable.ic_yandex_music
                        SettingsManager.PLATFORM_AUDIUS -> R.drawable.ic_audius
                        SettingsManager.PLATFORM_BANDCAMP -> R.drawable.ic_bandcamp
                        SettingsManager.PLATFORM_YOUTUBE -> R.drawable.ic_youtube
                        else -> R.drawable.ic_spotify
                     }
                     binding.iconTarget.setImageResource(iconRes)
                 }
            }
        }
    }
}
