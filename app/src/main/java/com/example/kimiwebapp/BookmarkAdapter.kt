package com.example.kimiwebapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BookmarkAdapter(
    private val bookmarks: List<Pair<String, String>>,
    private val onClick: (Int, String) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvBookmarkTitle)
        val tvUrl: TextView = itemView.findViewById(R.id.tvBookmarkUrl)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (title, url) = bookmarks[position]
        holder.tvTitle.text = title
        holder.tvUrl.text = url

        holder.itemView.setOnClickListener {
            onClick(position, url)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(position)
        }
    }

    override fun getItemCount() = bookmarks.size
}
