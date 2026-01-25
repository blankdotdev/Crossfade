package com.blankdev.crossfade.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blankdev.crossfade.R

class ItemMenuAdapter(
    private val onItemClick: (MenuItemData) -> Unit,
    private val onCopyClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var originalItems: List<MenuItemData> = emptyList()
    private var visibleItems: List<MenuItemData> = emptyList()
    private var recyclerView: RecyclerView? = null

    fun setData(items: List<MenuItemData>) {
        this.originalItems = items
        this.visibleItems = MenuItemUtils.rebuildVisibleList(items)
        // Safe update on next frame to avoid computeLayout glitches
        recyclerView?.post {
            notifyDataSetChanged()
        } ?: notifyDataSetChanged()
    }

    private fun toggleExpansion(item: MenuItemData) {
        val newList = originalItems.map {
            if (it.id == item.id) {
                it.copy(isExpanded = !it.isExpanded)
            } else {
                it
            }
        }
        setData(newList)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun getItemViewType(position: Int): Int {
        return visibleItems[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ShareBottomSheet.TYPE_SUBMENU_HEADER -> {
                val view = inflater.inflate(R.layout.item_menu_header, parent, false)
                HeaderVH(view, ::toggleExpansion)
            }
            ShareBottomSheet.TYPE_ACTION -> {
                val view = inflater.inflate(R.layout.item_menu_header, parent, false)
                ActionVH(view, onItemClick)
            }
            else -> { // ITEM or SUBMENU_ITEM
                val view = inflater.inflate(R.layout.item_menu_row, parent, false)
                ItemVH(view, onItemClick, onCopyClick, onShareClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = visibleItems[position]
        when (holder) {
            is HeaderVH -> holder.bind(item)
            is ActionVH -> holder.bind(item)
            is ItemVH -> holder.bind(item)
        }
    }

    override fun getItemCount() = visibleItems.size

    class HeaderVH(view: View, val onToggle: (MenuItemData) -> Unit) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.textTitle)
        private val imgArrow: android.widget.ImageView = view.findViewById(R.id.imgEndIcon)
        private var currentItem: MenuItemData? = null

        init {
            view.setOnClickListener { currentItem?.let { onToggle(it) } }
        }

        fun bind(item: MenuItemData) {
            currentItem = item
            title.text = item.title
            imgArrow.visibility = View.VISIBLE
            imgArrow.rotation = if (item.isExpanded) 90f else 0f
        }
    }

    class ActionVH(view: View, val onClick: (MenuItemData) -> Unit) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.textTitle)
        private val imgArrow: android.widget.ImageView = view.findViewById(R.id.imgEndIcon)
        private var currentItem: MenuItemData? = null

        init {
            view.setOnClickListener { currentItem?.let { onClick(it) } }
        }

        fun bind(item: MenuItemData) {
            currentItem = item
            title.text = item.title
            imgArrow.visibility = View.GONE
        }
    }

    class ItemVH(
        view: View,
        val onClick: (MenuItemData) -> Unit,
        val onCopy: (String) -> Unit,
        val onShare: (String) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.textTitle)
        private val imgIcon: android.widget.ImageView = view.findViewById(R.id.imgPlatformIcon)
        private val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
        private val btnShare: ImageButton = view.findViewById(R.id.btnShare)
        private val originalPaddingStart = view.paddingStart
        private var currentItem: MenuItemData? = null

        init {
            view.setOnClickListener { currentItem?.let { onClick(it) } }
        }

        fun bind(item: MenuItemData) {
            currentItem = item
            title.text = item.title

            if (item.url != null) {
                btnCopy.visibility = View.VISIBLE
                btnShare.visibility = View.VISIBLE
                btnCopy.setOnClickListener { onCopy(item.url) }
                btnShare.setOnClickListener { onShare(item.url) }
            } else {
                btnCopy.visibility = View.GONE
                btnShare.visibility = View.GONE
            }

            if (item.iconResId != null) {
                imgIcon.setImageResource(item.iconResId)
                imgIcon.visibility = View.VISIBLE
            } else {
                imgIcon.visibility = View.GONE
                imgIcon.setImageDrawable(null)
            }

            val density = itemView.resources.displayMetrics.density
            val indent = (if (item.type == ShareBottomSheet.TYPE_SUBMENU_ITEM) 32f else 0f) * density

            itemView.setPadding(
                (originalPaddingStart + indent).toInt(),
                itemView.paddingTop,
                itemView.paddingEnd,
                itemView.paddingBottom
            )
        }
    }
}
