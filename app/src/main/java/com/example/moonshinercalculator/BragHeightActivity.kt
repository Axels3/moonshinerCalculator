package com.example.moonshinercalculator

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class BragHeightActivity : AppCompatActivity() {

    private lateinit var sugarAmountEdit: EditText
    private lateinit var waterVolumeEdit: EditText
    private lateinit var resultText: TextView
    private lateinit var sugarTypeSpinner: Spinner
    private var coefficient = 1.0

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_brag_height)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("BragHeightPrefs", MODE_PRIVATE)

        // Инициализация полей
        sugarAmountEdit = findViewById(R.id.sugarAmountEdit)
        waterVolumeEdit = findViewById(R.id.waterVolumeEdit)
        resultText = findViewById(R.id.resultText)
        sugarTypeSpinner = findViewById(R.id.sugarTypeSpinner)

        // Кнопка "Назад"
        val homeButton = findViewById<ImageView>(R.id.homeButton)
        homeButton.setOnClickListener {
            finish()
        }

        // Включаем поддержку цветного текста
        resultText.movementMethod = LinkMovementMethod.getInstance()

        // Настройка Spinner
        val sugarTypes = arrayOf("1 - Сахар", "2 - Декстроза")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sugarTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sugarTypeSpinner.adapter = adapter

        // Восстановление состояния
        restoreState()

        sugarTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                coefficient = if (position == 0) 1.0 else 0.875
                calculate()
                saveState()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                coefficient = 1.0
                calculate()
                saveState()
            }
        }

        // TextWatcher для автоматического пересчёта и сохранения
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculate()
                saveState()
            }
        }
        sugarAmountEdit.addTextChangedListener(textWatcher)
        waterVolumeEdit.addTextChangedListener(textWatcher)

        calculate() // Первичный расчёт
    }

    private fun calculate() {
    val sugarStr = sugarAmountEdit.text.toString().trim()
    val waterStr = waterVolumeEdit.text.toString().trim()

    val errors = mutableListOf<String>()

    // Проверка на пустые поля
    if (sugarStr.isEmpty()) {
        errors.add("⚠️• Количество сахара не указано")
    }
    if (waterStr.isEmpty()) {
        errors.add("⚠️• Объём воды не указан")
    }

    // Проверка на валидность чисел
    val sugarKg = sugarStr.toDoubleOrNull()
    val waterVolume = waterStr.toDoubleOrNull()

    if (sugarStr.isNotEmpty() && sugarKg == null) {
        errors.add("⚠️• Некорректное значение сахара")
    }
    if (waterStr.isNotEmpty() && waterVolume == null) {
        errors.add("⚠️• Некорректное значение объёма воды")
    }

    // Проверка на положительные значения
    if (sugarKg != null && sugarKg <= 0) {
        errors.add("⚠️• Количество сахара должно быть больше нуля")
    }
    if (waterVolume != null && waterVolume <= 0) {
        errors.add("⚠️• Объём воды должен быть больше нуля")
    }

    // Если есть ошибки — выводим их и выходим
    if (errors.isNotEmpty()) {
        resultText.text = errors.joinToString("\n")
        return
    }

    // Все данные корректны — продолжаем расчёт
    val bragaVolume = sugarKg!! * 0.629 + waterVolume!!
    val fermentationTankVolume = bragaVolume * 1.2 // +20%
    val giromodule = waterVolume / sugarKg
    val extraktivity = 259 - 259000 / (sugarKg * 384 / bragaVolume + 1000)
    val alkogolVolume = sugarKg * 58.8 * coefficient / bragaVolume
    val distillateOutput = sugarKg * (55.88 / 40) * coefficient

    val baseText = """
        Сахар: ${"%.2f".format(sugarKg)} кг
        Вода: ${"%.2f".format(waterVolume)} л
        Объём браги: ${"%.2f".format(bragaVolume)} л
        Ёмкость для брожения: ${"%.2f".format(fermentationTankVolume)} л
        Гидромодуль: ${"%.2f".format(giromodule)}:1
        Экстрактивность: ${"%.2f".format(extraktivity)} Brix
        Выход дистиллята 40%об: ${"%.2f".format(distillateOutput)} л
        Крепость браги: ${"%.2f".format(alkogolVolume)} %об.
    """.trimIndent()

    // Сообщение о крепости
    val message: String
    val messageColor: Int
    val iconRes = android.R.drawable.ic_dialog_info

    when {
        alkogolVolume > 24 -> {
            message = "Превышена максимальная крепость браги"
            messageColor = Color.RED
        }
        alkogolVolume > 15 -> {
            message = "Превышена оптимальная крепость браги"
            messageColor = Color.rgb(255, 100, 0)
        }
        else -> {
            message = "Крепость браги оптимальная"
            messageColor = Color.GREEN
        }
    }

    val fullText = "$baseText\n$message"
    val resultWithMessage = SpannableStringBuilder(fullText)

    // Цвет сообщения
    val messageStart = baseText.length + 1
    val messageEnd = fullText.length
    resultWithMessage.setSpan(
        ForegroundColorSpan(messageColor),
        messageStart,
        messageEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
        // 🔥 Устанавливаем размер шрифта: 18sp (примерно 36px)
        // 36 — это размер в пикселях. Можно использовать RelativeSizeSpan для масштаба

        resultWithMessage.setSpan(
            AbsoluteSizeSpan(28, true), // true = масштабировать относительно текущего размера шрифта
            messageStart,
            messageEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
)



    // Иконка
    resultWithMessage.insert(messageStart, " ")
    val drawable = ContextCompat.getDrawable(this, iconRes)
    if (drawable != null) {
        drawable.mutate().setTint(messageColor)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
        resultWithMessage.setSpan(
            imageSpan,
            messageStart,
            messageStart + 1,
            Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )
    }

    // Подсветка строки "Крепость браги", если > 15%
    if (alkogolVolume > 15) {
        val alcoholLineStart = baseText.indexOf("Крепость браги")
        if (alcoholLineStart >= 0) {
            val alcoholLineEnd = baseText.indexOf('\n', alcoholLineStart).let { if (it == -1) baseText.length else it }
            resultWithMessage.setSpan(
                ForegroundColorSpan(Color.RED),
                alcoholLineStart,
                alcoholLineEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    resultText.text = resultWithMessage
    resultText.movementMethod = LinkMovementMethod.getInstance()
}

    /*private fun calculate() {
        val sugarStr = sugarAmountEdit.text.toString().trim()
        val waterStr = waterVolumeEdit.text.toString().trim()

        if (sugarStr.isEmpty() || waterStr.isEmpty()) {
            resultText.text = "Введите данные для расчёта"
            return
        }

        val sugarKg = sugarStr.toDoubleOrNull()
        val waterVolume = waterStr.toDoubleOrNull()

        if (sugarKg == null || waterVolume == null) {
            resultText.text = "Некорректный ввод"
            return
        }

        if (sugarKg <= 0) {
            resultText.text = "Количество сахара должно быть больше нуля"
            return
        }

        if (waterVolume <= 0) {
            resultText.text = "Объём воды должен быть больше нуля"
            return
        }

        val bragaVolume = sugarKg * 0.629 + waterVolume
        val fermentationTankVolume = bragaVolume * 1.2 // +20%
        val giromodule = waterVolume / sugarKg
        val extraktivity = 259 - 259000 / (sugarKg * 384 / bragaVolume + 1000)
        val alkogolVolume = sugarKg * 58.8 * coefficient / bragaVolume
        val distillateOutput = sugarKg * (55.88 / 40) * coefficient

        // Формируем строки с подстановкой значений
        val inputLine = "Сахар: $sugarKg кг, Вода: $waterVolume л"
        val volumeLine = "Объём браги: %.2f л".format(bragaVolume)
        val tankLine = "Ёмкость для брожения: %.2f л".format(fermentationTankVolume)
        val moduleLine = "Гидромодуль: %.2f:1".format(giromodule)
        val extraktivityLine = "Экстрактивность: %.2f %%".format(extraktivity)
        val alcoholLine = "Крепость браги: %.2f %%".format(alkogolVolume)
        val outputLine = "Выход дистиллята: %.2f л".format(distillateOutput)

        val baseText = """
            $inputLine
            $volumeLine
            $tankLine
            $moduleLine
            $extraktivityLine
            $alcoholLine
            $outputLine
        """.trimIndent()

// Определяем сообщение, цвет и системную иконку
        val message: String
        val messageColor: Int
        val systemIcon: Int

        when {
            alkogolVolume > 24 -> {
                message = "Превышена максимальная крепость браги"
                messageColor = Color.RED
                systemIcon = android.R.drawable.ic_dialog_alert
            }
            alkogolVolume > 15 -> {
                message = "Превышена оптимальная крепость браги"
                messageColor = Color.rgb(255, 165, 0)
                systemIcon = android.R.drawable.ic_dialog_alert
            }
            else -> {
                message = "Крепость браги оптимальная"
                messageColor = Color.GREEN
                systemIcon = android.R.drawable.ic_dialog_alert
            }
        }

        val fullText = "$baseText\n$message"
        val resultWithMessage = SpannableStringBuilder(fullText)

// Цвет текста
        val messageStart = baseText.length + 1
        val messageEnd = fullText.length
        resultWithMessage.setSpan(
            ForegroundColorSpan(messageColor),
            messageStart,
            messageEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// Вставляем иконку
        resultWithMessage.insert(messageStart, " ")
        val drawable = ContextCompat.getDrawable(this, systemIcon)
        if (drawable != null) {
            drawable.setTint(messageColor) // ← Основное исправление
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
            resultWithMessage.setSpan(
                imageSpan,
                messageStart,
                messageStart + 1,
                Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }

        // Подсветка строки "Крепость браги" — только если > 15%
        if (alkogolVolume > 15) {
            val alcoholLineStart = baseText.indexOf("Крепость браги")
            if (alcoholLineStart >= 0) {
                val alcoholLineEnd = baseText.indexOf('\n', alcoholLineStart).let { if (it == -1) baseText.length else it }
                resultWithMessage.setSpan(
                    ForegroundColorSpan(Color.RED),
                    alcoholLineStart,
                    alcoholLineEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        resultText.text = resultWithMessage
    }*/

    private fun saveState() {
        sharedPreferences.edit {
            putString("sugar_amount", sugarAmountEdit.text.toString())
            putString("water_volume", waterVolumeEdit.text.toString())
            putInt("sugar_type_position", sugarTypeSpinner.selectedItemPosition)
        }
    }

    private fun restoreState() {
        sugarAmountEdit.setText(sharedPreferences.getString("sugar_amount", ""))
        waterVolumeEdit.setText(sharedPreferences.getString("water_volume", ""))
        val position = sharedPreferences.getInt("sugar_type_position", 0)
        if (position < sugarTypeSpinner.adapter.count) {
            sugarTypeSpinner.setSelection(position)
        }
    }

    override fun onPause() {
        super.onPause()
        saveState()
    }
}