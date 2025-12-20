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

class KokteilActivity : AppCompatActivity() {

    private lateinit var alcohol1Edit: EditText
    private lateinit var volume1Edit: EditText
    private lateinit var alcohol2Edit: EditText
    private lateinit var volume2Edit: EditText
    private lateinit var alcohol3Edit: EditText
    private lateinit var volume3Edit: EditText
    private lateinit var alcohol4Edit: EditText
    private lateinit var volume4Edit: EditText
    private lateinit var sugarWeightEdit: EditText
    private lateinit var resultText: TextView

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_kokteil)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("CoktailPrefs", MODE_PRIVATE)

        // Инициализация полей
        alcohol1Edit = findViewById(R.id.alcohol1Edit)
        volume1Edit = findViewById(R.id.volume1Edit)
        alcohol2Edit = findViewById(R.id.alcohol2Edit)
        volume2Edit = findViewById(R.id.volume2Edit)
        alcohol3Edit = findViewById(R.id.alcohol3Edit)
        volume3Edit = findViewById(R.id.volume3Edit)
        alcohol4Edit = findViewById(R.id.alcohol4Edit)
        volume4Edit = findViewById(R.id.volume4Edit)
        sugarWeightEdit = findViewById(R.id.sugarWeightEdit)
        resultText = findViewById(R.id.resultText)

        // Восстановление состояния
        restoreState()

        // Кнопка "Назад"
        val homeButton = findViewById<ImageView>(R.id.homeButton)
        homeButton.setOnClickListener {
            finish()
        }

        // TextWatcher для пересчёта при вводе
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateStrength()
                saveState()
            }
        }

        // Привязка TextWatcher ко всем полям
        alcohol1Edit.addTextChangedListener(textWatcher)
        volume1Edit.addTextChangedListener(textWatcher)
        alcohol2Edit.addTextChangedListener(textWatcher)
        volume2Edit.addTextChangedListener(textWatcher)
        alcohol3Edit.addTextChangedListener(textWatcher)
        volume3Edit.addTextChangedListener(textWatcher)
        alcohol4Edit.addTextChangedListener(textWatcher)
        volume4Edit.addTextChangedListener(textWatcher)
        sugarWeightEdit.addTextChangedListener(textWatcher)

        calculateStrength()
    }

    @SuppressLint("DefaultLocale")
    private fun calculateStrength() {
        val components = listOf(
            getComponent(alcohol1Edit, volume1Edit),
            getComponent(alcohol2Edit, volume2Edit),
            getComponent(alcohol3Edit, volume3Edit),
            getComponent(alcohol4Edit, volume4Edit)
        ).filter { it.volume > 0 }

        val sugarGrams = sugarWeightEdit.text.toString().trim().toDoubleOrNull() ?: 0.0

        if (components.isEmpty() && sugarGrams <= 0) {
            resultText.text = "⚠️• Введите компоненты"
            return
        }

        // Общий объём чистого спирта (в мл)
        val totalAlcohol = components.sumOf { it.alcoholVolume }

        // Общий объём жидкости (в мл)
        val totalVolume = components.sumOf { it.volume }

        // Вклад сахара: эквивалентный объём, занимаемый сахаром (коэффициент 0.629 мл/г)
        val sugarVolumeEquivalent = sugarGrams * 0.629

        // Полный объём смеси (жидкость + объём, занимаемый сахаром)
        val totalEffectiveVolume = totalVolume + sugarVolumeEquivalent



        // Крепость коктейля (в %)
        val strength = (totalAlcohol / totalEffectiveVolume) * 100

        resultText.text = String.format("Крепость коктейля - %.1f %%", strength)
    }

    private fun getComponent(alcoholEdit: EditText, volumeEdit: EditText): Component {
        val strengthStr = alcoholEdit.text.toString().trim()
        val volumeStr = volumeEdit.text.toString().trim()

        val strength = strengthStr.toDoubleOrNull() ?: 0.0
        val volume = volumeStr.toDoubleOrNull() ?: 0.0

        if (volume <= 0 || strength < 0 || strength > 100) return Component(0.0, 0.0)

        val alcoholVolume = volume * (strength / 100)
        return Component(alcoholVolume, volume)
    }

    data class Component(val alcoholVolume: Double, val volume: Double)

    private fun saveState() {
        sharedPreferences.edit {
            putString("alcohol1", alcohol1Edit.text.toString())
            putString("volume1", volume1Edit.text.toString())
            putString("alcohol2", alcohol2Edit.text.toString())
            putString("volume2", volume2Edit.text.toString())
            putString("alcohol3", alcohol3Edit.text.toString())
            putString("volume3", volume3Edit.text.toString())
            putString("alcohol4", alcohol4Edit.text.toString())
            putString("volume4", volume4Edit.text.toString())
            putString("sugar_weight", sugarWeightEdit.text.toString())
        }
    }

    private fun restoreState() {
        alcohol1Edit.setText(sharedPreferences.getString("alcohol1", ""))
        volume1Edit.setText(sharedPreferences.getString("volume1", ""))
        alcohol2Edit.setText(sharedPreferences.getString("alcohol2", ""))
        volume2Edit.setText(sharedPreferences.getString("volume2", ""))
        alcohol3Edit.setText(sharedPreferences.getString("alcohol3", ""))
        volume3Edit.setText(sharedPreferences.getString("volume3", ""))
        alcohol4Edit.setText(sharedPreferences.getString("alcohol4", ""))
        volume4Edit.setText(sharedPreferences.getString("volume4", ""))
        sugarWeightEdit.setText(sharedPreferences.getString("sugar_weight", ""))
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }
}