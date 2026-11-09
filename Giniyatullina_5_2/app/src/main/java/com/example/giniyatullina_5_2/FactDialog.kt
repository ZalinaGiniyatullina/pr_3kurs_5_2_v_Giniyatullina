package com.example.giniyatullina_5_2

import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

fun showFactDialog(
    activity: AppCompatActivity,
    fact: FactEntity?,
    onSave: (FactEntity) -> Unit
) {
    val view = LayoutInflater.from(activity).inflate(R.layout.dialog_fact, null)
    val etDate = view.findViewById<EditText>(R.id.etDate)
    val etYear = view.findViewById<EditText>(R.id.etYear)
    val etText = view.findViewById<EditText>(R.id.etText)

    if (fact != null) {
        etDate.setText(fact.date)
        etYear.setText(fact.year)
        etText.setText(fact.text)
    }

    AlertDialog.Builder(activity)
        .setTitle(if (fact == null) "Добавить факт" else "Редактировать факт")
        .setView(view)
        .setPositiveButton("Сохранить") { _, _ ->
            val date = etDate.text.toString().trim()
            val year = etYear.text.toString().trim()
            val text = etText.text.toString().trim()

            if (date.isEmpty() || text.isEmpty()) {
                Toast.makeText(activity, "Заполните дату и текст факта", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val yearValue = if (year.isEmpty()) "—" else year
            val item = if (fact == null) {
                FactEntity(date = date, year = yearValue, text = text)
            } else {
                fact.copy(date = date, year = yearValue, text = text)
            }
            onSave(item)
        }
        .setNegativeButton("Отмена", null)
        .show()
}
