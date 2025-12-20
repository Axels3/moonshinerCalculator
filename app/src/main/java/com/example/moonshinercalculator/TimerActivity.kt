package com.example.moonshinercalculator

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
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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
    private lateinit var backButton: ImageView

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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_timer)

        // Создаем временные файлы для звуков (WAV)
        try {
            shortToneFile = createWavFile(createToneData(SHORT_TONE_MS))
            longToneFile = createWavFile(createToneData(LONG_TONE_MS))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        initViews()
        initSound()
        setupViews()
        setupButtonClick()
        setupInputListeners()
        setupTimeInputWatcher()
    }

    private fun initViews() {
        timerText = findViewById(R.id.timerText)
        startStopButton = findViewById(R.id.button)
        timeInput = findViewById(R.id.timeInput)
        resetButton = findViewById(R.id.resetButton)
        volumeInput = findViewById(R.id.volumeInput)
        speedResult = findViewById(R.id.speedResult)
        backButton = findViewById(R.id.homeButton)
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

        // Загружаем звуки
        shortSoundId = soundPool.load(shortToneFile.absolutePath, 1)
        longSoundId = soundPool.load(longToneFile.absolutePath, 1)

        // Проверка загрузки
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                // Можно логировать ошибку
            }
        }
    }


    private fun createToneData(durationMs: Int): ByteArray {
        val sampleRate = 8000
        val numSamples = (durationMs * sampleRate / 1000)
        val generatedSnd = ByteArray(2 * numSamples) // 16-bit PCM

        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (sampleRate / TONE_FREQ_HZ)
            val sample = (kotlin.math.sin(angle) * 32767).toInt()
            generatedSnd[2 * i] = (sample and 0xFF).toByte()
            generatedSnd[2 * i + 1] = (sample shr 8 and 0xFF).toByte()
        }

        return generatedSnd
    }

    private fun createWavFile(pcmData: ByteArray): File {
        val wavFile = File.createTempFile("tone", ".wav", cacheDir)
        wavFile.deleteOnExit()

        FileOutputStream(wavFile).use { fos ->
            val channels = 1
            val bitsPerSample = 16
            val byteRate = 8000 * channels * bitsPerSample / 8
            val blockAlign = (channels * bitsPerSample / 8).toShort()
            val dataSize = pcmData.size
            val riffChunkSize = 36 + dataSize

            // RIFF header
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(riffChunkSize))
            fos.write("WAVE".toByteArray())

            // fmt subchunk
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16))
            fos.write(shortToByteArray(1))
            fos.write(shortToByteArray(channels.toShort()))
            fos.write(intToByteArray(8000))
            fos.write(intToByteArray(byteRate))
            fos.write(shortToByteArray(blockAlign))
            fos.write(shortToByteArray(bitsPerSample.toShort()))

            // data subchunk
            fos.write("data".toByteArray())
            fos.write(intToByteArray(dataSize))
            fos.write(pcmData)
        }

        return wavFile
    }



    private fun intToByteArray(i: Int): ByteArray = byteArrayOf(
        (i and 0xff).toByte(),
        (i shr 8 and 0xff).toByte(),
        (i shr 16 and 0xff).toByte(),
        (i shr 24 and 0xff).toByte()
    )


    private fun shortToByteArray(s: Short): ByteArray = byteArrayOf(
        (s.toInt() and 0xff).toByte(),
        (s.toInt() shr 8 and 0xff).toByte()
    )

    private fun setupViews() {
        timerText.visibility = View.GONE
        timeInput.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE
        volumeInput.visibility = View.VISIBLE
        speedResult.visibility = View.VISIBLE
        timeInput.setText("00:00")
        volumeInput.setText("0")
        calculateSpeed()

        timeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        volumeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

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

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupInputListeners() {
        volumeInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateSpeed()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupTimeInputWatcher() {
        timeInput.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

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

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })
    }

    private fun formatInputBuffer(input: String): String = when (input.length) {
        0 -> "00:00"
        1 -> "00:0$input"
        2 -> "00:${input.takeLast(2)}"
        3 -> "0${input[0]}:${input.takeLast(2)}"
        else -> "${input.takeLast(4).take(2)}:${input.takeLast(2)}"
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

                // Короткий звук за 2 и 1 секунду
                if (secondsLeft == 2L || secondsLeft == 1L) {
                    soundPool.play(shortSoundId, 1f, 1f, 0, 0, 1f)
                }
            }

            override fun onFinish() {
                // Длинный звук на последней секунде (500 мс)
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
        timerText.visibility = View.GONE
        timeInput.visibility = View.VISIBLE
        resetButton.visibility = View.VISIBLE

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

    private fun formatTime(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun calculateSpeed() {
        val timeStr = if (isTimerRunning) timerText.text.toString() else timeInput.text.toString()
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