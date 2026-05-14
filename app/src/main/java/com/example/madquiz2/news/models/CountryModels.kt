package com.example.madquiz2.news.models

data class Country(
    val cca2: String? = null,
    val name: Name? = null
)

data class Name(
    val common: String? = null
)

data class CountryItem(
    val code: String,
    val name: String
)
