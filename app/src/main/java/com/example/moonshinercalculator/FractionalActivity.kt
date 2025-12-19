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

class FractionalActivity : AppCompatActivity() {

    private lateinit var distillateVolumeEdit: EditText
    private lateinit var distillateStrengthEdit: EditText
    private lateinit var tempEdit: EditText
    private lateinit var headsPercentEdit: EditText
    private lateinit var tailsPercentEdit: EditText
    private lateinit var distillateItogEdit: EditText
    private lateinit var resultText: TextView

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fractional)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("FractionalPrefs", MODE_PRIVATE)

        // Инициализация полей
        distillateVolumeEdit = findViewById(R.id.distillateVolumeEdit)
        distillateStrengthEdit = findViewById(R.id.distillateStrengthEdit)
        tempEdit = findViewById(R.id.tempEdit)
        headsPercentEdit = findViewById(R.id.headsPercentEdit)
        tailsPercentEdit = findViewById(R.id.tailsPercentEdit)
        distillateItogEdit = findViewById(R.id.distillateitogEdit)
        resultText = findViewById(R.id.resultText)

        // Восстановление сохранённых значений
        restoreState()

        // Установка значений по умолчанию
        if (headsPercentEdit.text.isEmpty()) headsPercentEdit.setText("10")
        if (tailsPercentEdit.text.isEmpty()) tailsPercentEdit.setText("15")
        if (tempEdit.text.isEmpty()) tempEdit.setText("20")
        if (distillateItogEdit.text.isEmpty()) distillateItogEdit.setText("40")

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
                calculateFractional()
                saveState()
            }
        }

        distillateVolumeEdit.addTextChangedListener(textWatcher)
        distillateStrengthEdit.addTextChangedListener(textWatcher)
        tempEdit.addTextChangedListener(textWatcher)
        headsPercentEdit.addTextChangedListener(textWatcher)
        tailsPercentEdit.addTextChangedListener(textWatcher)
        distillateItogEdit.addTextChangedListener(textWatcher)

        // Первичный расчёт
        calculateFractional()
    }

    @SuppressLint("SetTextI18n")
    private fun calculateFractional() {
        val volumeStr = distillateVolumeEdit.text.toString().trim()
        val strengthStr = distillateStrengthEdit.text.toString().trim()
        val tempStr = tempEdit.text.toString().trim()
        val headsPercentStr = headsPercentEdit.text.toString().trim()
        val tailsPercentStr = tailsPercentEdit.text.toString().trim()
        val itogStrengthStr = distillateItogEdit.text.toString().trim()

        if (volumeStr.isEmpty() || strengthStr.isEmpty() || tempStr.isEmpty() || itogStrengthStr.isEmpty()) {
            resultText.text = "Введите все данные"
            return
        }

        val volume = volumeStr.toDoubleOrNull()
        val strength = strengthStr.toDoubleOrNull()
        val temp = tempStr.toDoubleOrNull()
        val headsPercent = headsPercentStr.toDoubleOrNull() ?: 10.0
        val tailsPercent = tailsPercentStr.toDoubleOrNull() ?: 15.0
        val itogStrength = itogStrengthStr.toDoubleOrNull() ?: 40.0

        if (volume == null || strength == null || temp == null) {
            resultText.text = "Некорректный ввод чисел"
            return
        }

        if (volume <= 0) {
            resultText.text = "Объём спирта должен быть больше нуля"
            return
        }

        if (strength !in 0.0..100.0) {
            resultText.text = "Крепость спирта: от 0 до 100%"
            return
        }

        if (temp !in -10.0..100.0) {
            resultText.text = "Температура: от -10 до 100°C"
            return
        }

        if (itogStrength !in 0.1..100.0) {
            resultText.text = "Крепость итога: 0.1–100%"
            return
        }

        // Проверка: сумма голов и хвостов не должна превышать 100%
        if (headsPercent + tailsPercent >= 100) {
            resultText.text = "Сумма голов и хвостов < 100%"
            return
        }

        // Коррекция крепости по температуре (приведение к 20°C)
        val correctedStrength = strength + (20 - temp) * 0.04

        if (correctedStrength <= 0 || correctedStrength > 100) {
            resultText.text = "Ошибка коррекции: некорректная крепость"
            return
        }

        // 1. Абсолютный спирт: АС = СС × К0 / 100
        val absoluteAlcohol = volume * correctedStrength / 100.0

        // 2. Головы: Г = АС × g / 100
        val headsVolume = absoluteAlcohol * headsPercent / 100.0

        // 3. Хвосты: Х = АС × h / 100
        val tailsVolume = absoluteAlcohol * tailsPercent / 100.0

        // 4. Тело (основная фракция)
        val bodyVolume = volume - headsVolume - tailsVolume

        // 5. Объём итогового продукта указанной крепости (разбавление тела)
        // V_итог = (объём_тела × крепость_тела) / целевая_крепость
        val bodyStrength = correctedStrength // упрощение: крепость тела ≈ исходной (можно уточнить позже)
        val itogVolume = (bodyVolume * bodyStrength) / itogStrength



        // 7. Форматирование результата
        val correctedLine = "Скорректированная крепость: ${"%.1f".format(correctedStrength)} %об."
        val absoluteLine = "Абсолютный спирт: ${"%.2f".format(absoluteAlcohol)} л"
        val headsLine = "Головы: ${"%.2f".format(headsVolume)} л"
        val tailsLine = "Хвосты: ${"%.2f".format(tailsVolume)} л"
        val itogLine = "Продукт: $itogStrength%об: ${"%.2f".format(itogVolume)} л "

        resultText.text = "$correctedLine\n$absoluteLine\n$headsLine\n$tailsLine\n$itogLine"
    }

    // Сохранение состояния
    private fun saveState() {
        sharedPreferences.edit {
            putString("distillate_volume", distillateVolumeEdit.text.toString())
            putString("distillate_strength", distillateStrengthEdit.text.toString())
            putString("temperature", tempEdit.text.toString())
            putString("heads_percent", headsPercentEdit.text.toString())
            putString("tails_percent", tailsPercentEdit.text.toString())
            putString("itog_strength", distillateItogEdit.text.toString())
        }
    }

    // Восстановление состояния
    private fun restoreState() {
        distillateVolumeEdit.setText(sharedPreferences.getString("distillate_volume", ""))
        distillateStrengthEdit.setText(sharedPreferences.getString("distillate_strength", ""))
        tempEdit.setText(sharedPreferences.getString("temperature", ""))
        headsPercentEdit.setText(sharedPreferences.getString("heads_percent", ""))
        tailsPercentEdit.setText(sharedPreferences.getString("tails_percent", ""))
        distillateItogEdit.setText(sharedPreferences.getString("itog_strength", ""))
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }
}