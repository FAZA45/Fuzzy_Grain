package com.example.latihan5_skripsi.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.latihan5_skripsi.R
import com.example.latihan5_skripsi.model.DataTabel
import java.text.SimpleDateFormat
import java.util.Locale

class TabelAdapter(private val dataList: List<DataTabel>) :
    RecyclerView.Adapter<TabelAdapter.TabelViewHolder>() {

    inner class TabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktu)
        val tvSuhu: TextView = itemView.findViewById(R.id.tvSuhu)
        val tvKelembapanUdara: TextView = itemView.findViewById(R.id.tvKelembapanUdara)
        val tvKadarAirGabungan: TextView = itemView.findViewById(R.id.tvKadarAirGabungan)
        val tvKondisi: TextView = itemView.findViewById(R.id.tvKondisi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tabel_row, parent, false)
        return TabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabelViewHolder, position: Int) {
        val data = dataList[position]

        val waktuPendek =
            if (data.waktu.contains(":")) {
                // format per jam: HH:mm:ss → HH:mm
                data.waktu.split(":").take(2).joinToString(":")
            } else {
                // Format tanggal dd-MM-yyyy → dd-MMM (13-Nov)
                try {
                    val input = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))
                    val output = SimpleDateFormat("dd-MMM", Locale("id", "ID"))
                    output.format(input.parse(data.waktu)!!)
                } catch (e: Exception) {
                    data.waktu // fallback
                }
            }

        holder.tvWaktu.text = waktuPendek

        holder.tvSuhu.text = data.suhu
        holder.tvKelembapanUdara.text = data.kelembapan

        // Tambah jarak antar sensor
        val airA = formatOneDecimal(data.kadarAirA)
        val airB = formatOneDecimal(data.kadarAirB)
        val airC = formatOneDecimal(data.kadarAirC)

        holder.tvKadarAirGabungan.text = "$airA   |   $airB   |   $airC"

        // holder.tvKadarAirGabungan.setPadding(24, 0, 24, 0)   // tambah ruang kiri & kanan
        holder.tvKondisi.text = data.kondisi

        // 🔹 Ubah warna teks kondisi sesuai nilainya
        when (data.kondisi.uppercase()) {
            "NORMAL" -> holder.tvKondisi.setTextColor(Color.parseColor("#4CAF50")) // hijau
            "KURANG" -> holder.tvKondisi.setTextColor(Color.parseColor("#FF9800")) // oranye
            "BAHAYA" -> holder.tvKondisi.setTextColor(Color.parseColor("#F44336")) // merah
            else -> holder.tvKondisi.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int = dataList.size

    // ===============================
    // HELPER FORMAT 1 DESIMAL
    // ===============================
    private fun formatOneDecimal(value: String): String {
        if (value.equals("ERROR", true)) return "ERROR"

        return try {
            val cleanValue = value
                .replace(",", ".")
                .replace("%", "")
                .trim()

            val number = cleanValue.toFloat()

            String.format(Locale("id", "ID"), "%.1f", number)
        } catch (e: Exception) {
            "ERROR"
        }
    }
}
