package com.example.giniyatullina_5_2

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: FactAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var etDate: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.get(this)
        progressBar = findViewById(R.id.progressBar)
        etDate = findViewById(R.id.etDate)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val btnFetch = findViewById<Button>(R.id.btnFetch)

        adapter = FactAdapter(
            onDelete = { fact -> deleteFact(fact) },
            onEdit = { fact -> editFact(fact) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadFacts()

        btnFetch.setOnClickListener {
            val input = etDate.text.toString()
            if (input.isEmpty()) {
                Toast.makeText(this, "Введите дату в формате ММ-ДД", Toast.LENGTH_SHORT).show()
            } else {
                fetch(input)
            }
        }
    }

    /** Чтение из Room в фоновом потоке */
    private fun loadFacts() {
        Thread {
            val list = db.dao().getAll()
            runOnUiThread { adapter.submit(list) }
        }.start()
    }

    /** Запрос к History API через HttpURLConnection */
    private fun fetch(dateInput: String) {
        val parts = dateInput.split("-")
        if (parts.size != 2) {
            Toast.makeText(this, "Формат: ММ-ДД", Toast.LENGTH_SHORT).show()
            return
        }
        val month = parts[0].toIntOrNull()
        val day = parts[1].toIntOrNull()
        if (month == null || day == null) {
            Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE

        Thread {
            var errorMsg: String? = null
            val facts = mutableListOf<FactEntity>()

            try {
                val url = URL("https://history.muffinlabs.com/v1/history?month=$month&day=$day")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val events = root
                        .getJSONObject("data")
                        .optJSONArray("Events")

                    if (events != null && events.length() > 0) {
                        // Берём до 5 событий
                        val count = minOf(events.length(), 5)
                        for (i in 0 until count) {
                            val ev = events.getJSONObject(i)
                            facts.add(
                                FactEntity(
                                    date = dateInput,
                                    year = ev.optString("year", "—"),
                                    text = ev.optString("text", "Без описания")
                                )
                            )
                        }
                    }
                } else {
                    errorMsg = "HTTP ${conn.responseCode}"
                }
                conn.disconnect()
            } catch (e: Exception) {
                errorMsg = e.message ?: "Неизвестная ошибка"
            }

            // Возвращаемся в UI-поток
            runOnUiThread {
                progressBar.visibility = View.GONE
                if (errorMsg != null) {
                    Toast.makeText(this, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                } else if (facts.isEmpty()) {
                    Toast.makeText(this, "Ничего не найдено", Toast.LENGTH_SHORT).show()
                } else {
                    // Сохраняем в Room (фоново) и обновляем список
                    Thread {
                        facts.forEach { db.dao().insert(it) }
                        val all = db.dao().getAll()
                        runOnUiThread { adapter.submit(all) }
                    }.start()
                    Toast.makeText(this, "Добавлено фактов: ${facts.size}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun deleteFact(fact: FactEntity) {
        Thread {
            db.dao().delete(fact)
            val all = db.dao().getAll()
            runOnUiThread { adapter.submit(all) }
        }.start()
    }

    private fun editFact(fact: FactEntity) {
        val et = EditText(this).apply {
            setText(fact.text)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(32, 32, 32, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Редактировать факт")
            .setView(et)
            .setPositiveButton("Сохранить") { _, _ ->
                val updated = fact.copy(text = et.text.toString())
                Thread {
                    db.dao().update(updated)
                    val all = db.dao().getAll()
                    runOnUiThread { adapter.submit(all) }
                }.start()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

/** Адаптер тоже здесь, чтобы не плодить файлы */
class FactAdapter(
    private val onDelete: (FactEntity) -> Unit,
    private val onEdit: (FactEntity) -> Unit
) : RecyclerView.Adapter<FactAdapter.VH>() {

    private var items: List<FactEntity> = emptyList()

    fun submit(newItems: List<FactEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_fact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDate: TextView = view.findViewById(R.id.tvDate)
        private val tvText: TextView = view.findViewById(R.id.tvText)

        fun bind(fact: FactEntity) {
            tvDate.text = "Дата: ${fact.date} | Год: ${fact.year}"
            tvText.text = fact.text
            itemView.findViewById<Button>(R.id.btnDelete).setOnClickListener { onDelete(fact) }
            itemView.findViewById<Button>(R.id.btnEdit).setOnClickListener { onEdit(fact) }
        }
    }
}