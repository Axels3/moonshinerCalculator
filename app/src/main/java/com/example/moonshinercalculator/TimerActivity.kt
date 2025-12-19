package com.example.moonshinercalculator

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sin

class TimerActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var startStopButton: Button
    private lateinit var timeInput: EditText
    private lateinit var resetButton: ImageButton
    private lateinit var volumeInput: EditText
    private lateinit var speedResult: TextView
    private lateinit var homeButton3: ImageButton

    private var countDownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var targetSeconds = 0L

    private lateinit var soundPool: SoundPool
    private var shortSoundId: Int = 0
    private var longSoundId: Int = 0

    // Временные файлы для звуков
    private lateinit var shortToneFile: File
    private lateinit var longToneFile: File

    // Константы для звуков
    companion object {
        private const val TONE_FREQ_HZ = 800
        private const val SHORT_TONE_MS = 100
        private const val LONG_TONE_MS = 500
    }

    private var inputBuffer = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timer)

        // Создаем временные файлы для звуков
        try {
            shortToneFile = createTempSoundFile(createToneData(SHORT_TONE_MS))
            longToneFile = createTempSoundFile(createToneData(LONG_TONE_MS))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        initViews()
        initSound()
        setupViews()
        setupButtonClick()
        setupInputListeners()
        setupTimeInputWatcher()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        timerText = findViewById(R.id.timerText)
        startStopButton = findViewById(R.id.button)
        timeInput = findViewById(R.id.timeInput)
        resetButton = findViewById(R.id.resetButton)
        volumeInput = findViewById(R.id.volumeInput)
        speedResult = findViewById(R.id.speedResult)
        homeButton3 = findViewById(R.id.homeButton3) // Инициализация кнопки "на главную"
    }

    private fun initSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        // Добавим прослушку загрузки звуков
        shortSoundId = soundPool.load(shortToneFile.absolutePath, 1)
        longSoundId = soundPool.load(longToneFile.absolutePath, 1)

        // Проверка, что звуки загружены
        soundPool.setOnLoadCompleteListener { _, _, _ ->
            // Можно добавить лог, если нужно
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        timerText.visibility = View.GONE
        timeInput.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        volumeInput.visibility = View.VISIBLE
        speedResult.visibility = View.VISIBLE
        timeInput.setText("00:00")
        volumeInput.setText("0")
        calculateSpeed()

        // Установим цифровую клавиатуру
        timeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        volumeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun createToneData(durationMs: Int): ByteArray {
        val sampleRate = 8000
        val numSamples = ((durationMs * sampleRate) / 1000)
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (sampleRate / TONE_FREQ_HZ)
            val sample = (sin(angle) * 32767).toInt().toShort()
            generatedSnd[2 * i] = (sample.toInt() and 0xFF).toByte()
            generatedSnd[2 * i + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        return generatedSnd
    }

    @Throws(IOException::class)
    private fun createTempSoundFile(soundData: ByteArray): File {
        val tempFile = File.createTempFile("tone", ".pcm", cacheDir)
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { fos -> fos.write(soundData) }
        return tempFile
    }

    @SuppressLint("SetTextI18n")
    private fun setupButtonClick() {
        startStopButton.setOnClickListener {
            if (isTimerRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        resetButton.setOnClickListener {
            inputBuffer = ""
            timeInput.setText("00:00")
            calculateSpeed()
        }

        homeButton3.setOnClickListener {
            finish() // Закрывает текущую Activity и возвращается на предыдущую
        }
    }

    private fun setupInputListeners() {
        volumeInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateSpeed()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupTimeInputWatcher() {
        timeInput.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return
                val text = s.toString().filter { it.isDigit() }
                inputBuffer = text

                val formatted = formatInputBuffer(text)
                if (formatted != timeInput.text.toString()) {
                    isUpdating = true
                    timeInput.setText(formatted)
                    timeInput.setSelection(formatted.length)
                    isUpdating = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                calculateSpeed()
            }
        })
    }

    private fun formatInputBuffer(input: String): String {
        return when (input.length) {
            0 -> "00:00"
            1 -> "00:0$input"
            2 -> "00:${input.takeLast(2)}"
            3 -> "0${input[0]}:${input.takeLast(2)}"
            else -> "${input.takeLast(4).take(2)}:${input.takeLast(2)}"
        }
    }

    private fun startTimer() {
        val timeStr = timeInput.text.toString().trim()
        if (timeStr.isEmpty()) {
            timeInput.error = "Введите время"
            return
        }

        val (minutes, seconds) = parseTime(timeStr) ?: run {
            timeInput.error = "Формат: ММ:СС"
            return
        }

        targetSeconds = minutes * 60 + seconds
        if (targetSeconds <= 0) {
            timeInput.error = "Время > 0"
            return
        }

        timerText.text = formatTime(0)
        timerText.visibility = View.VISIBLE
        timeInput.visibility = View.GONE
        resetButton.visibility = View.GONE
        startStopButton.text = "Стоп"

        countDownTimer = object : CountDownTimer(targetSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                val elapsed = targetSeconds - secondsLeft
                timerText.text = formatTime(elapsed)

                if (secondsLeft <= 5L) {
                    soundPool.play(shortSoundId, 1f, 1f, 0, 0, 1f)
                }
            }

            override fun onFinish() {
                soundPool.play(longSoundId, 1f, 1f, 0, 0, 1f)
                timerFinished()
            }
        }.start()

        isTimerRunning = true
        calculateSpeed()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        isTimerRunning = false
        startStopButton.text = "Старт"
        timeInput.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        timerText.visibility = View.GONE

        val elapsedText = timerText.text.toString()
        val (minutes, seconds) = parseTime(elapsedText) ?: (0L to 0L)
        val formatted = formatTime(minutes * 60 + seconds)
        inputBuffer = formatted.replace(":", "")
        timeInput.setText(formatted)
        calculateSpeed()
    }

    private fun timerFinished() {
        isTimerRunning = false
        startStopButton.text = "Старт"
        timerText.visibility = View.GONE
        timeInput.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        timeInput.setText(formatTime(targetSeconds))
        inputBuffer = formatTime(targetSeconds).replace(":", "")
        calculateSpeed()
    }

    private fun parseTime(timeStr: String): Pair<Long, Long>? {
        val parts = timeStr.split(":").map { it.trim() }
        if (parts.size != 2) return null
        val minutes = parts[0].toLongOrNull() ?: return null
        val seconds = parts[1].toLongOrNull() ?: return null
        if (seconds >= 60 || minutes < 0 || seconds < 0) return null
        return minutes to seconds
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    @SuppressLint("SetTextI18n")
    private fun calculateSpeed() {
        val timeStr = if (isTimerRunning) {
            timerText.text.toString()
        } else {
            timeInput.text.toString()
        }
        val volumeStr = volumeInput.text.toString().trim()

        if (volumeStr.isEmpty()) {
            speedResult.text = "Скорость: — мл/час"
            return
        }

        val volume = volumeStr.toDoubleOrNull()
        if (volume == null || volume < 0) {
            speedResult.text = "Ошибка: объём ≥ 0"
            return
        }

        val (minutes, seconds) = parseTime(timeStr) ?: (0L to 0L)
        val totalMinutes = minutes + seconds / 60.0
        if (totalMinutes <= 0) {
            speedResult.text = "Скорость: — мл/час"
            return
        }

        val speed = (volume / totalMinutes) * 60
        speedResult.text = "Скорость: ${"%.1f".format(speed)} мл/час"
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        soundPool.release()
        super.onDestroy()
    }
}