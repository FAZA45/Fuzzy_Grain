package com.example.latihan5_skripsi

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.latihan5_skripsi.databinding.ActivityGrafikSuhuBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class GrafikSuhuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGrafikSuhuBinding
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

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateTimeRunnable: Runnable
    private lateinit var headerPollRunnable: Runnable

    private var modeFilter = "Perjam"
    private var selectedDateForPerjam = ""       // dd-MM-yyyy
    private var selectedMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGrafikSuhuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#1976D2")
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        // bind views (sama nama seperti TabelActivity)
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

        // Realtime
        // DataCenter.startFirebaseRealtime()
        updateHeaderFromDataCenter()

        // waktu realtime (update tiap 1 detik)
        updateTimeRunnable = object : Runnable {
            override fun run() {
                val format = SimpleDateFormat("HH:mm - EEEE, dd MMMM yyyy", Locale("id", "ID"))
                tvTanggal.text = format.format(Date())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTimeRunnable)

        // header polling 1.5s
        headerPollRunnable = object : Runnable {
            override fun run() {
                updateHeaderFromDataCenter()
                handler.postDelayed(this, 1500)
            }
        }
        handler.post(headerPollRunnable)

        // setup filter & tanggal (sama logic seperti TabelActivity)
        setupFilterMenu()
        setupTanggalButton()

        // Default: pilih tanggal terbaru (descending)
        DataCenter.getListTanggal { listTanggal ->
            runOnUiThread {

                if (listTanggal.isNotEmpty()) {
                    // Paksa sort descending berdasarkan timestamp
                    val sorted = listTanggal.sortedByDescending {
                        SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
                            .parse(it)?.time ?: 0L
                    }
                    selectedDateForPerjam = sorted.first()
                }
                else {
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
                    selectedDateForPerjam = sdf.format(Date())
                }

                btnTanggal.text = "$selectedDateForPerjam ▾"
                tampilkanGrafikPerjam(selectedDateForPerjam)
            }
        }

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    // header: gunakan nama & isi sama dengan TabelActivity
    private fun updateHeaderFromDataCenter() {

        // suhu
        tvSuhu.text =
            if (DataCenter.suhuStr.equals("ERROR", true))
                "ERROR"
            else
                "${DataCenter.suhuStr}°C"

        // kelembapan
        tvKelembapan.text =
            if (DataCenter.kelembapanStr.equals("ERROR", true))
                "Kelembapan Udara : ERROR"
            else
                "Kelembapan Udara : ${DataCenter.kelembapanStr}%"

        // kadar air
        tvKadarAirA.text =
            if (DataCenter.kadarAirAStr.equals("ERROR", true))
                "A = ERROR"
            else
                "A = ${DataCenter.kadarAirAStr}%"

        tvKadarAirB.text =
            if (DataCenter.kadarAirBStr.equals("ERROR", true))
                "B = ERROR"
            else
                "B = ${DataCenter.kadarAirBStr}%"

        tvKadarAirC.text =
            if (DataCenter.kadarAirCStr.equals("ERROR", true))
                "C = ERROR"
            else
                "C = ${DataCenter.kadarAirCStr}%"

        // status gudang (INI SUDAH BENAR DARI AWAL)
        val s = DataCenter.kondisi.trim().uppercase()
        tvStatusKondisi.text = DataCenter.kondisi

        when (s) {
            "AMAN" -> tvStatusKondisi.setTextColor(Color.parseColor("#67FF48"))
            "WASPADA" -> tvStatusKondisi.setTextColor(Color.parseColor("#FFEB3B"))
            "PERINGATAN" -> tvStatusKondisi.setTextColor(Color.parseColor("#FF9800"))
            "BERBAHAYA" -> tvStatusKondisi.setTextColor(Color.parseColor("#C62828"))
            else -> tvStatusKondisi.setTextColor(Color.BLACK)
        }
    }


    private fun setupFilterMenu() {
        btnFilter.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_filter, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_perjam -> {
                        modeFilter = "Perjam"
                        btnFilter.text = "Per Jam ▼"
                        if (selectedDateForPerjam.isNotEmpty()) btnTanggal.text = "$selectedDateForPerjam ▾"
                        tampilkanGrafikPerjam(selectedDateForPerjam)
                    }
                    R.id.menu_harian -> {
                        modeFilter = "Harian"
                        btnFilter.text = "Harian ▼"
                        val months = arrayOf(
                            "Januari","Februari","Maret","April","Mei","Juni",
                            "Juli","Agustus","September","Oktober","November","Desember")
                        btnTanggal.text = "${months[selectedMonthIndex]} $selectedYear ▾"
                        tampilkanGrafikHarian(selectedMonthIndex, selectedYear)
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun setupTanggalButton() {
        btnTanggal.setOnClickListener {
            val cal = Calendar.getInstance()
            val localeID = Locale("id", "ID")

            if (modeFilter == "Perjam") {
                DatePickerDialog(
                    this,
                    { _, year, month, day ->
                        val date = Calendar.getInstance().apply { set(year, month, day) }.time
                        val fmt = SimpleDateFormat("dd-MM-yyyy", localeID)
                        selectedDateForPerjam = fmt.format(date)
                        btnTanggal.text = "$selectedDateForPerjam ▾"
                        tampilkanGrafikPerjam(selectedDateForPerjam)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            } else {
                showMonthYearPicker { monthIndex, year ->
                    selectedMonthIndex = monthIndex
                    selectedYear = year
                    val months = arrayOf(
                        "Januari","Februari","Maret","April","Mei","Juni",
                        "Juli","Agustus","September","Oktober","November","Desember")
                    btnTanggal.text = "${months[monthIndex]} $year ▾"
                    tampilkanGrafikHarian(monthIndex, year)
                }
            }
        }
    }

    private fun showMonthYearPicker(onPicked: (Int, Int) -> Unit) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(40, 20, 40, 20)
            gravity = android.view.Gravity.CENTER
        }

        val npMonth = NumberPicker(this).apply {
            minValue = 0
            maxValue = 11
            displayedValues = arrayOf(
                "Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
            value = selectedMonthIndex
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val npYear = NumberPicker(this).apply {
            minValue = currentYear - 10
            maxValue = currentYear + 5
            value = selectedYear
        }

        container.addView(npMonth)
        container.addView(npYear)

        AlertDialog.Builder(this)
            .setTitle("Pilih Bulan")
            .setView(container)
            .setPositiveButton("OK") { _, _ -> onPicked(npMonth.value, npYear.value) }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --------------------------- PER JAM ---------------------------
    private fun tampilkanGrafikPerjam(tanggal: String) {
        DataCenter.getRiwayatByTanggal(tanggal) { list ->
            // WAJIB: sort DESCENDING → data baru di kiri grafik
            val sorted = list.sortedByDescending { it.waktu }

            // list sudah diurutkan di DataCenter berdasarkan jam
            val entries = ArrayList<Entry>()
            val xLabels = ArrayList<String>()

            for ((i, item) in sorted.withIndex()) {
                val y = try { item.suhu.toFloat() } catch (_: Exception) { 0f }
                entries.add(Entry(i.toFloat(), y))
                xLabels.add(item.waktu.split(":").take(2).joinToString(":"))
            }

            runOnUiThread { buatGrafik(entries, xLabels) }
        }
    }

    // --------------------------- HARIAN ---------------------------
    private fun tampilkanGrafikHarian(monthIndex: Int, year: Int) {
        DataCenter.getListTanggal { listTanggal ->

            val filtered = listTanggal.filter { tgl ->
                try {
                    val p = tgl.split("-")
                    p[1].toInt() - 1 == monthIndex && p[2].toInt() == year
                } catch (e: Exception) { false }
            }

            if (filtered.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, "Tidak ada data.", Toast.LENGTH_SHORT).show()
                    buatGrafik(emptyList(), emptyList())
                }
                return@getListTanggal
            }

            val results = mutableListOf<Pair<Long, Float>>() // timestamp -> avg suhu
            val labelsMap = mutableMapOf<Long, String>()
            var done = 0

            val inFmt = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
            val outFmt = SimpleDateFormat("dd-MMM", Locale("id", "ID"))

            // SORT DESCENDING by date (terbaru di kanan grafik)
            val filteredDesc = filtered.sortedByDescending {
                inFmt.parse(it)?.time ?: 0L
            }

            for (tanggal in filteredDesc) {
                DataCenter.getRiwayatByTanggal(tanggal) { jamList ->

                    val avg = if (jamList.isNotEmpty()) {
                        jamList.map { it.suhu.toFloatOrNull() ?: 0f
                        }.average().toFloat()
                    } else 0f

                    val date = inFmt.parse(tanggal)!!
                    val ts = date.time

                    results.add(ts to avg)
                    labelsMap[ts] = outFmt.format(date)

                    done++
                    if (done == filteredDesc.size) {
                        val sorted = results.sortedByDescending { it.first }
                        val entries = sorted.mapIndexed { idx, p ->
                            Entry(idx.toFloat(), p.second)
                        }
                        val labels = sorted.map { labelsMap[it.first]!! }
                        runOnUiThread { buatGrafik(entries, labels) }
                    }
                }
            }
        }
    }

    // --------------------------- GRAFIK ---------------------------
    private fun buatGrafik(
        entries: List<Entry>,
        xLabels: List<String> = emptyList()
    ) {
        val chart = binding.lineChartSuhu

        val dataSet = LineDataSet(entries, "Suhu Udara").apply {
            color = Color.parseColor("#42A5F5")
            lineWidth = 3f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#42A5F5"))
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
        }

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return String.format(Locale("id", "ID"), "%.1f", entry?.y ?: 0f)
            }
        }

        chart.data = LineData(dataSet)
        chart.description.isEnabled = false
        chart.animateX(1000)

        // Zoom bebas
        chart.setPinchZoom(true)
        chart.isDoubleTapToZoomEnabled = true
        chart.viewPortHandler.setMaximumScaleY(5f)
        chart.viewPortHandler.setMinimumScaleY(1f)


        // -----------------------------------------------------------
        // 1) LEGEND seperti grafik kadar air (di atas, tengah)
        // -----------------------------------------------------------
        chart.legend.apply {
            isEnabled = true
            verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            textSize = 12f
            formSize = 10f
        }

        // -----------------------------------------------------------
        // 2) Offset bawah agar sumbu X tidak terpotong (masalah utama)
        // -----------------------------------------------------------
        chart.setExtraOffsets(0f, 10f, 0f, 25f)
        chart.setExtraBottomOffset(20f)   // WAJIB! fixes X-axis clipping

        // -----------------------------------------------------------
        // 3) X Axis – mengikuti grafik kadar air
        // -----------------------------------------------------------
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(xLabels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            textColor = Color.DKGRAY
            labelRotationAngle = -30f
            yOffset = 12f            // tambahan penting agar tidak terpotong
        }

        // -----------------------------------------------------------
        // 4) Y Axis kiri tetap seperti sebelumnya
        // -----------------------------------------------------------
        chart.axisLeft.apply {
            textColor = Color.DKGRAY
            setDrawGridLines(true)
            axisMinimum = 0f
            axisMaximum = 100f
            setLabelCount(11, true)   // opsional supaya rapi: 0,20,40,60,80,100
        }

        chart.axisRight.isEnabled = false

        // -----------------------------------------------------------
        // 5) Marker tetap
        // -----------------------------------------------------------
        val marker = MarkerCustom(this)
        marker.isStatusMode = false     // ← gunakan mode angka
        marker.chartView = chart
        chart.marker = marker

        chart.post {
            chart.calculateOffsets()
            chart.invalidate()
        }

        chart.requestLayout()
        chart.invalidate()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
        handler.removeCallbacks(headerPollRunnable)
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTimeRunnable)
        handler.post(headerPollRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable)
        handler.removeCallbacks(headerPollRunnable)
    }
}
