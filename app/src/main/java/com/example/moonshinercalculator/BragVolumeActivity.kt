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

class BragVolumeActivity : AppCompatActivity() {

    private lateinit var sugarAmountEdit: EditText
    private lateinit var volumeEdit: EditText
    private lateinit var resultText: TextView
    private lateinit var sugarTypeSpinner: Spinner
    private lateinit var volumeTypeSpinner: Spinner
    private var coefficient = 1.0

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_brag_volume)
        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("BragaVolumePrefs", MODE_PRIVATE)
        // Инициализация полей
        sugarAmountEdit = findViewById(R.id.sugarAmountEdit)
        volumeEdit = findViewById(R.id.volumeEdit)
        resultText = findViewById(R.id.resultText)
        sugarTypeSpinner = findViewById(R.id.sugarTypeSpinner)
        volumeTypeSpinner = findViewById(R.id.volumeTypeSpinner)
        // Кнопка "Назад"
        val homeButton = findViewById<ImageView>(R.id.homeButton)
        homeButton.setOnClickListener {
            finish()
        }
        // Включаем поддержку цветного текста
        resultText.movementMethod = LinkMovementMethod.getInstance()
        // Настройка Spinner (сахар)
        val sugarTypes = arrayOf("1 - Сахар", "2 - Декстроза")
        val sugarAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sugarTypes)
        sugarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sugarTypeSpinner.adapter = sugarAdapter
        // Настройка Spinner (тип объёма)
        val volumeTypes = arrayOf("Объём браги", "Объём тары")
        val volumeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, volumeTypes)
        volumeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        volumeTypeSpinner.adapter = volumeAdapter
        // Восстановление состояния
        restoreState()
        // Обновление коэффициента и расчёта при выборе типа сахара
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
        // Переключение логики при выборе типа объёма
        volumeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                calculate()
                saveState()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                calculate()
                saveState()
            }
        }
        // TextWatcher для автоматического пересчёта
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculate()
                saveState()
            }
        }
        sugarAmountEdit.addTextChangedListener(textWatcher)
        volumeEdit.addTextChangedListener(textWatcher)

        calculate() // Первичный расчёт
    }
    private fun calculate() {
        val sugarStr = sugarAmountEdit.text.toString().trim()
        val volumeStr = volumeEdit.text.toString().trim()

        val errors = mutableListOf<String>()
        // Проверка пустых полей
        if (sugarStr.isEmpty()) {
            errors.add("⚠️• Количество сахара не указано")
        }
        if (volumeStr.isEmpty()) {
            errors.add("⚠️• Объём не указан")
        }
        // Проверка валидности чисел
        val sugarKg = sugarStr.toDoubleOrNull()
        val targetVolume = volumeStr.toDoubleOrNull()

        if (sugarStr.isNotEmpty() && sugarKg == null) {
            errors.add("⚠️• Некорректное значение сахара")
        }
        if (volumeStr.isNotEmpty() && targetVolume == null) {
            errors.add("⚠️• Некорректное значение объёма")
        }
        // Проверка положительных значений
        if (sugarKg != null && sugarKg <= 0) {
            errors.add("⚠️• Количество сахара должно быть больше нуля")
        }
        if (targetVolume != null && targetVolume <= 0) {
            errors.add("⚠️• Объём должен быть больше нуля")
        }
        // Если есть ошибки — выводим все
        if (errors.isNotEmpty()) {
            resultText.text = errors.joinToString("\n")
            return
        }
        // Все данные корректны — продолжаем расчёт
        val bragaVolume = if (volumeTypeSpinner.selectedItemPosition == 0) {
            targetVolume!!
        } else {
            targetVolume!! * 0.8
        }

        val waterVolume = bragaVolume - (sugarKg!! * 0.629)
        if (waterVolume <= 0) {
            resultText.text = "⚠️• Ошибка: недостаточный объём браги для указанного сахара"
            return
        }
        // Гидромодуль = вода / сахар
        val giromodule = waterVolume / sugarKg

        val fermentationTankVolume = bragaVolume * 1.2 // +20%
        val extraktivity = 259 - 259000 / (sugarKg * 384 / bragaVolume + 1000)
        val alkogolVolume = sugarKg * 58.8 * coefficient / bragaVolume
        val distillateOutput = sugarKg * (55.88 / 40) * coefficient
        // Определяем сообщение и цвет
        val message: String
        val messageColor: Int
        val iconRes = android.R.drawable.ic_dialog_alert

        when {
            alkogolVolume > 24 -> {
                message = "  Превышена максимальная крепость браги"
                messageColor = Color.RED
            }
            alkogolVolume > 15 -> {
                message = "  Превышена оптимальная крепость браги"
                messageColor = Color.rgb(255, 165, 0)
            }
            else -> {
                message = "  Крепость браги оптимальная"
                messageColor = Color.GREEN
            }
        }
        // Формируем текст результата
        val baseText = """
            Сахар ${"%.1f".format(sugarKg)} кг. Вода ${"%.1f".format(waterVolume)} л.
            Объём браги: ${"%.1f".format(bragaVolume)} л.
            Ёмкость для брожения: ${"%.1f".format(fermentationTankVolume)} л.
            Гидромодуль: ${"%.1f".format(giromodule)}:1.
            Экстрактивность: ${"%.1f".format(extraktivity)} Brix.
            Крепость браги: ${"%.1f".format(alkogolVolume)} %об.
            Выход дистиллята 40%об: ${"%.1f".format(distillateOutput)} л.
        """.trimIndent()

        val fullText = "$baseText\n$message"
        val resultWithMessage = SpannableStringBuilder(fullText)
        // Цвет сообщения
        val messageStart = baseText.length + 1
        val messageEnd = fullText.length
        resultWithMessage.setSpan(
            ForegroundColorSpan(messageColor),
            messageStart,
            messageEnd,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // 🔥 Устанавливаем размер шрифта
        // 36 — это размер в пикселях. Можно использовать RelativeSizeSpan для масштаба
        resultWithMessage.setSpan(
            AbsoluteSizeSpan(28, true), // true = масштабировать относительно текущего размера шрифта
            messageStart,
            messageEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Иконка
        val drawable = ContextCompat.getDrawable(this, iconRes)
        if (drawable != null) {
            drawable.mutate().setTint(messageColor)
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
            resultWithMessage.setSpan(
                imageSpan,
                messageStart,
                messageStart + 1,
                android.text.Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            )
        }
        // Подсветка строки "Крепость браги" при >15%
        if (alkogolVolume > 15) {
            val alcoholLineStart = baseText.indexOf("Крепость браги")
            if (alcoholLineStart >= 0) {
                val alcoholLineEnd = baseText.indexOf('\n', alcoholLineStart).let { if (it == -1) baseText.length else it }
                resultWithMessage.setSpan(
                    ForegroundColorSpan(Color.RED),
                    alcoholLineStart,
                    alcoholLineEnd,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        // Устанавливаем результат
        resultText.text = resultWithMessage
    }
    private fun saveState() {
        sharedPreferences.edit {
            putString("sugar_amount", sugarAmountEdit.text.toString())
            putString("volume", volumeEdit.text.toString())
            putInt("sugar_type_position", sugarTypeSpinner.selectedItemPosition)
            putInt("volume_type_position", volumeTypeSpinner.selectedItemPosition)
        }
    }
    private fun restoreState() {
        sugarAmountEdit.setText(sharedPreferences.getString("sugar_amount", ""))
        volumeEdit.setText(sharedPreferences.getString("volume", ""))
        val sugarPos = sharedPreferences.getInt("sugar_type_position", 0)
        val volumePos = sharedPreferences.getInt("volume_type_position", 0)
        if (sugarPos < sugarTypeSpinner.adapter.count) {
            sugarTypeSpinner.setSelection(sugarPos)
        }
        if (volumePos < volumeTypeSpinner.adapter.count) {
            volumeTypeSpinner.setSelection(volumePos)
        }
    }
    override fun onPause() {
        super.onPause()
        saveState()
    }
}