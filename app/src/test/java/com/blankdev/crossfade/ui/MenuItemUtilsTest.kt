package com.blankdev.crossfade.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuItemUtilsTest {

    @Test
    fun `rebuildVisibleList should show sub-items when header is expanded`() {
        // Arrange
        val subItems = listOf(
            MenuItemData(id = "sub1", title = "Sub 1", type = ShareBottomSheet.TYPE_SUBMENU_ITEM)
        )
        val header = MenuItemData(
            id = "header", 
            title = "Header", 
            type = ShareBottomSheet.TYPE_SUBMENU_HEADER,
            isExpanded = true,
            subItems = subItems
        )
        val originalItems = listOf(header)

        // Act
        val visible = MenuItemUtils.rebuildVisibleList(originalItems)

        // Assert
        assertEquals(2, visible.size)
        assertEquals("header", visible[0].id)
        assertEquals("sub1", visible[1].id)
    }

    @Test
    fun `rebuildVisibleList should hide sub-items when header is collapsed`() {
        // Arrange
        val subItems = listOf(
            MenuItemData(id = "sub1", title = "Sub 1", type = ShareBottomSheet.TYPE_SUBMENU_ITEM)
        )
        val header = MenuItemData(
            id = "header", 
            title = "Header", 
            type = ShareBottomSheet.TYPE_SUBMENU_HEADER,
            isExpanded = false,
            subItems = subItems
        )
        val originalItems = listOf(header)

        // Act
        val visible = MenuItemUtils.rebuildVisibleList(originalItems)

        // Assert
        assertEquals(1, visible.size)
        assertEquals("header", visible[0].id)
    }
}
