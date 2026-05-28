package com.example.latihan5_skripsi

import com.google.firebase.database.*
import com.example.latihan5_skripsi.model.DataTabel
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

object DataCenter {

    // ======================================
    // 🔹 REALTIME VALUE (NUMERIC UNTUK GRAFIK)
    // ======================================
    var suhu: Double? = null
    var kelembapan: Double? = null
    var kadarAirA: Double? = null
    var kadarAirB: Double? = null
    var kadarAirC: Double? = null

    // ======================================
    // 🔹 REALTIME RAW (STRING UNTUK UI)
    // ======================================
    var suhuStr: String = "-"
    var kelembapanStr: String = "-"
    var kadarAirAStr: String = "-"
    var kadarAirBStr: String = "-"
    var kadarAirCStr: String = "-"

    var kondisi: String = ""
    var waktu: String = ""

    var onRealtimeUpdate: (() -> Unit)? = null

    // ======================================
    // 🔥 FIREBASE
    // ======================================
    private val db = FirebaseDatabase.getInstance()
    private val dbRealtime = db.getReference("REALTIME")
    private val dbRiwayat = db.getReference("RIWAYAT")

    private var firebaseActive = false
    private var lastSavedTime = ""
    private var lastSavedData = ""
    private var firstRealtime = true

    // ======================================
    // 🔧 SAFE READ
    // ======================================
    private fun readDoubleNullable(snapshot: DataSnapshot, key: String): Double? {
        val raw = snapshot.child(key).value
        return if (raw is Number) raw.toDouble() else null
    }

    private fun readString(snapshot: DataSnapshot, key: String): String {
        return snapshot.child(key).value?.toString() ?: "ERROR"
    }

    // ======================================
    // 🔥 REALTIME
    // ======================================
    fun startFirebaseRealtime() {
        if (firebaseActive) return

        dbRealtime.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                // === RAW STRING (UNTUK UI)
                suhuStr = readString(snapshot, "suhu")
                kelembapanStr = readString(snapshot, "kelembapan")
                kadarAirAStr = readString(snapshot, "kadarAir1")
                kadarAirBStr = readString(snapshot, "kadarAir2")
                kadarAirCStr = readString(snapshot, "kadarAir3")

                // === NUMERIC (UNTUK GRAFIK)
                suhu = readDoubleNullable(snapshot, "suhu")
                kelembapan = readDoubleNullable(snapshot, "kelembapan")
                kadarAirA = readDoubleNullable(snapshot, "kadarAir1")
                kadarAirB = readDoubleNullable(snapshot, "kadarAir2")
                kadarAirC = readDoubleNullable(snapshot, "kadarAir3")

                kondisi = snapshot.child("kondisiGudang")
                    .getValue(String::class.java) ?: kondisi

                waktu = snapshot.child("waktu")
                    .getValue(String::class.java) ?: waktu

                onRealtimeUpdate?.invoke()

                if (firstRealtime) {
                    firstRealtime = false
                    return
                }

                simpanRiwayat()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        firebaseActive = true
    }

    // ======================================
    // 🔥 SIMPAN RIWAYAT
    // ======================================
    fun simpanRiwayat() {

        val tanggal = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID")).format(Date())
        val jam = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date())

        if (jam == lastSavedTime) return
        lastSavedTime = jam

        val dataString =
            "$suhuStr|$kelembapanStr|$kadarAirAStr|$kadarAirBStr|$kadarAirCStr|$kondisi"

        if (dataString == lastSavedData) return
        lastSavedData = dataString

        val data = mapOf(
            "suhu" to suhuStr,
            "kelembapan" to kelembapanStr,
            "kadarAir1" to kadarAirAStr,
            "kadarAir2" to kadarAirBStr,
            "kadarAir3" to kadarAirCStr,
            "kondisiGudang" to kondisi
        )

        dbRiwayat.child(tanggal).child(jam).setValue(data)
    }

    // ======================================
    // 🔥 RIWAYAT (API TETAP)
    // ======================================
    fun getRiwayatByTanggal(
        tanggal: String,
        callback: (List<DataTabel>) -> Unit
    ) {
        val ref = dbRiwayat.child(tanggal)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val list = mutableListOf<DataTabel>()

                for (jamNode in snapshot.children) {
                    val jam = jamNode.key ?: continue

                    fun readAny(key: String): String =
                        jamNode.child(key).value?.toString() ?: "-"

                    list.add(
                        DataTabel(
                            waktu = jam,
                            suhu = readAny("suhu"),
                            kelembapan = readAny("kelembapan"),
                            kadarAirA = readAny("kadarAir1"),
                            kadarAirB = readAny("kadarAir2"),
                            kadarAirC = readAny("kadarAir3"),
                            kondisi = readAny("kondisiGudang")
                        )
                    )
                }

                list.sortByDescending { it.waktu }
                callback(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ======================================
    // 🔥 LIST TANGGAL (API TIDAK BOLEH HILANG)
    // ======================================
    fun getListTanggal(callback: (List<String>) -> Unit) {
        dbRiwayat.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))

                val list = snapshot.children.mapNotNull { it.key }

                val sortedList = list.sortedByDescending { tanggal ->
                    try {
                        sdf.parse(tanggal)?.time ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }

                callback(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun getLatestTanggal(callback: (String?) -> Unit) {
        dbRiwayat.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale("id", "ID"))

                val latest = snapshot.children.mapNotNull { it.key }
                    .maxByOrNull { tanggal ->
                        try {
                            sdf.parse(tanggal)?.time ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                    }

                callback(latest)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ======================================
    // 🔥 DUMMY DATA (TIDAK DIUBAH)
    // ======================================
    private var sudahGenerate = false

    fun generateDummyData() {
        if (sudahGenerate) return

        suhu = Random.nextDouble(25.0, 36.0)
        kelembapan = Random.nextDouble(45.0, 86.0)
        kadarAirA = Random.nextDouble(10.0, 25.0)
        kadarAirB = Random.nextDouble(10.0, 25.0)
        kadarAirC = Random.nextDouble(10.0, 25.0)

        suhuStr = suhu.toString()
        kelembapanStr = kelembapan.toString()
        kadarAirAStr = kadarAirA.toString()
        kadarAirBStr = kadarAirB.toString()
        kadarAirCStr = kadarAirC.toString()

        kondisi = "-"
        waktu = "-"

        sudahGenerate = true
    }
}
