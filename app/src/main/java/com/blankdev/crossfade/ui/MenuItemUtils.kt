package com.blankdev.crossfade.ui

object MenuItemUtils {
    fun rebuildVisibleList(originalItems: List<MenuItemData>): List<MenuItemData> {
        val visible = mutableListOf<MenuItemData>()
        originalItems.forEach { item ->
            visible.add(item)
            if (item.type == ShareBottomSheet.TYPE_SUBMENU_HEADER && item.isExpanded) {
                visible.addAll(item.subItems)
            }
        }
        return visible
    }
}
