package com.example.giniyatullina_5_2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class FactsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: FactAdapter
    private lateinit var tvEmpty: TextView

    private var filterIds: List<Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facts)

        db = AppDatabase.get(this)
        tvEmpty = findViewById(R.id.tvEmpty)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        val ids = intent.getIntArrayExtra(EXTRA_IDS)?.toList()
        if (!ids.isNullOrEmpty()) {
            filterIds = ids
            tvTitle.text = "Результаты поиска"
            tvEmpty.text = "По этой дате ничего не нашлось.\nПопробуйте другую дату."
        }

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        adapter = FactAdapter(
            showFavoriteButton = true,
            onFavorite = { toggleFavorite(it) },
            onEdit = { fact -> showFactDialog(this, fact) { updated -> updateFact(updated) } },
            onDelete = { deleteFact(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showFactDialog(this, null) { fact -> insertFact(fact) }
        }

        findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadFacts()
    }

    private fun loadFacts() {
        Thread {
            val list = queryList()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun queryList(): List<FactEntity> {
        val ids = filterIds
        return if (ids == null) db.dao().getAll() else db.dao().getByIds(ids)
    }

    private fun showList(list: List<FactEntity>) {
        adapter.submit(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun insertFact(fact: FactEntity) {
        Thread {
            val newId = db.dao().insert(fact).toInt()
            val ids = filterIds
            if (ids != null) filterIds = ids + newId

            val list = queryList()
            runOnUiThread {
                showList(list)
                Toast.makeText(this, "Факт добавлен", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun updateFact(fact: FactEntity) {
        Thread {
            db.dao().update(fact)
            val list = queryList()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun toggleFavorite(fact: FactEntity) {
        Thread {
            db.dao().update(fact.copy(isFavorite = !fact.isFavorite))
            val list = queryList()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun deleteFact(fact: FactEntity) {
        Thread {
            db.dao().delete(fact)
            val list = queryList()
            runOnUiThread { showList(list) }
        }.start()
    }

    companion object {
        const val EXTRA_IDS = "extra_fact_ids"
    }
}
