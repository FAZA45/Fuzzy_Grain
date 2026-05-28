package com.example.latihan5_skripsi

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.latihan5_skripsi.adapter.TabelAdapter
import com.example.latihan5_skripsi.model.DataTabel
import java.text.SimpleDateFormat
import java.util.*

class TabelActivity : AppCompatActivity() {

    private lateinit var tvSuhu: TextView
    private lateinit var tvKelembapan: TextView
    private lateinit var tvKadarAirA: TextView
    private lateinit var tvKadarAirB: TextView
    private lateinit var tvKadarAirC: TextView
    private lateinit var tvStatusKondisi: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var btnFilter: Button
    private lateinit var btnTanggal: Button
    private lateinit var btnHome: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabelAdapter: TabelAdapter

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTimeRunnable: Runnable

    private val dataList = mutableListOf<DataTabel>()

    // FILTER state
    private var modeFilter = "Perjam"       // "Perjam" or "Harian"
    private var selectedDateForPerjam = ""  // dd-MM-yyyy (e.g. 01-12-2025)
    private var selectedMonthIndex = Calendar.getInstance().get(Calendar.MONTH) // 0..11
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabel)

        // Make status bar consistent with your theme (kept same behaviour as before)
        // 🌈 Status bar & nav bar transparan
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#1976D2")
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        // init views
        tvSuhu = findViewById(R.id.tvSuhu)
        tvKelembapan = findViewById(R.id.tvKelembapan)
        tvKadarAirA = findViewById(R.id.tvKadarAirA)
        tvKadarAirB = findViewById(R.id.tvKadarAirB)
        tvKadarAirC = findViewById(R.id.tvKadarAirC)
        tvStatusKondisi = findViewById(R.id.tvStatusKondisi)
        tvTanggal = findViewById(R.id.tvTanggal)
        btnFilter = findViewById(R.id.btnFilter)
        btnTanggal = findViewById(R.id.btnTanggal)
        btnHome = findViewById(R.id.btnHome)
        recyclerView = findViewById(R.id.recyclerTabel)

        recyclerView.layoutManager = LinearLayoutManager(this)
        tabelAdapter = TabelAdapter(dataList)
        recyclerView.adapter = tabelAdapter

        // Start realtime listener (idempotent)
        // DataCenter.startFirebaseRealtime()

        // show current realtime summary in header
        updateHeaderFromDataCenter()

        // update header time (top bar)
        updateTimeRunnable = object : Runnable {
            override fun run() {
                val format = SimpleDateFormat("HH:mm - EEEE, dd MMMM yyyy", Locale("id", "ID"))
                tvTanggal.text = format.format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)

        // Setup filter menu & tanggal button
        setupFilterWaktu()
        setupTanggalButton()

        // Load default selection:
        // grab list of tanggal from firebase, pick latest -> for Perjam load that date,
        // for Harian we will default to current month/year and show monthly averages.
        DataCenter.getLatestTanggal { latestDate ->
            if (latestDate != null) {

                // pick latest date (list already sorted desc in DataCenter)
                selectedDateForPerjam = latestDate
                // update btnTanggal text based on current mode
                if (modeFilter == "Perjam") {
                    btnTanggal.text = "$selectedDateForPerjam ▼"
                    loadRiwayatPerjam(selectedDateForPerjam)
                } else {
                    // if Harian, set btn text to month-year
                    val months = arrayOf(
                        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                    )
                    btnTanggal.text = "${months[selectedMonthIndex]} $selectedYear ▾"
                    loadRiwayatHarian()
                }
            } else {
                // no dates yet
                btnTanggal.text = "Pilih Tanggal ▼"
            }
        }

        // keep header summary updated each time realtime changes (DataCenter updates its vars)
        // We can poll small interval to reflect DataCenter changes (safe).
        // Alternatively, if you already update header elsewhere, remove this.
        handler.post(object : Runnable {
            override fun run() {
                updateHeaderFromDataCenter()
                handler.postDelayed(this, 1500)
            }
        })

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    // update header text from DataCenter global vars
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


    // ----------------------------
    // FILTER SETUP
    // ----------------------------
    private fun setupFilterWaktu() {
        btnFilter.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_filter, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_perjam -> {
                        modeFilter = "Perjam"
                        btnFilter.text = "Per Jam ▼"
                        // If we have a selected date, ensure button text shows it
                        if (selectedDateForPerjam.isNotEmpty()) btnTanggal.text = "$selectedDateForPerjam ▼"
                    }
                    R.id.menu_harian -> {
                        modeFilter = "Harian"
                        btnFilter.text = "Harian ▼"
                        // show month name in btnTanggal
                        val months = arrayOf(
                            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                        )
                        btnTanggal.text = "${months[selectedMonthIndex]} $selectedYear ▾"
                    }
                }
                // reload according to selected mode
                if (modeFilter == "Perjam") {
                    if (selectedDateForPerjam.isNotEmpty()) loadRiwayatPerjam(selectedDateForPerjam)
                } else {
                    loadRiwayatHarian()
                }
                true
            }

            popup.show()
        }
    }

    // ----------------------------
    // TANGGAL / BULAN PICKER
    // ----------------------------
    private fun setupTanggalButton() {
        btnTanggal.setOnClickListener {
            if (modeFilter == "Perjam") {
                // choose single date
                val cal = Calendar.getInstance()
                val dialog = DatePickerDialog(
                    this,
                    { _, year, month, day ->
                        selectedDateForPerjam = String.format("%02d-%02d-%04d", day, month + 1, year)
                        btnTanggal.text = "$selectedDateForPerjam ▼"
                        loadRiwayatPerjam(selectedDateForPerjam)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                dialog.show()
            } else {
                // choose month + year for Harian mode
                showMonthYearPicker { monthIndex, year ->
                    selectedMonthIndex = monthIndex
                    selectedYear = year
                    val months = arrayOf(
                        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                    )
                    btnTanggal.text = "${months[selectedMonthIndex]} $selectedYear ▾"
                    loadRiwayatHarian()
                }
            }
        }
    }

    private fun showMonthYearPicker(onPicked: (Int, Int) -> Unit) {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )

        // Root vertical layout (full custom view)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 18.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        // Custom title centered
        val titleView = TextView(this).apply {
            text = "Pilih Bulan & Tahun"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE) // Agar kontras dengan dialog gelap
            gravity = android.view.Gravity.CENTER
            // padding bawah agar tidak rapat dengan picker
            setPadding(0, 0, 0, 12.dpToPx())
        }
        root.addView(titleView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Container for pickers centered horizontally
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        // Month NumberPicker
        val npMonth = NumberPicker(this).apply {
            minValue = 0
            maxValue = 11
            displayedValues = months
            value = selectedMonthIndex
            wrapSelectorWheel = true
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        // Year NumberPicker
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val npYear = NumberPicker(this).apply {
            minValue = currentYear - 5
            maxValue = currentYear + 5
            value = selectedYear
            wrapSelectorWheel = false
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        // Layout params with spacing
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(32.dpToPx(), 0, 32.dpToPx(), 0)

        container.addView(npMonth, lp)
        container.addView(npYear, lp)

        root.addView(container)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )            .setView(root)
            .setPositiveButton("OK") { _, _ ->
                onPicked(npMonth.value, npYear.value)
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()

        // Set dialog width to 60% of screen to avoid full-width stretch
        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        dialog.window?.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    // ----------------------------
    // LOAD DATA - PERJAM
    // ----------------------------
    private fun loadRiwayatPerjam(tanggal: String) {
        dataList.clear()
        tabelAdapter.notifyDataSetChanged()

        // tanggal format expected: dd-MM-yyyy
        DataCenter.getRiwayatByTanggal(tanggal) { list ->
            dataList.clear()
            dataList.addAll(list)
            // list items have waktu as "HH:mm:ss" (from DataCenter)
            dataList.sortByDescending { it.waktu } // Descending by time
            tabelAdapter.notifyDataSetChanged()
            if (dataList.isEmpty()) {
                Toast.makeText(this, "Tidak ada data pada $tanggal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ----------------------------
    // LOAD DATA - HARIAN (rata2 per tanggal dalam bulan yang dipilih)
    // ----------------------------
    private fun loadRiwayatHarian() {
        dataList.clear()
        tabelAdapter.notifyDataSetChanged()

        // 1) ambil semua tanggal yg ada di DB
        DataCenter.getListTanggal { listTanggal ->

            // filter tanggal yg sesuai bulan & tahun terpilih
            val filtered = listTanggal.filter { tanggal ->
                // tanggal format dd-MM-yyyy
                try {
                    val parts = tanggal.split("-")
                    val day = parts[0].toInt()
                    val month = parts[1].toInt() - 1
                    val year = parts[2].toInt()
                    month == selectedMonthIndex && year == selectedYear
                } catch (e: Exception) {
                    false
                }
            }

            if (filtered.isEmpty()) {
                Toast.makeText(this, "Tidak ada data untuk bulan yang dipilih", Toast.LENGTH_SHORT).show()
                return@getListTanggal
            }

            // load each date's riwayat, hitung rata2, lalu kumpulkan
            var counter = 0
            for (tanggal in filtered) {
                DataCenter.getRiwayatByTanggal(tanggal) { jamList ->
                    if (jamList.isNotEmpty()) {
                        // convert string fields to double safely
                        val suhuVals = jamList.mapNotNull { it.suhu.toDoubleOrNull() }
                        val humVals = jamList.mapNotNull { it.kelembapan.toDoubleOrNull() }
                        val aVals = jamList.mapNotNull { it.kadarAirA.toDoubleOrNull() }
                        val bVals = jamList.mapNotNull { it.kadarAirB.toDoubleOrNull() }
                        val cVals = jamList.mapNotNull { it.kadarAirC.toDoubleOrNull() }

                        val avgSuhu = if (suhuVals.isNotEmpty()) suhuVals.average() else 0.0
                        val avgHum = if (humVals.isNotEmpty()) humVals.average() else 0.0
                        val avgA = if (aVals.isNotEmpty()) aVals.average() else 0.0
                        val avgB = if (bVals.isNotEmpty()) bVals.average() else 0.0
                        val avgC = if (cVals.isNotEmpty()) cVals.average() else 0.0
                        val lastStatus = jamList.last().kondisi

                        dataList.add(
                            DataTabel(
                                waktu = tanggal,
                                suhu = "%.1f".format(avgSuhu),
                                kelembapan = "%.1f".format(avgHum),
                                kadarAirA = "%.1f".format(avgA),
                                kadarAirB = "%.1f".format(avgB),
                                kadarAirC = "%.1f".format(avgC),
                                kondisi = lastStatus
                            )
                        )
                    }
                    counter++
                    if (counter == filtered.size) {
                        // selesai load semua tanggal -> urutkan berdasarkan tanggal ascending
                        dataList.sortWith(compareByDescending { parseDateStringToMillis(it.waktu) })
                        tabelAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    // helper parse dd-MM-yyyy to epoch millis (for sorting)
    private fun parseDateStringToMillis(dateStr: String): Long {
        return try {
            when {
                dateStr.contains("_") -> {
                    val sdf = SimpleDateFormat("HH:mm:ss_dd-MM-yyyy", Locale("id", "ID"))
                    sdf.parse(dateStr)?.time ?: 0L
                }
                dateStr.contains("-") -> {
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
                    sdf.parse(dateStr)?.time ?: 0L
                }
                else -> 0L
            }
        } catch (e: Exception) { 0L }
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
    }

    // ---------- helper: dp to px ------------
    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}
