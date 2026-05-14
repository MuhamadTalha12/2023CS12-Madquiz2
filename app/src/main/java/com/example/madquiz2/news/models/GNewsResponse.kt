package com.example.madquiz2.news.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class GNewsResponse(
    val totalArticles: Int = 0,
    val articles: List<Article> = emptyList()
)

@Parcelize
data class Article(
    val title: String? = null,
    val description: String? = null,
    val content: String? = null,
    val url: String? = null,
    val image: String? = null,
    val publishedAt: String? = null,
    val source: Source? = null
) : Parcelable

@Parcelize
data class Source(
    val name: String? = null
) : Parcelable
