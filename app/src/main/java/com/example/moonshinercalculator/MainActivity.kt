package com.example.moonshinercalculator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_braga_height).setOnClickListener {
            startActivity(Intent(this, BragHeightActivity::class.java))
        }

        findViewById<Button>(R.id.btn_braga_volume).setOnClickListener {
            startActivity(Intent(this, BragVolumeActivity::class.java))
        }

        findViewById<Button>(R.id.btn_dilution).setOnClickListener {
            startActivity(Intent(this, DilutionActivity::class.java))
        }

        findViewById<Button>(R.id.btn_corrector).setOnClickListener {
            startActivity(Intent(this, SpirtCorrectorActivity::class.java)) // ← С большой буквы
        }

        findViewById<Button>(R.id.btn_fractional).setOnClickListener {
            startActivity(Intent(this, FractionalActivity::class.java))
        }

        findViewById<Button>(R.id.btn_refractometr).setOnClickListener {
            startActivity(Intent(this, RefractometrCorrectActivity::class.java))
        }
        findViewById<Button>(R.id.btn_kokteil).setOnClickListener {
            startActivity(Intent(this, KokteilActivity::class.java))
        }
        findViewById<Button>(R.id.btn_timer).setOnClickListener {
            startActivity(Intent(this, TimerActivity::class.java))
        }
        findViewById<Button>(R.id.btn_info).setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
        }
    }

}