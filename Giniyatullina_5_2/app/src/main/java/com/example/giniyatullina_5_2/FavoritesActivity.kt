package com.example.giniyatullina_5_2

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: FactAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        db = AppDatabase.get(this)
        tvEmpty = findViewById(R.id.tvEmpty)

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        adapter = FactAdapter(
            showFavoriteButton = true,
            onFavorite = { removeFromFavorites(it) },
            onEdit = { fact -> showFactDialog(this, fact) { updated -> updateFact(updated) } },
            onDelete = { deleteFact(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        Thread {
            val list = db.dao().getFavorites()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun showList(list: List<FactEntity>) {
        adapter.submit(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateFact(fact: FactEntity) {
        Thread {
            db.dao().update(fact)
            val list = db.dao().getFavorites()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun removeFromFavorites(fact: FactEntity) {
        Thread {
            db.dao().update(fact.copy(isFavorite = false))
            val list = db.dao().getFavorites()
            runOnUiThread {
                showList(list)
                Toast.makeText(this, "Убрано из избранного", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun deleteFact(fact: FactEntity) {
        Thread {
            db.dao().delete(fact)
            val list = db.dao().getFavorites()
            runOnUiThread { showList(list) }
        }.start()
    }
}
