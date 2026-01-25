# Crossfade

Crossfade is an Android application designed to bridge the gap between music streaming services. It enables users to seamlessly redirect song and album links from one platform to another, ensuring a smooth listening experience regardless of which app your friends use.

<a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/blankdotdev/Crossfade"><img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="40"></a>

## ✨ Features

- **Universal Link Handling**: Support for Spotify, Apple Music, Tidal, Amazon Music, YouTube, YouTube Music, Deezer, Soundcloud, and more.
- **Smart Redirection**: Leverages the Odesli (Song.link) API to find the equivalent track on your preferred service.
- **Fallback Mechanism**: If an exact match isn't found, Crossfade performs a smart search (Title + Artist) within your target app.
- **Share Sheet Integration**: Redirect links directly from the Android Share Sheet—no more manual copying and pasting.
- **History & Favorites**: Keep track of resolved links for quick access.

## 🛠️ Configuration

To get the most out of Crossfade, set it as your default link handler for the music apps you don't use:

1.  Open **Crossfade** and navigate to **Settings**.
2.  Tap on **"Set Link Handling Defaults"**.
3.  In the System Settings, ensure "Open supported links" is enabled.
4.  Select the domains you want Crossfade to handle (e.g., `open.spotify.com`, `music.apple.com`).

## 📥 Installation

Currently, Crossfade is available as a direct APK download, can be built from source or can be installed from Obtainium.

## 🛡️ Verification

To ensure your Crossfade build is genuine and hasn't been tampered with, you can verify it using [AppVerifier](https://github.com/soupslurpr/AppVerifier).

**Package Name:** `com.blankdev.crossfade`
**SHA-256 Hash:** `BA:22:A6:99:ED:89:C9:FD:05:06:B6:E2:D7:6B:54:18:C9:45:F2:1E:A4:42:11:6C:17:44:F7:0D:54:EC:03:BA`

### How to Verify
1. Copy the SHA-256 hash above.
2. In [AppVerifier](https://github.com/soupslurpr/AppVerifier), select **Verify from clipboard**.
3. Choose the installed **Crossfade** app or the APK file.
4. [AppVerifier](https://github.com/soupslurpr/AppVerifier) will confirm if the hashes match.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.