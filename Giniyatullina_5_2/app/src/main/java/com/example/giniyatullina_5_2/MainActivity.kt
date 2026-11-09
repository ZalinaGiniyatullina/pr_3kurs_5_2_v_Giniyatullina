package com.example.giniyatullina_5_2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import java.util.GregorianCalendar

class MainActivity : AppCompatActivity() {

    private lateinit var queue: RequestQueue
    private lateinit var db: AppDatabase
    private lateinit var factsPrefs: FactsPrefs
    private lateinit var etDate: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var btnFetch: Button
    private lateinit var tvLastResult: TextView
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        queue = Volley.newRequestQueue(this)
        db = AppDatabase.get(this)
        factsPrefs = FactsPrefs(this)

        etDate = findViewById(R.id.etDate)
        progressBar = findViewById(R.id.progressBar)
        btnFetch = findViewById(R.id.btnFetch)
        tvLastResult = findViewById(R.id.tvLastResult)

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnFetch.setOnClickListener {
            val input = etDate.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Введите дату в формате ММ-ДД", Toast.LENGTH_SHORT).show()
            } else {
                fetch(input)
            }
        }

        findViewById<Button>(R.id.btnSaved).setOnClickListener {
            startActivity(Intent(this, FactsActivity::class.java))
        }

        findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    override fun onDestroy() {
        queue.cancelAll(TAG)
        super.onDestroy()
    }

    private fun isValidDate(month: Int, day: Int): Boolean {
        if (month !in 1..12 || day !in 1..31) return false
        return try {
            val cal = GregorianCalendar()
            cal.isLenient = false
            // 2000 — високосный год, поэтому 29 февраля тоже разрешено
            cal.set(2000, month - 1, day)
            cal.time
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun fetch(dateInput: String) {
        val parts = dateInput.split("-")
        if (parts.size != 2) {
            Toast.makeText(this, "Формат: ММ-ДД, например 07-20", Toast.LENGTH_SHORT).show()
            return
        }
        val month = parts[0].trim().toIntOrNull()
        val day = parts[1].trim().toIntOrNull()
        if (month == null || day == null || !isValidDate(month, day)) {
            Toast.makeText(this, "Нет такой даты", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedDate = "%02d-%02d".format(month, day)

        progressBar.visibility = View.VISIBLE
        btnFetch.isEnabled = false
        tvLastResult.text = ""

        val url = "https://ru.wikipedia.org/api/rest_v1/feed/onthisday/events/%02d/%02d"
            .format(month, day)

        val request = object : StringRequest(
            Request.Method.GET,
            url,
            { response -> finishFetch(parseFacts(response, normalizedDate), null) },
            { error ->
                finishFetch(emptyList(), error.message ?: "ошибка сети")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> = mutableMapOf(
                "User-Agent" to "GiniyatullinaFacts/1.0 (student Android app)"
            )
        }
        request.tag = TAG
        queue.add(request)
    }

    private fun parseFacts(json: String, date: String): List<FactEntity> {
        return try {
            val data = gson.fromJson(json, WikiOnThisDay::class.java)
            data?.events
                .orEmpty()
                .filter { !it.text.isNullOrBlank() }
                .shuffled()
                .map { event ->
                    FactEntity(
                        date = date,
                        year = event.year?.toString() ?: "—",
                        text = event.text?.trim().orEmpty()
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun finishFetch(facts: List<FactEntity>, error: String?) {
        progressBar.visibility = View.GONE
        btnFetch.isEnabled = true

        if (facts.isEmpty()) {
            Toast.makeText(
                this,
                if (error != null) "Ошибка: $error" else "Ничего не найдено",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        factsPrefs.saveFacts(facts)

        Thread {
            val ids = mutableListOf<Int>()
            facts.forEach { ids.add(db.dao().insert(it).toInt()) }
            val totalInPrefs = factsPrefs.loadFacts().size
            runOnUiThread {
                tvLastResult.text = "Найдено фактов: ${facts.size}."
                Toast.makeText(this, "Найдено фактов: ${facts.size}", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, FactsActivity::class.java)
                intent.putExtra(FactsActivity.EXTRA_IDS, ids.toIntArray())
                startActivity(intent)
            }
        }.start()
    }

    companion object {
        private const val TAG = "FACTS_API"
    }
}
