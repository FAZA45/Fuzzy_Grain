package com.example.latihan5_skripsi

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.latihan5_skripsi.databinding.ActivityGrafikBinding
import java.text.SimpleDateFormat
import java.util.*

class GrafikActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGrafikBinding

    private lateinit var tvSuhu: TextView
    private lateinit var tvKelembapan: TextView
    private lateinit var tvKadarAirA: TextView
    private lateinit var tvKadarAirB: TextView
    private lateinit var tvKadarAirC: TextView
    private lateinit var tvStatusKondisi: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var btnHome: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTimeRunnable: Runnable
    private lateinit var headerPollRunnable: Runnable

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGrafikBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🌈 Status bar & nav bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#1976D2")
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        // Bind view
        tvSuhu = findViewById(R.id.tvSuhu)
        tvKelembapan = findViewById(R.id.tvKelembapan)
        tvKadarAirA = findViewById(R.id.tvKadarAirA)
        tvKadarAirB = findViewById(R.id.tvKadarAirB)
        tvKadarAirC = findViewById(R.id.tvKadarAirC)
        tvStatusKondisi = findViewById(R.id.tvStatusKondisi)
        tvTanggal = findViewById(R.id.tvTanggal)
        btnHome = findViewById(R.id.btnHome)

        // Start Firebase Realtime (idempotent)
        DataCenter.startFirebaseRealtime()

        // Update header realtime pertama kali
        updateHeaderFromDataCenter()

        // Waktu realtime
        updateTimeRunnable = object : Runnable {
            override fun run() {
                val format = SimpleDateFormat("HH:mm - EEEE, dd MMMM yyyy", Locale("id", "ID"))
                tvTanggal.text = format.format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)

        // Poll header setiap 1.5 detik agar sinkron
        headerPollRunnable = object : Runnable {
            override fun run() {
                updateHeaderFromDataCenter()
                handler.postDelayed(this, 1500)
            }
        }
        handler.post(headerPollRunnable)

        // Tombol Navigasi
        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnGrafikSuhu.setOnClickListener {
            startActivity(Intent(this, GrafikSuhuActivity::class.java))
        }
        binding.btnGrafikKelembapan.setOnClickListener {
            startActivity(Intent(this, GrafikKelembapanActivity::class.java))
        }
        binding.btnGrafikKadarAir.setOnClickListener {
            startActivity(Intent(this, GrafikKadarAirActivity::class.java))
        }
        binding.btnGrafikKondisi.setOnClickListener {
            startActivity(Intent(this, GrafikKondisiActivity::class.java))
        }
    }

    // Header realtime
    @SuppressLint("SetTextI18n")
    private fun updateHeaderFromDataCenter() {

        // ===== SUHU =====
        tvSuhu.text = if (DataCenter.suhu == null)
            "ERROR"
        else
            "%.1f°C".format(DataCenter.suhu)

        // ===== KELEMBAPAN =====
        tvKelembapan.text = if (DataCenter.kelembapan == null)
            "ERROR"
        else
            "Kelembapan Udara : %.1f%%".format(DataCenter.kelembapan)

        // ===== KADAR AIR =====
        tvKadarAirA.text = if (DataCenter.kadarAirA == null)
            "A = ERROR"
        else
            "A = %.1f%%".format(DataCenter.kadarAirA)

        tvKadarAirB.text = if (DataCenter.kadarAirB == null)
            "B = ERROR"
        else
            "B = %.1f%%".format(DataCenter.kadarAirB)

        tvKadarAirC.text = if (DataCenter.kadarAirC == null)
            "C = ERROR"
        else
            "C = %.1f%%".format(DataCenter.kadarAirC)

        // ===== STATUS =====
        tvStatusKondisi.text = DataCenter.kondisi
        ubahWarnaStatus(DataCenter.kondisi)
    }


    private fun ubahWarnaStatus(status: String) {
        when (status.uppercase(Locale.getDefault())) {
            "AMAN" -> tvStatusKondisi.setTextColor(Color.parseColor("#67FF48"))
            "WASPADA" -> tvStatusKondisi.setTextColor(Color.parseColor("#FFEB3B"))
            "PERINGATAN" -> tvStatusKondisi.setTextColor(Color.parseColor("#FF9800"))
            "BERBAHAYA" -> tvStatusKondisi.setTextColor(Color.parseColor("#C62828"))
            else -> tvStatusKondisi.setTextColor(Color.BLACK)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
        handler.removeCallbacks(headerPollRunnable)
    }
}