package com.example.latihan5_skripsi

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper
import android.content.Intent
import com.google.firebase.FirebaseApp


class MainActivity : AppCompatActivity() {

    private lateinit var tvSuhu: TextView
    private lateinit var tvKelembapan: TextView
    private lateinit var tvKadarAirA: TextView
    private lateinit var tvKadarAirB: TextView
    private lateinit var tvKadarAirC: TextView
    private lateinit var tvStatusGudang: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var btnGrafik: Button
    private lateinit var btnTabel: Button
    private lateinit var imgHome: ImageView

    // 🔹 Tambahan untuk jam real-time
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTimeRunnable: Runnable

    // 🔹 Handler untuk auto-refresh UI realtime Firebase
    private val uiHandler = Handler(Looper.getMainLooper())
    private val uiUpdater = object : Runnable {
        override fun run() {
            updateRealtimeUI()
            uiHandler.postDelayed(this, 500) // Refresh tiap 0.5 detik
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        // DataCenter.generateDummyData()     // dummy dulu
        DataCenter.startFirebaseRealtime() // kalau ada Firebase → override dummy

        setContentView(R.layout.activity_main)

        // 🌈 Buat status bar transparan modern
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#1976D2")
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        // Hubungkan ID layout
        tvSuhu = findViewById(R.id.tvSuhu)
        tvKelembapan = findViewById(R.id.tvKelembapan)
        tvKadarAirA = findViewById(R.id.tvKadarAirA)
        tvKadarAirB = findViewById(R.id.tvKadarAirB)
        tvKadarAirC = findViewById(R.id.tvKadarAirC)
        tvStatusGudang = findViewById(R.id.tvStatusGudang)
        tvTanggal = findViewById(R.id.tvTanggal)
        btnGrafik = findViewById(R.id.btnGrafik)
        btnTabel = findViewById(R.id.btnTabel)
        imgHome = findViewById(R.id.imgHome)

        imgHome.setOnClickListener {
            // Sudah di dashboard, tidak melakukan apa-apa
        }

        // 🔹 Pasang UI awal dari DataCenter
        updateRealtimeUI()

        // 🔥 Setiap DataCenter menerima data baru → update UI otomatis
        DataCenter.onRealtimeUpdate = {
            runOnUiThread { updateRealtimeUI() }
        }

        // 🔹 Update waktu real-time
        updateTimeRunnable = object : Runnable {
            override fun run() {
                val calendar = Calendar.getInstance()
                val formatTanggal = SimpleDateFormat("HH:mm - EEEE, dd MMMM yyyy", Locale("id", "ID"))
                tvTanggal.text = formatTanggal.format(calendar.time)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)

        // 🔹 Navigasi ke halaman Tabel
        btnTabel.setOnClickListener {
            val intent = Intent(this, TabelActivity::class.java)
            startActivity(intent)
        }

        // 🔹 Navigasi ke halaman Grafik
        val btnGrafik = findViewById<Button>(R.id.btnGrafik)
        btnGrafik.setOnClickListener {
            val intent = Intent(this, GrafikActivity::class.java)
            startActivity(intent)
        }

        // 🔥 Mulai auto-refresh UI Firebase
        uiHandler.post(uiUpdater)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
        uiHandler.removeCallbacks(uiUpdater)
    }

    // 🔥 Fungsi update UI realtime Firebase
    @SuppressLint("SetTextI18n")
    private fun updateRealtimeUI() {

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
        tvStatusGudang.text = DataCenter.kondisi
        ubahWarnaStatus(DataCenter.kondisi)
    }

    // 🔹 Fungsi ubah warna kondisi (oval dinamis)
    private fun ubahWarnaStatus(status: String) {
        val background = tvStatusGudang.background.mutate() as GradientDrawable
        val warna = when (status.uppercase()) {

            "AMAN" -> Color.parseColor("#67FF48")          // Hijau terang
            "WASPADA" -> Color.parseColor("#FFEB3B")       // Kuning
            "PERINGATAN" -> Color.parseColor("#FF9800")    // Oranye
            "BERBAHAYA" -> Color.parseColor("#C62828")     // Merah
            else -> Color.parseColor("#8E24AA")            // Default ungu
        }
        background.setColor(warna)
        background.cornerRadius = 100f
        tvStatusGudang.background = background
    }
}
