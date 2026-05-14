package com.example.madquiz2.news.network

import com.example.madquiz2.news.models.GNewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
           @Query("country") country: String?,
        @Query("max") max: Int = 10,
        @Query("lang") lang: String = "en",
        @Query("category") category: String = "general",
        @Query("apikey") apiKey: String
    ): GNewsResponse
}
