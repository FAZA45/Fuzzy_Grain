package com.example.latihan5_skripsi

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.util.Locale

class MarkerCustom(context: Context) :
    MarkerView(context, R.layout.custom_marker) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    // 🎯 MODE: false = sensor (angka), true = kondisi (AMAN/WASPADA/...)
    var isStatusMode: Boolean = false

    override fun refreshContent(e: Entry?, highlight: Highlight?) {

        if (isStatusMode) {
            // 🎯 MODE KONDISI
            tvContent.text = when (e?.y?.toInt()) {
                1 -> "AMAN"
                2 -> "WASPADA"
                3 -> "PERINGATAN"
                4 -> "BERBAHAYA"
                else -> "-"
            }
        } else {
            // 🎯 MODE SENSOR (angka 1 decimal)
            tvContent.text = String.format(Locale("id", "ID"), "%.1f", e?.y ?: 0f)
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
