# Changelog

All notable changes to this project will be documented in this file.

## [1.1.2] - 2026-02-02
- Added monochrome app icon support for Android 13+ theming.

## [1.1.1] - 2026-02-01
- Automatic redirection to "Fix Match" flow when a link cannot be resolved.

## [1.1.0] - 2026-02-01
- New "Fix Match" flow for manual link resolution.
- UX, performance and minor fixes.

## [1.0.4]
- Stable release of performance improvements and caching.
- Resolved link resolution issues on real devices.
- Cleaned up search queries for fallback redirects.
- Improved network compatibility with official APIs.

## [1.0.3]
- Disabled minification temporarily to rule out obfuscation-related resolution failures.
- Added User-Agent to API requests for better compatibility.
- Added diagnostic toasts to identify resolution errors on real devices.
- Refined Odesli resolution logic for better entity matching.

## [1.0.2]
- Fixed Odesli resolution failure on real devices by adding missing ProGuard rules.
- Improved search query cleaning to remove platform names from fallback searches.
- Enhanced reliability of caching mechanisms.

## [1.0.1]
- Added database caching for significantly faster link resolution.
- Optimized SoundCloud short link handling.
- Improved search results by filtering platform names from metadata.
- UI enhancements for a smoother redirection experience.

## [1.0.0]
- Initial release of Crossfade.
- Support for major music streaming platforms.
- Smart link redirection via Odesli API.
- Material You inspired design.
