package com.blankdev.crossfade.utils

import com.blankdev.crossfade.api.PlatformLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkUtilsTest {

    @Test
    fun `mergeAppleMusicLinks should replace itunes with appleMusic if appleMusic missing`() {
        // Arrange
        val itunesLink = PlatformLink("it1", "itunes_url", null, null)
        val links = mapOf("itunes" to itunesLink, "spotify" to PlatformLink("s1", "spotify_url", null, null))

        // Act
        val result = LinkUtils.mergeAppleMusicLinks(links)

        // Assert
        assertTrue(result.containsKey("appleMusic"))
        assertFalse(result.containsKey("itunes"))
        assertEquals("itunes_url", result["appleMusic"]?.url)
    }

    @Test
    fun `mergeAppleMusicLinks should remove itunes if appleMusic already exists`() {
        // Arrange
        val itunesLink = PlatformLink("it1", "itunes_url", null, null)
        val amLink = PlatformLink("am1", "am_url", null, null)
        val links = mapOf("itunes" to itunesLink, "appleMusic" to amLink)

        // Act
        val result = LinkUtils.mergeAppleMusicLinks(links)

        // Assert
        assertTrue(result.containsKey("appleMusic"))
        assertFalse(result.containsKey("itunes"))
        assertEquals("am_url", result["appleMusic"]?.url)
    }

    @Test
    fun `mergeAppleMusicLinks should do nothing if no itunes link`() {
        // Arrange
        val links = mapOf("spotify" to PlatformLink("s1", "spotify_url", null, null))

        // Act
        val result = LinkUtils.mergeAppleMusicLinks(links)

        // Assert
        assertEquals(1, result.size)
        assertTrue(result.containsKey("spotify"))
    }
}
