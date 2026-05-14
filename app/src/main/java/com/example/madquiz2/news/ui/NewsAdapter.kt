package com.example.madquiz2.news.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.madquiz2.R
import com.example.madquiz2.news.models.Article
import java.text.SimpleDateFormat
import java.util.*

class NewsAdapter(
    private val items: MutableList<Article> = mutableListOf(),
    private val onItemClick: (Article) -> Unit
) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    fun setArticles(newItems: List<Article>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(v)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = items[position]
        holder.bind(article)
        holder.itemView.setOnClickListener { onItemClick(article) }
    }

    override fun getItemCount(): Int = items.size

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivImage: ImageView = view.findViewById(R.id.ivImage)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSource: TextView = view.findViewById(R.id.tvSource)
        private val tvDate: TextView = view.findViewById(R.id.tvDate)

        fun bind(article: Article) {
            tvTitle.text = article.title ?: "No Title"
            tvSource.text = article.source?.name ?: "Unknown"
            tvDate.text = formatDate(article.publishedAt)
            val imageUrl = article.image
            if (!imageUrl.isNullOrEmpty()) {
                val glideUrl = GlideUrl(
                    imageUrl,
                    LazyHeaders.Builder()
                        .addHeader("Accept", "image/*")
                        .build()
                )
                Glide.with(itemView.context)
                    .load(glideUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivImage)
            } else {
                ivImage.setImageResource(R.drawable.ic_placeholder)
            }
        }

        private fun formatDate(raw: String?): String {
            if (raw.isNullOrEmpty()) return ""
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
                val outputFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM, Locale.getDefault())
                val date = inputFormat.parse(raw)
                if (date != null) outputFormat.format(date) else raw
            } catch (e: Exception) {
                raw
            }
        }
    }
}
