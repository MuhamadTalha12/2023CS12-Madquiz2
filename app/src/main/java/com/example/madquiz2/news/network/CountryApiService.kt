package com.example.madquiz2.news.network

import com.example.madquiz2.news.models.Country
import retrofit2.http.GET
import retrofit2.http.Query

interface CountryApiService {
    @GET("all")
    suspend fun getAllCountries(
        @Query("fields") fields: String = "name,cca2"
    ): List<Country>
}
