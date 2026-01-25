package com.blankdev.crossfade.ui

data class MenuItemData(
    val id: String,
    val title: String,
    val url: String? = null,
    val type: Int,
    val isAction: Boolean = false,
    var isExpanded: Boolean = false,
    val iconResId: Int? = null,
    val subItems: List<MenuItemData> = emptyList()
)
