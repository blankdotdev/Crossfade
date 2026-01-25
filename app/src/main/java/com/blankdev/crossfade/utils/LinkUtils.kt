package com.blankdev.crossfade.utils

import com.blankdev.crossfade.api.PlatformLink

object LinkUtils {
    /**
     * Merges 'itunes' link into 'appleMusic' if 'appleMusic' is missing.
     * Removes 'itunes' from the map in all cases to avoid redundancy.
     */
    fun mergeAppleMusicLinks(links: Map<String, PlatformLink>): Map<String, PlatformLink> {
        val mutableLinks = links.toMutableMap()
        val itunesLink = mutableLinks["itunes"]
        if (itunesLink != null) {
            if (!mutableLinks.containsKey("appleMusic")) {
                mutableLinks["appleMusic"] = itunesLink
            }
            mutableLinks.remove("itunes")
        }
        return mutableLinks
    }
}
