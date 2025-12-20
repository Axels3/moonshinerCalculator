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

class RefractometrCorrectActivity : AppCompatActivity() {

    private lateinit var initialBrixEdit: EditText
    private lateinit var finalBrixEdit: EditText
    private lateinit var resultText: TextView
    private lateinit var textView12: TextView  // Для отображения нового расчёта

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_refractometr_correct)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("RefractometerPrefs", MODE_PRIVATE)

        // Привязка UI-элементов
        initialBrixEdit = findViewById(R.id.initialBrixEdit)
        finalBrixEdit = findViewById(R.id.finalBrixEdit)
        resultText = findViewById(R.id.resultText)
        textView12 = findViewById(R.id.textView12)  // Привязка нового TextView

        // Отключаем прокрутку текста
        resultText.isVerticalScrollBarEnabled = false

        // Восстановление предыдущих значений
        restoreState()

        // Кнопка "Назад"
        findViewById<ImageView>(R.id.homeButton).setOnClickListener { finish() }

        // TextWatcher для отслеживания ввода и расчёта
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateAndDisplay()
                calculateAndDisplayByOg()  // Добавлен вызов новой функции
                saveState()
            }
        }

        initialBrixEdit.addTextChangedListener(textWatcher)
        finalBrixEdit.addTextChangedListener(textWatcher)

        // Первичный расчёт при старте
        calculateAndDisplay()
        calculateAndDisplayByOg()
    }

    // === Модель результата расчёта ===
    data class BeerCalculationResult(
        val ogSg: Double,
        val fgSg: Double,
        val fgBrixCorrected: Double,
        val abw: Double,
        val abv: Double,
        val ogBrix: Double  // Добавлено: начальные Brix
    )

    // === Проверка ввода ===
    @SuppressLint("SetTextI18n")
    private fun validateInput(ogBrix: Double, fgBrix: Double): Boolean {
        if (ogBrix < 0 || ogBrix > 75 || ogBrix.isNaN()) {
            resultText.text = "Начальные показания Brix должны быть числом от 0 до 75."
            return false
        }
        if (fgBrix < 0 || fgBrix > 75 || fgBrix.isNaN()) {
            resultText.text = "Конечные показания Brix должны быть числом от 0 до 75."
            return false
        }
        return true
    }

    // === Основной расчёт ===
    private fun calculateABV(ogBrix: Double, fgBrix: Double): BeerCalculationResult? {
        if (!validateInput(ogBrix, fgBrix)) return null

        val ogSg = (ogBrix / (258.6 - ((ogBrix / 258.2) * 227.1))) + 1
        val fgSg = 1 + 0.006276 * fgBrix - 0.002349 * ogBrix
        val fgBrixCorrected = -616.868 + 1111.14 * fgSg - 630.272 * fgSg * fgSg + 135.997 * fgSg * fgSg * fgSg
        val abw = 0.67062 * ogBrix - 0.66091 * fgBrix
        val abv = (fgSg * abw) / 0.791

        return BeerCalculationResult(ogSg, fgSg, fgBrixCorrected, abw, abv, ogBrix)
    }

    // === Вызов расчёта и вывод результата в виде таблицы ===
    private fun calculateAndDisplay() {
        val initialStr = initialBrixEdit.text.toString().trim()
        val finalStr = finalBrixEdit.text.toString().trim()

        if (initialStr.isEmpty() || finalStr.isEmpty()) {
            resultText.text = "⚠️• Ожидание ввода данных..."
            return
        }

        val ogBrix = initialStr.toDoubleOrNull()
        val fgBrix = finalStr.toDoubleOrNull()

        if (ogBrix == null || fgBrix == null) {
            resultText.text = "⚠️• Некорректный ввод чисел"
            return
        }

        val result = calculateABV(ogBrix, fgBrix) ?: return

        resultText.text = buildString {
            appendLine("Скорректированные показания")
            appendLine("%-12s %-10s %-10s %-10s".format("Плотности", " ", "SG", "Brix"))
            appendLine("-".repeat(50))
            // Данные
            appendLine("%-12s %-8s %-10s %-10s".format(
                "Начальная:",
                "",
                "%.3f".format(result.ogSg),
                "%.1f".format(result.ogBrix)
            ))
            appendLine("%-12s %-10s %-10s %-10s".format(
                "Конечная:",
                "",
                "%.3f".format(result.fgSg),
                "%.1f".format(result.fgBrixCorrected)
            ))
            appendLine()
            appendLine("Алкоголь:")
            appendLine("ABV: ${"%.2f".format(result.abv)} %")
            appendLine()
            appendLine("")
        }.trimEnd()
    }

    // === Новая функция: расчёт ТОЛЬКО по НП (OG_brix) ===
    @SuppressLint("SetTextI18n")
    private fun calculateAndDisplayByOg() {
        val initialStr = initialBrixEdit.text.toString().trim()

        if (initialStr.isEmpty()) {
            textView12.text = "⚠️• Введите начальные Brix"
            return
        }

        val ogBrix = initialStr.toDoubleOrNull()

        if (ogBrix == null) {
            textView12.text = "⚠️• Некорректный ввод"
            return
        }

        if (ogBrix < 0 || ogBrix > 75) {
            textView12.text = "НП должен быть от 0 до 75"
            return
        }

        // Расчёт по новому алгоритму
        val fgBrix = (0.0000116016269889 + 0.002349 * ogBrix) / 0.006276
        val fgSg = 1 + 0.006276 * fgBrix - 0.002349 * ogBrix
        val abw = 0.67062 * ogBrix - 0.66091 * fgBrix
        val abv = (fgSg * abw) / 0.791

        // Вывод в textView12
        textView12.text = buildString {
            appendLine("Прогноз при полном сбраживании:")
            appendLine("показаний рефрактометра: ${"%.2f".format(fgBrix)} °Brix")
            appendLine("алкоголя: ${"%.2f".format(abv)} %")
        }.trimEnd()
    }

    // === Сохранение состояния ===
    private fun saveState() {
        sharedPreferences.edit {
            putString("initial_brix", initialBrixEdit.text.toString())
            putString("final_brix", finalBrixEdit.text.toString())
        }
    }

    // === Восстановление состояния ===
    private fun restoreState() {
        initialBrixEdit.setText(sharedPreferences.getString("initial_brix", ""))
        finalBrixEdit.setText(sharedPreferences.getString("final_brix", ""))
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }
}