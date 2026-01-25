package com.blankdev.crossfade.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankdev.crossfade.R
import com.blankdev.crossfade.utils.DefaultHandlerChecker

data class DefaultCheckItem(
    val platformId: String,
    val name: String,
    val iconRes: Int,
    val status: DefaultHandlerChecker.ServiceStatus,
    val isConflict: Boolean = false,
    val isPreferred: Boolean = false
)

class DefaultsAdapter(
    private var items: List<DefaultCheckItem> = emptyList()
) : RecyclerView.Adapter<DefaultsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusDot: View = view.findViewById(R.id.statusDot)
        val icon: ImageView = view.findViewById(R.id.ivServiceIcon)
        val name: TextView = view.findViewById(R.id.tvServiceName)
        val preferredIndicator: ImageView = view.findViewById(R.id.ivPreferredIndicator)
        val statusText: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_default_check, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.name.text = item.name
        holder.icon.setImageResource(item.iconRes)
        holder.preferredIndicator.visibility = if (item.isPreferred) View.VISIBLE else View.GONE
        
        val (statusString, colorRes) = if (item.isConflict) {
             // Conflict case: Use true status text, but Red color
             val text = when (item.status) {
                 DefaultHandlerChecker.ServiceStatus.ACTIVE -> "Active"
                 DefaultHandlerChecker.ServiceStatus.PARTIAL -> "Partial"
                 else -> "Inactive"
             }
             text to R.color.pastel_red
        } else {
            when (item.status) {
                DefaultHandlerChecker.ServiceStatus.ACTIVE -> "Active" to R.color.pastel_green
                DefaultHandlerChecker.ServiceStatus.PARTIAL -> "Partial" to R.color.pastel_orange
                DefaultHandlerChecker.ServiceStatus.INACTIVE -> "Inactive" to R.color.pastel_red
            }
        }
        
        holder.statusText.text = statusString
        holder.statusDot.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, colorRes)
        )
        
        if (item.isConflict) {
            holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.pastel_red))
        } else {
             holder.statusText.setTextColor(ContextCompat.getColor(context, R.color.material_on_surface_variant)) /* Default or Custom */
             // Revert to default text header color if needed or keep standard
             // Since we don't know the exact default, let's trust the XML default unless conflict
             // But wait, view holders constitute recycled views. We MUST reset color if not conflict.
             val typedValue = android.util.TypedValue()
             context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
             holder.statusText.setTextColor(typedValue.data)
        }
        
        // Special handling for Conflicts (Red Traffic Light for Active/Partial preferred apps)
        // If the item made it to this list and it IS the preferred app (or we add a flag later),
        // filtering logic happens in the BottomSheet, but if we need visual override here we can add it.
        // For now, based on requirements, if an item is listed here it follows standard rules
        // UNLESS it's the preferred app "exceptionally in red".
        // The implementation plan put the filtering logic in the BottomSheet.
        // We can add a property 'isConflict' to checking later if needed.
    }
    
    override fun getItemCount() = items.size

    fun updateData(newItems: List<DefaultCheckItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
