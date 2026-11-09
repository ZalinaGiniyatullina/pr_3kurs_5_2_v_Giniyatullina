package com.example.giniyatullina_5_2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val auth = AuthManager(this)
        val etName = findViewById<EditText>(R.id.etName)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val name = etName.text.toString().trim()
            val password = etPassword.text.toString()

            var hasError = false
            if (name.isEmpty()) {
                etName.error = "Введите имя"
                hasError = true
            }
            if (password.isEmpty()) {
                etPassword.error = "Введите пароль"
                hasError = true
            }
            if (hasError) {
                Toast.makeText(this, "Заполните имя и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val saved = auth.getUser()

            when {
                saved == null -> {
                    auth.saveUser(User(name, password))
                    enter(name)
                }

                saved.name == name && saved.password.isNullOrEmpty() -> {
                    auth.saveUser(User(name, password))
                    enter(name)
                }

                saved.name != name -> {
                    etName.error = "Пользователь с таким именем не найден"
                    Toast.makeText(this, "Неверное имя пользователя", Toast.LENGTH_SHORT).show()
                }

                saved.password != password -> {
                    etPassword.error = "Неверный пароль"
                    Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
                }

                else -> enter(name)
            }
        }
    }

    private fun enter(name: String) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
