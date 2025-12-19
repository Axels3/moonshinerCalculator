package com.example.moonshinercalculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SpirtCorrectorActivity : AppCompatActivity() {

    private lateinit var temperatureEdit: EditText
    private lateinit var strengthEdit: EditText
    private lateinit var resultText: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spirt_corrector)

        temperatureEdit = findViewById(R.id.temperatureEdit)
        strengthEdit = findViewById(R.id.strengthEdit)
        resultText = findViewById(R.id.resultText)

        // Установка значения по умолчанию
        if (temperatureEdit.text.isEmpty()) {
            temperatureEdit.setText("20")
        }

        // Кнопка "Назад"
        val homeButton = findViewById<ImageView>(R.id.homeButton)
        homeButton.setOnClickListener {
            finish()
        }

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

    private fun calculateCorrection() {
        val tempStr = temperatureEdit.text.toString().trim()
        val strengthStr = strengthEdit.text.toString().trim()

        if (tempStr.isEmpty() || strengthStr.isEmpty()) {
            resultText.text = "Введите температуру и показания спиртометра"
            return
        }

        val temperature = tempStr.toDoubleOrNull()
        val strength = strengthStr.toDoubleOrNull()

        if (temperature == null || strength == null) {
            resultText.text = "Некорректный ввод"
            return
        }

        if (temperature !in 0.0..100.0) {
            resultText.text = ""
            return
        }

        if (strength !in 0.0..100.0) {
            resultText.text = "Температура должна быть от 0 до 100°C"
            return
        }

        // Коррекция крепости к 20°C
        val correctedStrength = strength + (20 - temperature) * 0.3

        val result = "Реальная крепость при 20°C: %.2f %%".format(correctedStrength)
        resultText.text = result
    }
}