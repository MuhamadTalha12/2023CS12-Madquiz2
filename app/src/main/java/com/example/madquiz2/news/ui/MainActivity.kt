package com.example.madquiz2.news.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.madquiz2.R
import com.example.madquiz2.news.models.Article
import com.example.madquiz2.news.models.CountryItem
import com.example.madquiz2.news.network.ApiClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val adapter by lazy { NewsAdapter(mutableListOf(), this::onArticleClicked) }
    private var countries: List<CountryItem> = emptyList()
    private var isInitializingSpinner = false
    private var lastSpinnerPosition: Int = -1
    private var pendingNewsJob: Job? = null
    private var activeNewsJob: Job? = null
    private var lastRequestedCountry: String? = null
    private var lastRateLimitAt: Long = 0L

    // TODO: Provide your GNews API key here
    private val GNEWS_API_KEY = "f569d93868421ebac9f44faaeef56b94"
    private val newsDebounceMs = 500L
    private val rateLimitCooldownMs = 30000L
    private val supportedCountryCodes = setOf(
        "ar", "au", "at", "be", "br", "bg", "ca", "cn", "co", "cu", "cz", "eg",
        "fr", "de", "gr", "hk", "hu", "in", "id", "ie", "il", "it", "jp", "lv",
        "lt", "my", "mx", "ma", "nl", "nz", "ng", "no", "ph", "pl", "pt", "ro",
        "ru", "sa", "rs", "sg", "sk", "si", "za", "kr", "se", "ch", "tw", "th",
        "tr", "ae", "ua", "gb", "us", "ve", "pk"
    )
    private val prioritizedCountryCodes = listOf("il", "de", "pk")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val spinnerCountries = findViewById<Spinner>(R.id.spinnerCountries)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            requestNewsForSelectedCountry(immediate = true)
        }

        btnRefresh.setOnClickListener {
            requestNewsForSelectedCountry(immediate = true)
        }

        // store views on fields via tags for use in other methods
        this.spinnerCountries = spinnerCountries
        this.progressBar = progressBar
        this.swipeRefresh = swipeRefresh

        loadCountries()
    }

    private fun onArticleClicked(article: Article) {
        val i = Intent(this, DetailActivity::class.java)
        i.putExtra("article", article)
        startActivity(i)
    }

    // views stored for use across methods
    private lateinit var spinnerCountries: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private fun loadCountries() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    ApiClient.countryService.getAllCountries()
                }
                val mapped = list.mapNotNull { c ->
                    val code = c.cca2
                    val name = c.name?.common
                    if (!code.isNullOrBlank() && !name.isNullOrBlank()) {
                        CountryItem(code = code.lowercase(Locale.getDefault()), name = name)
                    } else null
                }.sortedBy { it.name }

                val finalCountries = when {
                    mapped.isNotEmpty() -> prioritizeCountries(filterSupportedCountries(mapped))
                    else -> prioritizeCountries(filterSupportedCountries(buildFallbackCountries()))
                }

                countries = finalCountries
                val display = countries.map { it.name }
                val spinnerAdapter = ArrayAdapter(this@MainActivity, R.layout.spinner_item, display)
                spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerCountries.adapter = spinnerAdapter

                val index = countries.indexOfFirst { it.code.equals("il", ignoreCase = true) }.let { if (it >= 0) it else 0 }
                isInitializingSpinner = true
                lastSpinnerPosition = index
                spinnerCountries.setSelection(index)
                spinnerCountries.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (isInitializingSpinner || position == lastSpinnerPosition) {
                            return
                        }
                        lastSpinnerPosition = position
                        scheduleNewsLoad(countries[position].code)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                progressBar.visibility = View.GONE
                isInitializingSpinner = false
                scheduleNewsLoad(countries[index].code, immediate = true)
            } catch (e: Exception) {
                val fallback = prioritizeCountries(filterSupportedCountries(buildFallbackCountries()))
                if (fallback.isNotEmpty()) {
                    countries = fallback
                    val display = countries.map { it.name }
                    val spinnerAdapter = ArrayAdapter(this@MainActivity, R.layout.spinner_item, display)
                    spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    spinnerCountries.adapter = spinnerAdapter
                    val index = countries.indexOfFirst { it.code.equals("il", ignoreCase = true) }.let { if (it >= 0) it else 0 }
                    isInitializingSpinner = true
                    lastSpinnerPosition = index
                    spinnerCountries.setSelection(index)
                    spinnerCountries.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (isInitializingSpinner || position == lastSpinnerPosition) {
                                return
                            }
                            lastSpinnerPosition = position
                            scheduleNewsLoad(countries[position].code)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                    isInitializingSpinner = false
                    scheduleNewsLoad(countries[index].code, immediate = true)
                    Toast.makeText(this@MainActivity, "Using offline country list", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to load countries: ${e.message}", Toast.LENGTH_LONG).show()
                }
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun buildFallbackCountries(): List<CountryItem> {
        return Locale.getISOCountries()
            .mapNotNull { code ->
                val name = Locale("", code).displayCountry
                if (name.isNullOrBlank()) null else CountryItem(code = code.lowercase(Locale.getDefault()), name = name)
            }
            .sortedBy { it.name }
    }

    private fun filterSupportedCountries(source: List<CountryItem>): List<CountryItem> {
        return source.filter { supportedCountryCodes.contains(it.code.lowercase(Locale.getDefault())) }
    }

    private fun prioritizeCountries(source: List<CountryItem>): List<CountryItem> {
        val normalized = source.map { it.copy(code = it.code.lowercase(Locale.getDefault())) }
        val byCode = normalized.associateBy { it.code }
        val prioritized = prioritizedCountryCodes.mapNotNull { byCode[it] }
        val remaining = normalized.filter { it.code !in prioritizedCountryCodes }
        return prioritized + remaining
    }

    private fun requestNewsForSelectedCountry(immediate: Boolean) {
        val pos = spinnerCountries.selectedItemPosition
        if (countries.isEmpty() || pos < 0 || pos >= countries.size) {
            Toast.makeText(this, "No country selected", Toast.LENGTH_SHORT).show()
            return
        }
        scheduleNewsLoad(countries[pos].code, immediate)
    }

    private fun scheduleNewsLoad(code: String, immediate: Boolean = false) {
        if (!immediate && code == lastRequestedCountry) {
            return
        }
        pendingNewsJob?.cancel()
        pendingNewsJob = lifecycleScope.launch {
            if (!immediate) {
                delay(newsDebounceMs)
            }
            loadNewsForCountry(code)
        }
    }

    private fun loadNewsForCountry(code: String) {
        val now = System.currentTimeMillis()
        if (now - lastRateLimitAt < rateLimitCooldownMs) {
            Toast.makeText(this, "Rate limited. Please wait a moment.", Toast.LENGTH_SHORT).show()
            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
            return
        }

        lastRequestedCountry = code
        progressBar.visibility = View.VISIBLE
        swipeRefresh.isRefreshing = true

        activeNewsJob?.cancel()
        activeNewsJob = lifecycleScope.launch {
            try {
                val newsResponse = withContext(Dispatchers.IO) {
                    ApiClient.newsService.getTopHeadlines(country = code, apiKey = GNEWS_API_KEY, max = 10)
                }
                adapter.setArticles(newsResponse.articles)
            } catch (e: HttpException) {
                when (e.code()) {
                    400, 404 -> {
                        try {
                            val fallback = withContext(Dispatchers.IO) {
                                ApiClient.newsService.getTopHeadlines(country = null, apiKey = GNEWS_API_KEY, max = 10)
                            }
                            adapter.setArticles(fallback.articles)
                            Toast.makeText(this@MainActivity, "No headlines for that country. Showing global news.", Toast.LENGTH_LONG).show()
                        } catch (fallbackError: Exception) {
                            Toast.makeText(this@MainActivity, "Failed to load global news: ${fallbackError.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    429 -> {
                        lastRateLimitAt = System.currentTimeMillis()
                        Toast.makeText(this@MainActivity, "Rate limited by news API. Try again soon.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(this@MainActivity, "Failed to load news: ${e.message()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load news: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }
}
