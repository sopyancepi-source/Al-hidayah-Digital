package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    private var activeDatabase: FirebaseDatabase? = null
    private val activeListeners = mutableListOf<Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()

    fun startRealtimeSync(context: Context, url: String, repository: AlHidayahRepository) {
        val finalUrl = getResolvedUrl(url)
        if (finalUrl.isEmpty()) return

        stopRealtimeSync()

        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            activeDatabase = db

            // 1. Sync Santri
            val santriRef = db.getReference("santri")
            val santriListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val downloadedSantris = mutableListOf<Santri>()
                            snapshot.children.forEach { child ->
                                val id = child.child("id").getValue(Int::class.java) ?: 0
                                val name = child.child("name").getValue(String::class.java) ?: ""
                                val pin = child.child("pin").getValue(String::class.java) ?: ""
                                val gender = child.child("gender").getValue(String::class.java) ?: "Perempuan"
                                if (name.isNotEmpty()) {
                                    downloadedSantris.add(Santri(id = id, name = name, pin = pin, gender = gender))
                                }
                            }
                            if (downloadedSantris.isNotEmpty()) {
                                repository.deleteAllSantri()
                                downloadedSantris.forEach { repository.insertSantri(it) }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Realtime sync error (santri): ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            santriRef.addValueEventListener(santriListener)
            activeListeners.add(santriRef to santriListener)

            // 2. Sync Reports
            val reportsRef = db.getReference("reports")
            val reportsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            snapshot.children.forEach { parentChild ->
                                parentChild.children.forEach { child ->
                                    val r = parseReport(child)
                                    if (r != null) {
                                        repository.insertReport(r)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Realtime sync error (reports): ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            reportsRef.addValueEventListener(reportsListener)
            activeListeners.add(reportsRef to reportsListener)

            // 3. Sync Attendance
            val attendanceRef = db.getReference("attendance")
            val attendanceListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val downloadedAttendance = mutableListOf<Attendance>()
                            snapshot.children.forEach { child ->
                                val santriId = child.child("santriId").getValue(Int::class.java) ?: 0
                                val santriName = child.child("santriName").getValue(String::class.java) ?: ""
                                val date = child.child("date").getValue(String::class.java) ?: ""
                                val status = child.child("status").getValue(String::class.java) ?: "HADIR"
                                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                                if (santriId != 0 && date.isNotEmpty()) {
                                    downloadedAttendance.add(Attendance(santriId, santriName, date, status, timestamp))
                                }
                            }
                            if (downloadedAttendance.isNotEmpty()) {
                                repository.saveAllAttendance(downloadedAttendance)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Realtime sync error (attendance): ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            attendanceRef.addValueEventListener(attendanceListener)
            activeListeners.add(attendanceRef to attendanceListener)

            // 4. Sync Evaluation Periods
            val periodsRef = db.getReference("evaluation_periods")
            val periodsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            snapshot.children.forEach { child ->
                                val id = child.child("id").getValue(Int::class.java) ?: 0
                                val name = child.child("name").getValue(String::class.java) ?: ""
                                val startDate = child.child("startDate").getValue(String::class.java) ?: ""
                                val endDate = child.child("endDate").getValue(String::class.java) ?: ""
                                val isActive = child.child("active").getValue(Boolean::class.java) ?: false
                                if (name.isNotEmpty()) {
                                    repository.savePeriod(EvaluationPeriod(id, name, startDate, endDate, isActive))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Realtime sync error (evaluation_periods): ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            periodsRef.addValueEventListener(periodsListener)
            activeListeners.add(periodsRef to periodsListener)

            // 5. Sync Weekly Reports
            val weeklyRef = db.getReference("weekly_reports")
            val weeklyListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            snapshot.children.forEach { child ->
                                val w = parseWeeklyReport(child)
                                if (w != null) {
                                    repository.saveWeeklyReport(w)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Realtime sync error (weekly_reports): ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            weeklyRef.addValueEventListener(weeklyListener)
            activeListeners.add(weeklyRef to weeklyListener)

            // 6. Sync System Pins Realtime
            val pinsRef = db.getReference("system_pins")
            val pinsListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val guruPin = snapshot.child("guruPin").getValue(String::class.java)
                            val admin1Pin = snapshot.child("admin1Pin").getValue(String::class.java)
                            val admin2Pin = snapshot.child("admin2Pin").getValue(String::class.java)
                            
                            if (guruPin != null || admin1Pin != null || admin2Pin != null) {
                                val currentConfig = repository.getConfig()
                                val updatedConfig = currentConfig.copy(
                                    guruPin = guruPin ?: currentConfig.guruPin,
                                    admin1Pin = admin1Pin ?: currentConfig.admin1Pin,
                                    admin2Pin = admin2Pin ?: currentConfig.admin2Pin
                                )
                                repository.updateConfig(updatedConfig)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Realtime sync error (system_pins): ${e.message}")
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            pinsRef.addValueEventListener(pinsListener)
            activeListeners.add(pinsRef to pinsListener)

            Log.d(TAG, "Realtime sync successfully started for url: $finalUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting realtime sync: ${e.message}")
        }
    }

    fun stopRealtimeSync() {
        try {
            activeListeners.forEach { (ref, listener) ->
                ref.removeEventListener(listener)
            }
            activeListeners.clear()
            activeDatabase = null
            Log.d(TAG, "Realtime sync stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping realtime sync: ${e.message}")
        }
    }

    // Silakan isi URL Firebase Anda di bawah ini agar semua HP secara otomatis terhubung
    // tanpa perlu mengetikkan URL secara manual!
    // Contoh: const val DEFAULT_FIREBASE_URL = "https://nama-db-anda.firebaseio.com/"
    const val DEFAULT_FIREBASE_URL = "https://alhidayah-82b02-default-rtdb.asia-southeast1.firebasedatabase.app/" 
    const val DEFAULT_FIREBASE_ENABLED = true

    fun getResolvedUrl(url: String): String {
        return if (url.trim().isEmpty()) DEFAULT_FIREBASE_URL else url.trim()
    }

    fun getResolvedEnabled(url: String, enabled: Boolean): Boolean {
        val finalUrl = getResolvedUrl(url)
        if (finalUrl.isEmpty()) return false
        return if (url.trim().isEmpty()) DEFAULT_FIREBASE_ENABLED else enabled
    }

    fun getDatabaseInstance(context: Context, url: String): FirebaseDatabase? {
        val finalUrl = getResolvedUrl(url)
        if (finalUrl.isEmpty()) return null
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.alhidayah.mhkrzw")
                    .setApiKey("dummy-api-key-so-app-doesnt-crash")
                    .setDatabaseUrl(finalUrl)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            val db = FirebaseDatabase.getInstance(finalUrl)
            try {
                db.setPersistenceEnabled(true)
            } catch (e: Exception) {
                // Ignored if already set
            }
            db
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}")
            null
        }
    }

    // Individual item upload syncs
    fun syncReport(context: Context, url: String, enabled: Boolean, report: Report) {
        val finalUrl = getResolvedUrl(url)
        val finalEnabled = getResolvedEnabled(url, enabled)
        if (!finalEnabled || finalUrl.isEmpty()) return
        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            val ref = db.getReference("reports").child(report.santriId.toString()).child(report.date)
            ref.setValue(report)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync report: ${e.message}")
        }
    }

    fun syncSantri(context: Context, url: String, enabled: Boolean, santri: Santri) {
        val finalUrl = getResolvedUrl(url)
        val finalEnabled = getResolvedEnabled(url, enabled)
        if (!finalEnabled || finalUrl.isEmpty()) return
        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            val ref = db.getReference("santri").child(santri.id.toString())
            ref.setValue(santri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync santri: ${e.message}")
        }
    }

    fun syncAttendance(context: Context, url: String, enabled: Boolean, attendance: Attendance) {
        val finalUrl = getResolvedUrl(url)
        val finalEnabled = getResolvedEnabled(url, enabled)
        if (!finalEnabled || finalUrl.isEmpty()) return
        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            val key = "${attendance.date}_${attendance.santriId}"
            val ref = db.getReference("attendance").child(key)
            ref.setValue(attendance)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync attendance: ${e.message}")
        }
    }

    fun syncPeriod(context: Context, url: String, enabled: Boolean, period: EvaluationPeriod) {
        val finalUrl = getResolvedUrl(url)
        val finalEnabled = getResolvedEnabled(url, enabled)
        if (!finalEnabled || finalUrl.isEmpty()) return
        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            val ref = db.getReference("evaluation_periods").child(period.id.toString())
            ref.setValue(period)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync period: ${e.message}")
        }
    }

    fun syncWeeklyReport(context: Context, url: String, enabled: Boolean, weeklyReport: WeeklyReport) {
        val finalUrl = getResolvedUrl(url)
        val finalEnabled = getResolvedEnabled(url, enabled)
        if (!finalEnabled || finalUrl.isEmpty()) return
        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            val ref = db.getReference("weekly_reports").child(weeklyReport.id.toString())
            ref.setValue(weeklyReport)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync weekly report: ${e.message}")
        }
    }

    fun syncSystemPins(context: Context, url: String, enabled: Boolean, guruPin: String, admin1Pin: String, admin2Pin: String) {
        val finalUrl = getResolvedUrl(url)
        val finalEnabled = getResolvedEnabled(url, enabled)
        if (!finalEnabled || finalUrl.isEmpty()) return
        try {
            val db = getDatabaseInstance(context, finalUrl) ?: return
            val ref = db.getReference("system_pins")
            val data = mapOf(
                "guruPin" to guruPin,
                "admin1Pin" to admin1Pin,
                "admin2Pin" to admin2Pin
            )
            ref.setValue(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync system pins: ${e.message}")
        }
    }

    // Full Upload (Send local to Firebase)
    suspend fun fullUpload(
        context: Context,
        url: String,
        repository: AlHidayahRepository,
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val finalUrl = getResolvedUrl(url)
            val db = getDatabaseInstance(context, finalUrl) ?: return@withContext false

            onProgress("Mengunggah data Santri...")
            val santris = repository.allSantri.first()
            for (s in santris) {
                db.getReference("santri").child(s.id.toString()).setValue(s).await()
            }

            onProgress("Mengunggah Laporan Harian...")
            val reports = repository.allReports.first()
            for (r in reports) {
                db.getReference("reports").child(r.santriId.toString()).child(r.date).setValue(r).await()
            }

            onProgress("Mengunggah Absensi...")
            val attendanceList = repository.allAttendance.first()
            for (a in attendanceList) {
                val key = "${a.date}_${a.santriId}"
                db.getReference("attendance").child(key).setValue(a).await()
            }

            onProgress("Mengunggah Periode Evaluasi...")
            val periods = repository.allPeriods.first()
            for (p in periods) {
                db.getReference("evaluation_periods").child(p.id.toString()).setValue(p).await()
            }

            onProgress("Mengunggah Laporan Mingguan...")
            val weeklyReports = repository.allWeeklyReports.first()
            for (w in weeklyReports) {
                db.getReference("weekly_reports").child(w.id.toString()).setValue(w).await()
            }

            onProgress("Mengunggah PIN Sistem...")
            val currentConfig = repository.getConfig()
            val pinsData = mapOf(
                "guruPin" to currentConfig.guruPin,
                "admin1Pin" to currentConfig.admin1Pin,
                "admin2Pin" to currentConfig.admin2Pin
            )
            db.getReference("system_pins").setValue(pinsData).await()

            onProgress("Unggah selesai!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in fullUpload: ${e.message}")
            false
        }
    }

    // Full Download (Pull from Firebase to local)
    suspend fun fullDownload(
        context: Context,
        url: String,
        repository: AlHidayahRepository,
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getDatabaseInstance(context, url) ?: return@withContext false

            onProgress("Mengunduh data Santri...")
            val santriSnapshot = getValueSingle(db.getReference("santri"))
            val downloadedSantris = mutableListOf<Santri>()
            santriSnapshot.children.forEach { child ->
                val id = child.child("id").getValue(Int::class.java) ?: 0
                val name = child.child("name").getValue(String::class.java) ?: ""
                val pin = child.child("pin").getValue(String::class.java) ?: ""
                val gender = child.child("gender").getValue(String::class.java) ?: "Perempuan"
                if (name.isNotEmpty()) {
                    downloadedSantris.add(Santri(id = id, name = name, pin = pin, gender = gender))
                }
            }
            if (downloadedSantris.isNotEmpty()) {
                repository.deleteAllSantri()
                downloadedSantris.forEach { repository.insertSantri(it) }
            }

            onProgress("Mengunduh Laporan Harian...")
            val reportsSnapshot = getValueSingle(db.getReference("reports"))
            reportsSnapshot.children.forEach { parentChild ->
                parentChild.children.forEach { child ->
                    val r = parseReport(child)
                    if (r != null) {
                        repository.insertReport(r)
                    }
                }
            }

            onProgress("Mengunduh Absensi...")
            val attendanceSnapshot = getValueSingle(db.getReference("attendance"))
            val downloadedAttendance = mutableListOf<Attendance>()
            attendanceSnapshot.children.forEach { child ->
                val santriId = child.child("santriId").getValue(Int::class.java) ?: 0
                val santriName = child.child("santriName").getValue(String::class.java) ?: ""
                val date = child.child("date").getValue(String::class.java) ?: ""
                val status = child.child("status").getValue(String::class.java) ?: "HADIR"
                val timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                if (santriId != 0 && date.isNotEmpty()) {
                    downloadedAttendance.add(Attendance(santriId, santriName, date, status, timestamp))
                }
            }
            if (downloadedAttendance.isNotEmpty()) {
                repository.saveAllAttendance(downloadedAttendance)
            }

            onProgress("Mengunduh Periode Evaluasi...")
            val periodsSnapshot = getValueSingle(db.getReference("evaluation_periods"))
            periodsSnapshot.children.forEach { child ->
                val id = child.child("id").getValue(Int::class.java) ?: 0
                val name = child.child("name").getValue(String::class.java) ?: ""
                val startDate = child.child("startDate").getValue(String::class.java) ?: ""
                val endDate = child.child("endDate").getValue(String::class.java) ?: ""
                val isActive = child.child("active").getValue(Boolean::class.java) ?: false
                if (name.isNotEmpty()) {
                    repository.savePeriod(EvaluationPeriod(id, name, startDate, endDate, isActive))
                }
            }

            onProgress("Mengunduh Laporan Mingguan...")
            val weeklySnapshot = getValueSingle(db.getReference("weekly_reports"))
            weeklySnapshot.children.forEach { child ->
                val w = parseWeeklyReport(child)
                if (w != null) {
                    repository.saveWeeklyReport(w)
                }
            }

            onProgress("Mengunduh PIN Sistem...")
            try {
                val pinsSnapshot = getValueSingle(db.getReference("system_pins"))
                val guruPin = pinsSnapshot.child("guruPin").getValue(String::class.java)
                val admin1Pin = pinsSnapshot.child("admin1Pin").getValue(String::class.java)
                val admin2Pin = pinsSnapshot.child("admin2Pin").getValue(String::class.java)
                
                if (guruPin != null || admin1Pin != null || admin2Pin != null) {
                    val currentConfig = repository.getConfig()
                    val updatedConfig = currentConfig.copy(
                        guruPin = guruPin ?: currentConfig.guruPin,
                        admin1Pin = admin1Pin ?: currentConfig.admin1Pin,
                        admin2Pin = admin2Pin ?: currentConfig.admin2Pin
                    )
                    repository.updateConfig(updatedConfig)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download system_pins in fullDownload: ${e.message}")
            }

            onProgress("Unduh selesai!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in fullDownload: ${e.message}")
            false
        }
    }

    private suspend fun getValueSingle(ref: com.google.firebase.database.DatabaseReference): DataSnapshot =
        suspendCancellableCoroutine { continuation ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            }
            ref.addListenerForSingleValueEvent(listener)
            continuation.invokeOnCancellation {
                ref.removeEventListener(listener)
            }
        }

    private fun parseReport(snap: DataSnapshot): Report? {
        return try {
            val id = snap.child("id").getValue(Int::class.java) ?: 0
            val santriId = snap.child("santriId").getValue(Int::class.java) ?: 0
            val santriName = snap.child("santriName").getValue(String::class.java) ?: ""
            val date = snap.child("date").getValue(String::class.java) ?: ""
            if (santriId == 0 || date.isEmpty()) return null

            Report(
                id = id,
                santriId = santriId,
                santriName = santriName,
                date = date,
                timestamp = snap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                parentName = snap.child("parentName").getValue(String::class.java) ?: "",
                parentSignature = snap.child("parentSignature").getValue(String::class.java),
                isHaid = snap.child("haid").getValue(Boolean::class.java) ?: snap.child("isHaid").getValue(Boolean::class.java) ?: false,
                subuhWaktu = snap.child("subuhWaktu").getValue(String::class.java),
                subuhFoto = snap.child("subuhFoto").getValue(String::class.java),
                subuhCara = snap.child("subuhCara").getValue(String::class.java),
                zuhurWaktu = snap.child("zuhurWaktu").getValue(String::class.java),
                zuhurFoto = snap.child("zuhurFoto").getValue(String::class.java),
                zuhurCara = snap.child("zuhurCara").getValue(String::class.java),
                asharWaktu = snap.child("asharWaktu").getValue(String::class.java),
                asharFoto = snap.child("asharFoto").getValue(String::class.java),
                asharCara = snap.child("asharCara").getValue(String::class.java),
                maghribWaktu = snap.child("maghribWaktu").getValue(String::class.java),
                maghribFoto = snap.child("maghribFoto").getValue(String::class.java),
                maghribCara = snap.child("maghribCara").getValue(String::class.java),
                isyaWaktu = snap.child("isyaWaktu").getValue(String::class.java),
                isyaFoto = snap.child("isyaFoto").getValue(String::class.java),
                isyaCara = snap.child("isyaCara").getValue(String::class.java),
                tahajudWaktu = snap.child("tahajudWaktu").getValue(String::class.java),
                tahajudFoto = snap.child("tahajudFoto").getValue(String::class.java),
                tahajudCara = snap.child("tahajudCara").getValue(String::class.java),
                witirWaktu = snap.child("witirWaktu").getValue(String::class.java),
                witirFoto = snap.child("witirFoto").getValue(String::class.java),
                witirCara = snap.child("witirCara").getValue(String::class.java),
                zikirBacaan = snap.child("zikirBacaan").getValue(String::class.java),
                zikirJumlah = snap.child("zikirJumlah").getValue(Int::class.java) ?: 0,
                quranSurat = snap.child("quranSurat").getValue(String::class.java),
                quranAyat = snap.child("quranAyat").getValue(String::class.java),
                iqroJilid = snap.child("iqroJilid").getValue(String::class.java),
                iqroHalaman = snap.child("iqroHalaman").getValue(String::class.java),
                baktiJenis = snap.child("baktiJenis").getValue(String::class.java),
                baktiFoto = snap.child("baktiFoto").getValue(String::class.java)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWeeklyReport(snap: DataSnapshot): WeeklyReport? {
        return try {
            val id = snap.child("id").getValue(Int::class.java) ?: 0
            val periodId = snap.child("periodId").getValue(Int::class.java) ?: 0
            val santriId = snap.child("santriId").getValue(Int::class.java) ?: 0
            if (periodId == 0 || santriId == 0) return null

            WeeklyReport(
                id = id,
                periodId = periodId,
                santriId = santriId,
                periodName = snap.child("periodName").getValue(String::class.java) ?: "",
                startDate = snap.child("startDate").getValue(String::class.java) ?: "",
                endDate = snap.child("endDate").getValue(String::class.java) ?: "",
                autoNotes = snap.child("autoNotes").getValue(String::class.java) ?: "",
                ustazNotes = snap.child("ustazNotes").getValue(String::class.java) ?: "",
                adminNotes = snap.child("adminNotes").getValue(String::class.java) ?: "",
                parentNotes = snap.child("parentNotes").getValue(String::class.java) ?: "",
                parentSignature = snap.child("parentSignature").getValue(String::class.java),
                parentSignedName = snap.child("parentSignedName").getValue(String::class.java) ?: "",
                isSigned = snap.child("signed").getValue(Boolean::class.java) ?: snap.child("isSigned").getValue(Boolean::class.java) ?: false,
                isSentToParent = snap.child("sentToParent").getValue(Boolean::class.java) ?: snap.child("isSentToParent").getValue(Boolean::class.java) ?: false,
                signedTimestamp = snap.child("signedTimestamp").getValue(Long::class.java) ?: 0L
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearFirebaseReportsAndAttendance(
        context: Context,
        url: String,
        onProgress: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val finalUrl = getResolvedUrl(url)
            val db = getDatabaseInstance(context, finalUrl) ?: return@withContext false
            
            onProgress("Menghapus data Laporan Harian di Firebase...")
            db.getReference("reports").setValue(null).await()
            
            onProgress("Menghapus data Absensi di Firebase...")
            db.getReference("attendance").setValue(null).await()
            
            onProgress("Menghapus data Laporan Mingguan di Firebase...")
            db.getReference("weekly_reports").setValue(null).await()
            
            onProgress("Penghapusan data di Firebase berhasil!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in clearFirebaseReportsAndAttendance: ${e.message}")
            false
        }
    }
}
