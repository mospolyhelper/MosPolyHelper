package com.mospolytech.mospolyhelper.features.ui.addresses

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.mospolytech.mospolyhelper.R
import com.mospolytech.mospolyhelper.domain.addresses.model.Addresses

class AddressesAdapter(
    var addresses: List<String>,
    val type: String
) : RecyclerView.Adapter<AddressesAdapter.ViewHolder>() {
    override fun getItemCount() = addresses.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_address, parent, false);
        return ViewHolder(view);
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val spanned = HtmlCompat.fromHtml(addresses[position], HtmlCompat.FROM_HTML_MODE_LEGACY)
        viewHolder.text.setText(spanned, TextView.BufferType.NORMAL);
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById<TextView>(R.id.text);
    }

    class ItemDecoration(private val offset: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.top = offset;
        }
    }
}