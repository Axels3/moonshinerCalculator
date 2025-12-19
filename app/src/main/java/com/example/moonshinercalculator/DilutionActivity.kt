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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.moonshinercalculators.utils.GostTable

class DilutionActivity : AppCompatActivity() {

    private lateinit var volumeValueLabel: TextView
    private lateinit var finalVolumeEdit: EditText
    private lateinit var finalStrengthTextEdit: EditText
    private lateinit var strengthTextEdit: EditText
    private lateinit var tempTextEdit: EditText
    private lateinit var resultTextView: TextView
    private lateinit var homeButton: ImageView

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dilution)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("DilutionPrefs", MODE_PRIVATE)

        // Привязка элементов
        volumeValueLabel = findViewById(R.id.dilutionTextView)
        finalVolumeEdit = findViewById(R.id.finalVolumeEdit)
        finalStrengthTextEdit = findViewById(R.id.finalStrengthTextEdit)
        strengthTextEdit = findViewById(R.id.strengthTextEdit)
        tempTextEdit = findViewById(R.id.TempTextEdit)
        resultTextView = findViewById(R.id.resultTextView)
        homeButton = findViewById(R.id.homeButton)

        // Устанавливаем статичный режим
        volumeValueLabel.text = "Режим: Начальный объём"

        // Общий TextWatcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputs()
                saveInputValues()
            }
        }

        finalVolumeEdit.addTextChangedListener(textWatcher)
        finalStrengthTextEdit.addTextChangedListener(textWatcher)
        strengthTextEdit.addTextChangedListener(textWatcher)
        tempTextEdit.addTextChangedListener(textWatcher)

        // Восстановление значений
        restoreSavedValues()
        validateInputs()

        // Кнопка "Назад"
        homeButton.setOnClickListener { finish() }

        // Обработка отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onPause() {
        super.onPause()
        saveInputValues()
    }

    @SuppressLint("UseKtx")
    private fun saveInputValues() {
        with(sharedPreferences.edit()) {
            putString("strength", strengthTextEdit.text.toString())
            putString("temperature", tempTextEdit.text.toString())
            putString("target_strength", finalStrengthTextEdit.text.toString())
            putString("final_volume", finalVolumeEdit.text.toString())
            apply() // Асинхронное сохранение
        }
    }

    private fun restoreSavedValues() {
        // Явно читаем значения
        val strength = sharedPreferences.getString("strength", "")
        val temp = sharedPreferences.getString("temperature", "")
        val target = sharedPreferences.getString("target_strength", "")
        val volume = sharedPreferences.getString("final_volume", "")

        // Устанавливаем в поля
        strengthTextEdit.setText(strength)
        tempTextEdit.setText(temp)
        finalStrengthTextEdit.setText(target)
        finalVolumeEdit.setText(volume)
    }

    private fun validateInputs() {
        val errors = mutableListOf<String>()

        fun addErrorIf(condition: Boolean, message: String) {
            if (condition) errors.add("⚠️ $message")
        }

        val strength = strengthTextEdit.text.toString().toDoubleOrNull()
        val targetStrength = finalStrengthTextEdit.text.toString().toDoubleOrNull()
        val temp = tempTextEdit.text.toString().toDoubleOrNull()
        val volume = finalVolumeEdit.text.toString().toDoubleOrNull()

        addErrorIf(strength == null, "Введите начальную крепость")
        addErrorIf(strength != null && (strength < 0 || strength > 100), "Начальная крепость: 0–100%")
        addErrorIf(targetStrength == null, "Введите целевую крепость")
        addErrorIf(targetStrength != null && (targetStrength < 0 || targetStrength > 100), "Целевая крепость: 0–100%")
        addErrorIf(targetStrength != null && strength != null && targetStrength >= strength, "Целевая крепость < начальной")
        addErrorIf(temp == null, "Введите температуру")
        addErrorIf(temp != null && (temp < 0 || temp > 76), "Температура: 0–76°C")
        addErrorIf(volume == null, "Введите объём")
        addErrorIf(volume != null && volume <= 0, "Объём должен быть > 0")

        if (errors.isNotEmpty()) {
            resultTextView.text = errors.joinToString("\n")
            return
        }

        calculateWithInitialVolume()
    }

    private fun density(x: Double): Double = findValue(x) { it.first }

    private fun findValue(x: Double, selector: (Pair<Double, Double>) -> Double): Double {
        val clampedX = x.coerceIn(0.0, 100.0)
        val keys = GostTable.data.keys.sorted()

        GostTable.data[clampedX]?.let { return selector(it) }

        val lower = keys.lastOrNull { it <= clampedX } ?: keys.first()
        val upper = keys.firstOrNull { it >= clampedX } ?: keys.last()

        if (lower == upper) {
            return selector(GostTable.data[lower]!!)
        }

        val (y1, y2) = GostTable.data[lower]!! to GostTable.data[upper]!!
        val x1 = lower
        val x2 = upper
        val r = (clampedX - x1) / (x2 - x1)
        return selector(y1) + r * (selector(y2) - selector(y1))
    }

    private fun calculateWithInitialVolume() {
        val volume = finalVolumeEdit.text.toString().toDouble()
        val strength = strengthTextEdit.text.toString().toDouble()
        val temperature = tempTextEdit.text.toString().toDouble()
        val targetStrength = finalStrengthTextEdit.text.toString().toDouble()

        val correctedStrength = strength + (20 - temperature) * 0.3
        if (correctedStrength <= 0) {
            resultTextView.text = "Ошибка: скорректированная крепость ≤ 0"
            return
        }
        if (targetStrength >= correctedStrength) {
            resultTextView.text = "Ошибка: целевая крепость должна быть < начальной"
            return
        }

        val s = correctedStrength
        val t = targetStrength
        val v0 = volume * 1000
        val ps = density(s)
        val pt = density(t)
        val mw = (s / t) * v0 - v0
        val m0 = v0 * ps
        val m1 = m0 + mw
        val v1 = m1 / pt

        resultTextView.text = buildString {
            append("Скорректированная крепость: ${"%.1f".format(s)} %,об\n")
            append("Чтобы получить продукт крепостью: ${"%.1f".format(t)} %,об\n")
            append("Требуется спирта крепостью ${"%.1f".format(s)} %: ${"%.0f".format(v0)} мл весом: ${"%.0f".format(m0)} гр.\n")
            append("Требуется воды: ${"%.0f".format(mw)} мл\n")
            append("Итоговый объём: ${"%.0f".format(v1)} мл")
        }
    }
}