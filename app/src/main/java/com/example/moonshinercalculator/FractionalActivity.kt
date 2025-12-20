package com.example.moonshinercalculator

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat

class FractionalActivity : AppCompatActivity() {

    private lateinit var distillateVolumeEdit: EditText
    private lateinit var distillateStrengthEdit: EditText
    private lateinit var tempEdit: EditText
    private lateinit var headsPercentEdit: EditText
    private lateinit var tailsPercentEdit: EditText
    private lateinit var distillateItogEdit: EditText
    private lateinit var resultText: TextView

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_fractional)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("FractionalPrefs", MODE_PRIVATE)

        // Привязка элементов
        distillateVolumeEdit = findViewById(R.id.distillateVolumeEdit)
        distillateStrengthEdit = findViewById(R.id.distillateStrengthEdit)
        tempEdit = findViewById(R.id.tempEdit)
        headsPercentEdit = findViewById(R.id.headsPercentEdit)
        tailsPercentEdit = findViewById(R.id.tailsPercentEdit)
        distillateItogEdit = findViewById(R.id.distillateitogEdit)
        resultText = findViewById(R.id.resultText)

        // Кнопка "Назад"
        val backButton = findViewById<ImageView>(R.id.homeButton)
        backButton.setOnClickListener { finish() }

        // Восстановление состояния
        restoreState()

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

        calculateFractional()
    }

    private fun calculateFractional() {
        val volumeStr = distillateVolumeEdit.text.toString().trim()
        val strengthStr = distillateStrengthEdit.text.toString().trim()
        val tempStr = tempEdit.text.toString().trim()
        val headsPercentStr = headsPercentEdit.text.toString().trim()
        val tailsPercentStr = tailsPercentEdit.text.toString().trim()
        val itogStrengthStr = distillateItogEdit.text.toString().trim()

        val errors = mutableListOf<String>()
        var temp = 20.0 // по умолчанию
        var itogStrength = 40.0 // по умолчанию
        var headsPercent = 0.0 // по умолчанию 0, если не указано
        var tailsPercent = 0.0 // по умолчанию 0, если не указано
        var hasTempWarning = false
        var hasItogWarning = false
        var hasHeadsEmpty = false
        var hasTailsEmpty = false

        // Проверка объёма
        val volume = volumeStr.toDoubleOrNull()
        if (volumeStr.isEmpty()) {
            errors.add("⚠️• Объём спирта не указан")
        } else if (volume == null) {
            errors.add("⚠️• Некорректный объём спирта")
        } else if (volume <= 0) {
            errors.add("⚠️• Объём должен быть больше нуля")
        }

        // Проверка крепости
        val strength = strengthStr.toDoubleOrNull()
        if (strengthStr.isEmpty()) {
            errors.add("⚠️• Крепость спирта не указана")
        } else if (strength == null) {
            errors.add("⚠️• Некорректная крепость")
        } else if (strength !in 0.0..100.0) {
            errors.add("⚠️• Крепость: от 0 до 100%")
        }

        // Проверка итоговой крепости
        if (itogStrengthStr.isEmpty()) {
            hasItogWarning = true
            itogStrength = 40.0 // значение по умолчанию
        } else {
            val input = itogStrengthStr.toDoubleOrNull()
            when {
                input == null -> {
                    errors.add("⚠️• Некорректная крепость на выходе")
                }
                input !in 0.1..100.0 -> {
                    errors.add("⚠️• Крепость на выходе: от 0.1 до 100%")
                }
                else -> {
                    itogStrength = input
                }
            }
        }

        // Проверка температуры
        val tempInput = tempStr.toDoubleOrNull()
        if (tempStr.isEmpty()) {
            hasTempWarning = true
            temp = 20.0 // значение по умолчанию
        } else if (tempInput == null) {
            errors.add("⚠️• Некорректная температура")
        } else if (tempInput !in -10.0..100.0) {
            errors.add("⚠️• Температура: от -10 до 100°C")
        } else {
            temp = tempInput
        }

        // Проверка процента голов
        if (headsPercentStr.isEmpty()) {
            hasHeadsEmpty = true
            headsPercent = 0.0 // значение по умолчанию
        } else {
            val input = headsPercentStr.toDoubleOrNull()
            when {
                input == null -> {
                    errors.add("⚠️• Некорректный процент голов")
                }
                input < 0 -> {
                    errors.add("⚠️• Процент голов не может быть отрицательным")
                }
                else -> {
                    headsPercent = input
                }
            }
        }

        // Проверка процента хвостов
        if (tailsPercentStr.isEmpty()) {
            hasTailsEmpty = true
            tailsPercent = 0.0 // значение по умолчанию
        } else {
            val input = tailsPercentStr.toDoubleOrNull()
            when {
                input == null -> {
                    errors.add("⚠️• Некорректный процент хвостов")
                }
                input < 0 -> {
                    errors.add("⚠️• Процент хвостов не может быть отрицательным")
                }
                else -> {
                    tailsPercent = input
                }
            }
        }

        // Проверка на сумму голов и хвостов
        if (headsPercent > 0 || tailsPercent > 0) {
            if (headsPercent + tailsPercent >= 100) {
                errors.add("⚠️• Сумма голов и хвостов не может быть ≥ 100%")
            }
        }

        // Добавляем предупреждения как сообщения в общий поток ошибок
        if (hasTempWarning) {
            errors.add("⚠️• ❗ Не указана темп. → принято 20°C")
        }
        if (hasItogWarning) {
            errors.add("⚠️• ❗ Не указана крепость → принято 40% об.")
        }
        if (hasHeadsEmpty) {
            errors.add("⚠️• ❗ Не указаны головы → принято 0%")
        }
        if (hasTailsEmpty) {
            errors.add("⚠️• ❗ Не указаны хвосты → принято 0%")
        }

        // Если есть ошибки — выводим все
        if (errors.isNotEmpty()) {
            val errorText = SpannableStringBuilder(errors.joinToString("\n"))
            errorText.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.holo_orange_dark)),
                0,
                errorText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            resultText.text = errorText
            return
        }

        // Коррекция крепости по температуре
        val correctedStrength = strength!! + (20 - temp) * 0.04

        if (correctedStrength <= 0 || correctedStrength > 100) {
            resultText.text = "⚠️• Ошибка коррекции: некорректная крепость"
            return
        }

        // Расчёт
        val absoluteAlcohol = volume!! * correctedStrength / 100.0
        val headsVolume = absoluteAlcohol * headsPercent / 100.0
        val tailsVolume = absoluteAlcohol * tailsPercent / 100.0
        val bodyVolume = volume - headsVolume - tailsVolume
        val bodyStrength = correctedStrength
        val itogVolume = (bodyVolume * bodyStrength) / itogStrength

        // Формируем результат
        val correctedLine = "Корр. крепость: ${"%.1f".format(correctedStrength)} %об."
        val absoluteLine = "Абс. спирт: ${"%.2f".format(absoluteAlcohol)} л"
        val headsLine = "Головы: ${"%.2f".format(headsVolume)} л"
        val tailsLine = "Хвосты: ${"%.2f".format(tailsVolume)} л"
        val itogLine = "Продукт $itogStrength%об: ${"%.2f".format(itogVolume)} л"

        val result = "$correctedLine\n$absoluteLine\n$headsLine\n$tailsLine\n$itogLine"
        resultText.text = result
    }

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