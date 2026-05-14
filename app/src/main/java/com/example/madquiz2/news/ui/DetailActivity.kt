package com.example.madquiz2.news.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.madquiz2.R
import com.example.madquiz2.news.models.Article
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView


class DetailActivity : AppCompatActivity() {

    private var article: Article? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        article = intent.getParcelableExtra("article")
        if (article == null) {
            finish()
            return
        }

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvSource = findViewById<TextView>(R.id.tvSource)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvContent = findViewById<TextView>(R.id.tvContent)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val ivHeader = findViewById<ImageView>(R.id.ivHeader)
        val btnOpenFull = findViewById<Button>(R.id.btnOpenFull)

        tvTitle.text = article?.title ?: ""
        tvSource.text = article?.source?.name ?: ""
        tvDescription.text = article?.description ?: ""
        tvContent.text = article?.content ?: ""
        tvDate.text = article?.publishedAt ?: ""

        val imageUrl = article?.image
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_placeholder).into(ivHeader)
        } else {
            ivHeader.setImageResource(R.drawable.ic_placeholder)
        }

        btnOpenFull.setOnClickListener {
            article?.url?.let { url ->
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(i)
            }
        }
    }
}
