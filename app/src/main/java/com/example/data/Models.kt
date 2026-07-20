package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "santri")
data class Santri(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pin: String,
    val gender: String = "Perempuan"
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val guruPin: String = "1234",
    val admin1Pin: String = "1111",
    val admin2Pin: String = "2222",
    val firebaseUrl: String = "https://alhidayah-82b02-default-rtdb.asia-southeast1.firebasedatabase.app/",
    val firebaseEnabled: Boolean = true
)

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val santriId: Int,
    val santriName: String,
    val date: String, // "yyyy-MM-dd"
    val timestamp: Long = System.currentTimeMillis(),
    val parentName: String = "",
    val parentSignature: String? = null, // Base64 encoded tiny compressed image
    val isHaid: Boolean = false, // True if the female santri is menstruating

    // Shalat Fardhu
    val subuhWaktu: String? = null,
    val subuhFoto: String? = null, // Base64 encoded compressed photo
    val subuhCara: String? = null, // "Munfarid" / "Berjamaah"

    val zuhurWaktu: String? = null,
    val zuhurFoto: String? = null,
    val zuhurCara: String? = null,

    val asharWaktu: String? = null,
    val asharFoto: String? = null,
    val asharCara: String? = null,

    val maghribWaktu: String? = null,
    val maghribFoto: String? = null,
    val maghribCara: String? = null,

    val isyaWaktu: String? = null,
    val isyaFoto: String? = null,
    val isyaCara: String? = null,

    // Ibadah Sunnah
    val tahajudWaktu: String? = null,
    val tahajudFoto: String? = null,
    val tahajudCara: String? = null,

    val witirWaktu: String? = null,
    val witirFoto: String? = null,
    val witirCara: String? = null,

    val zikirBacaan: String? = null,
    val zikirJumlah: Int = 0,

    val quranSurat: String? = null,
    val quranAyat: String? = null,

    val iqroJilid: String? = null,
    val iqroHalaman: String? = null,

    val baktiJenis: String? = null,
    val baktiFoto: String? = null
)

@Entity(tableName = "attendance", primaryKeys = ["santriId", "date"])
data class Attendance(
    val santriId: Int,
    val santriName: String,
    val date: String, // "yyyy-MM-dd"
    val status: String = "HADIR", // "HADIR", "SAKIT", "IZIN", "ALFA"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "evaluation_periods")
data class EvaluationPeriod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // e.g. "Minggu Pertama Juli"
    val startDate: String, // "yyyy-MM-dd"
    val endDate: String,   // "yyyy-MM-dd"
    val isActive: Boolean = true
)

@Entity(tableName = "weekly_reports")
data class WeeklyReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val periodId: Int,
    val santriId: Int,
    val periodName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val autoNotes: String = "",
    val ustazNotes: String = "",
    val adminNotes: String = "",
    val parentNotes: String = "",
    val parentSignature: String? = null,
    val parentSignedName: String = "",
    val isSigned: Boolean = false,
    val isSentToParent: Boolean = false,
    val signedTimestamp: Long = 0L
)
