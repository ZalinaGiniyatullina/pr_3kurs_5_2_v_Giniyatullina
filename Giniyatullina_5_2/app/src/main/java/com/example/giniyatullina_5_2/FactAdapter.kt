package com.example.giniyatullina_5_2

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FactAdapter(
    private val showFavoriteButton: Boolean,
    private val onFavorite: (FactEntity) -> Unit,
    private val onEdit: (FactEntity) -> Unit,
    private val onDelete: (FactEntity) -> Unit
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
        private val btnFavorite: Button = view.findViewById(R.id.btnFavorite)
        private val btnEdit: Button = view.findViewById(R.id.btnEdit)
        private val btnDelete: Button = view.findViewById(R.id.btnDelete)

        fun bind(fact: FactEntity) {
            tvDate.text = "Дата: ${fact.date} | Год: ${fact.year}"
            tvText.text = fact.text

            btnFavorite.visibility = if (showFavoriteButton) View.VISIBLE else View.GONE
            btnFavorite.text = "*"

            // Цвет кнопки показывает состояние:
            // оранжевая — факт в избранном, серая — ещё нет
            if (fact.isFavorite) {
                btnFavorite.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#F9A825"))
                btnFavorite.setTextColor(Color.WHITE)
            } else {
                btnFavorite.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#CFD8DC"))
                btnFavorite.setTextColor(Color.parseColor("#1A237E"))
            }

            btnFavorite.contentDescription =
                if (fact.isFavorite) "Убрать из избранного" else "Добавить в избранное"

            btnFavorite.setOnClickListener { onFavorite(fact) }
            btnEdit.setOnClickListener { onEdit(fact) }
            btnDelete.setOnClickListener { onDelete(fact) }
        }
    }
}
