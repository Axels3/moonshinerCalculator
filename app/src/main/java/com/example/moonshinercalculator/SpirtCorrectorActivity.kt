package com.example.moonshinercalculator

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SpirtCorrectorActivity : AppCompatActivity() {

    private lateinit var temperatureEdit: EditText
    private lateinit var strengthEdit: EditText
    private lateinit var resultText: TextView

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spirt_corrector)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("SpirtCorrectorPrefs", MODE_PRIVATE)

        // Привязка элементов
        temperatureEdit = findViewById(R.id.temperatureEdit)
        strengthEdit = findViewById(R.id.strengthEdit)
        resultText = findViewById(R.id.resultText)

        // Кнопка "Назад"
        val homeButton = findViewById<ImageView>(R.id.homeButton)
        homeButton.setOnClickListener {
            finish()
        }

        // Восстановление сохранённых данных
        restoreState()

        // TextWatcher для автоматического пересчёта
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateCorrection()
            }
        }

        temperatureEdit.addTextChangedListener(textWatcher)
        strengthEdit.addTextChangedListener(textWatcher)

        calculateCorrection()
    }

    @SuppressLint("SetTextI18n")
    private fun calculateCorrection() {
        val tempStr = temperatureEdit.text.toString().trim()
        val strengthStr = strengthEdit.text.toString().trim()

        if (tempStr.isEmpty() || strengthStr.isEmpty()) {
            resultText.text = "⚠️• Введите температуру и показания спиртометра"
            return
        }

        val temperature = tempStr.toDoubleOrNull()
        val strength = strengthStr.toDoubleOrNull()

        if (temperature == null || strength == null) {
            resultText.text = "⚠️• Некорректный ввод"
            return
        }

        if (temperature !in 0.0..100.0) {
            resultText.text = "⚠️• Температура должна быть от 0 до 100°C"
            return
        }

        if (strength !in 0.0..100.0) {
            resultText.text = "⚠️• Крепость должна быть от 0 до 100%"
            return
        }

        // Коррекция крепости к 20°C
        val correctedStrength = strength + (20 - temperature) * 0.3

        val result = "Реальная крепость при 20°C: %.1f %%".format(correctedStrength)
        resultText.text = result
    }

    // Сохранение состояния
    private fun saveState() {
        sharedPreferences.edit {
            putString("temperature", temperatureEdit.text.toString())
            putString("strength", strengthEdit.text.toString())
        }
    }

    // Восстановление состояния
    private fun restoreState() {
        val temperature = sharedPreferences.getString("temperature", "")
        val strength = sharedPreferences.getString("strength", "")
        temperatureEdit.setText(temperature)
        strengthEdit.setText(strength)
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }
}