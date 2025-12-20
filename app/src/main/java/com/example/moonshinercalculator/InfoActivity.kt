package com.example.moonshinercalculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class InfoActivity : AppCompatActivity() {
    @SuppressLint("UseKtx", "QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info)

        // Применение отступов под системные панели
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Исправление: homeButton — это ImageView, а не Button
        val homeButton = findViewById<ImageView>(R.id.homeButton)
        homeButton.setOnClickListener {
            finish()
        }

        // Инициализация TextView для отображения информации
        val textView10 = findViewById<TextView>(R.id.textView10)
        textView10.text = buildString {
            appendLine("Программа разработана Алексеем Уфимцевым на бесплатной основе.")
            appendLine()
            appendLine("При разработке программы я воспользовался всеми формулами, находящимися в свободном доступе.")
            appendLine("а так-же формулами, которые выводил из формул в свободном доступе.")
            appendLine()
            appendLine("Однако не все эти формулы достаточно точные, собственно как и в большинстве онлайн-калькуляторов.")
            appendLine()
            appendLine("это тестовая версия программы. 0.1")
            appendLine("")
        }


    }
}