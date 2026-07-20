package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfig
import com.example.data.AppDatabase
import com.example.data.AlHidayahRepository
import com.example.data.Report
import com.example.data.Santri
import com.example.data.Attendance
import com.example.data.EvaluationPeriod
import com.example.data.WeeklyReport
import com.example.data.FirebaseSyncManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class Role {
    NONE, PARENT, GURU, ADMIN1, ADMIN2
}

enum class WorshipType {
    SUBUH, ZUHUR, ASHAR, MAGHRIB, ISYA,
    TAHAJUD, WITIR, ZIKIR, QURAN, BAKTI,
    VERIFIKASI
}

class AlHidayahViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlHidayahRepository
    
    // States
    private val _currentRole = MutableStateFlow(Role.NONE)
    val currentRole: StateFlow<Role> = _currentRole.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _activeSantri = MutableStateFlow<Santri?>(null)
    val activeSantri: StateFlow<Santri?> = _activeSantri.asStateFlow()

    private val _activeForm = MutableStateFlow<WorshipType?>(null)
    val activeForm: StateFlow<WorshipType?> = _activeForm.asStateFlow()

    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _parentReport = MutableStateFlow<Report?>(null)
    val parentReport: StateFlow<Report?> = _parentReport.asStateFlow()

    // Date Tracker
    private val _selectedDateString = MutableStateFlow("")
    val selectedDateString: StateFlow<String> = _selectedDateString.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AlHidayahRepository(
            santriDao = database.santriDao(),
            reportDao = database.reportDao(),
            appConfigDao = database.appConfigDao(),
            attendanceDao = database.attendanceDao(),
            evaluationPeriodDao = database.evaluationPeriodDao(),
            weeklyReportDao = database.weeklyReportDao()
        )
        
        // Initialize date to today
        _selectedDateString.value = getTodayDateString()
        
        // Observe config changes in real-time from database (including those updated via FirebaseSyncManager)
        viewModelScope.launch {
            repository.appConfigFlow.collect { config ->
                if (config != null) {
                    _appConfig.value = config
                }
            }
        }
        
        // Load configuration on startup and run background auto-sync & realtime listener
        viewModelScope.launch {
            val config = repository.getConfig()
            _appConfig.value = config
            if (config.firebaseUrl.isNotEmpty() && config.firebaseEnabled) {
                // Mulai real-time listener agar perubahan di Firebase langsung tersinkron ke HP lain secara realtime
                FirebaseSyncManager.startRealtimeSync(application, config.firebaseUrl, repository)
                
                _isSyncing.value = true
                _syncProgress.value = "Sinkronisasi otomatis..."
                try {
                    FirebaseSyncManager.fullDownload(
                        application,
                        config.firebaseUrl,
                        repository
                    ) { progress ->
                        _syncProgress.value = progress
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AlHidayahViewModel", "Auto sync failed: ${e.message}")
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    // Reactive streams from Room
    val santriList: StateFlow<List<Santri>> = repository.allSantri
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val attendanceList: StateFlow<List<Attendance>> = _selectedDateString
        .flatMapLatest { date ->
            if (date.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getAttendanceForDate(date)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allAttendanceList: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveAttendance(santriId: Int, name: String, dateStr: String, status: String) {
        viewModelScope.launch {
            val attendance = Attendance(
                santriId = santriId,
                santriName = name,
                date = dateStr,
                status = status
            )
            repository.saveAttendance(attendance)
            val config = _appConfig.value
            FirebaseSyncManager.syncAttendance(getApplication(), config.firebaseUrl, config.firebaseEnabled, attendance)
        }
    }

    fun saveAllAttendance(list: List<Attendance>) {
        viewModelScope.launch {
            repository.saveAllAttendance(list)
            val config = _appConfig.value
            list.forEach {
                FirebaseSyncManager.syncAttendance(getApplication(), config.firebaseUrl, config.firebaseEnabled, it)
            }
        }
    }

    val reportList: StateFlow<List<Report>> = repository.allReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setDate(dateStr: String) {
        _selectedDateString.value = dateStr
        val active = _activeSantri.value
        if (active != null) {
            loadOrCreateReport(active.id, active.name, dateStr)
        }
    }

    fun selectRole(role: Role) {
        _currentRole.value = role
        _isAuthenticated.value = false
        _activeSantri.value = null
        _activeForm.value = null
    }

    fun logout() {
        _currentRole.value = Role.NONE
        _isAuthenticated.value = false
        _activeSantri.value = null
        _activeForm.value = null
    }

    fun loginWithPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val config = repository.getConfig()
            _appConfig.value = config
            
            val success = when (_currentRole.value) {
                Role.GURU -> pin == config.guruPin
                Role.ADMIN1 -> pin == config.admin1Pin
                Role.ADMIN2 -> pin == config.admin2Pin
                Role.PARENT -> {
                    val santri = repository.getSantriByPin(pin)
                    if (santri != null) {
                        _activeSantri.value = santri
                        loadOrCreateReport(santri.id, santri.name, _selectedDateString.value)
                        true
                    } else {
                        false
                    }
                }
                Role.NONE -> false
            }
            
            _isAuthenticated.value = success
            onResult(success)
        }
    }

    private fun loadOrCreateReport(santriId: Int, santriName: String, dateStr: String) {
        viewModelScope.launch {
            val report = repository.getReportForSantriAndDate(santriId, dateStr)
            if (report != null) {
                _parentReport.value = report
            } else {
                val newReport = Report(
                    santriId = santriId,
                    santriName = santriName,
                    date = dateStr
                )
                val id = repository.insertReport(newReport)
                val finalReport = newReport.copy(id = id.toInt())
                _parentReport.value = finalReport
                
                // Sync to Firebase
                val config = _appConfig.value
                FirebaseSyncManager.syncReport(getApplication(), config.firebaseUrl, config.firebaseEnabled, finalReport)
            }
        }
    }

    fun openForm(type: WorshipType) {
        _activeForm.value = type
    }

    fun closeForm() {
        _activeForm.value = null
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        return sdf.format(Date())
    }

    // Save individual activities inside today's report
    fun saveSubuh(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(subuhWaktu = waktu, subuhFoto = foto, subuhCara = cara) }
    }

    fun saveZuhur(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(zuhurWaktu = waktu, zuhurFoto = foto, zuhurCara = cara) }
    }

    fun saveAshar(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(asharWaktu = waktu, asharFoto = foto, asharCara = cara) }
    }

    fun saveMaghrib(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(maghribWaktu = waktu, maghribFoto = foto, maghribCara = cara) }
    }

    fun saveIsya(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(isyaWaktu = waktu, isyaFoto = foto, isyaCara = cara) }
    }

    fun saveTahajud(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(tahajudWaktu = waktu, tahajudFoto = foto, tahajudCara = cara) }
    }

    fun saveWitir(waktu: String, foto: String?, cara: String) {
        updateReport { it.copy(witirWaktu = waktu, witirFoto = foto, witirCara = cara) }
    }

    fun saveZikir(bacaan: String, jumlah: Int) {
        updateReport { it.copy(zikirBacaan = bacaan, zikirJumlah = jumlah) }
    }

    fun saveQuran(surat: String, ayat: String) {
        updateReport { it.copy(quranSurat = surat, quranAyat = ayat, iqroJilid = null, iqroHalaman = null) }
    }

    fun saveIqro(jilid: String, halaman: String) {
        updateReport { it.copy(iqroJilid = jilid, iqroHalaman = halaman, quranSurat = null, quranAyat = null) }
    }

    fun saveBakti(jenis: String, foto: String?) {
        updateReport { it.copy(baktiJenis = jenis, baktiFoto = foto) }
    }

    fun saveVerifikasi(namaOrtu: String, signatureBase64: String) {
        updateReport { it.copy(parentName = namaOrtu, parentSignature = signatureBase64) }
    }

    fun saveHaidStatus(isHaid: Boolean) {
        updateReport { it.copy(isHaid = isHaid) }
    }

    private fun updateReport(transform: (Report) -> Report) {
        val current = _parentReport.value ?: return
        val updated = transform(current)
        _parentReport.value = updated
        viewModelScope.launch {
            repository.updateReport(updated)
            _activeForm.value = null // Close the form and return to original menu
            
            // Sync to Firebase
            val config = _appConfig.value
            FirebaseSyncManager.syncReport(getApplication(), config.firebaseUrl, config.firebaseEnabled, updated)
        }
    }

    // Teacher & Config Controls (Guru Mode)
    fun updateSystemPins(newGuruPin: String, newAdmin1Pin: String, newAdmin2Pin: String) {
        viewModelScope.launch {
            val updatedConfig = _appConfig.value.copy(
                guruPin = newGuruPin,
                admin1Pin = newAdmin1Pin,
                admin2Pin = newAdmin2Pin
            )
            repository.updateConfig(updatedConfig)
            _appConfig.value = updatedConfig
            
            // Sync updated PINs to Firebase so other devices receive them instantly
            FirebaseSyncManager.syncSystemPins(
                getApplication(),
                updatedConfig.firebaseUrl,
                updatedConfig.firebaseEnabled,
                newGuruPin,
                newAdmin1Pin,
                newAdmin2Pin
            )
        }
    }

    fun updateFirebaseConfig(url: String, enabled: Boolean) {
        viewModelScope.launch {
            val updatedConfig = _appConfig.value.copy(
                firebaseUrl = url,
                firebaseEnabled = enabled
            )
            repository.updateConfig(updatedConfig)
            _appConfig.value = updatedConfig
            if (enabled && url.isNotEmpty()) {
                FirebaseSyncManager.startRealtimeSync(getApplication(), url, repository)
            } else {
                FirebaseSyncManager.stopRealtimeSync()
            }
        }
    }

    private val _syncProgress = MutableStateFlow("")
    val syncProgress: StateFlow<String> = _syncProgress.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun triggerFullUpload(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = "Menghubungkan ke Firebase..."
            val success = FirebaseSyncManager.fullUpload(
                getApplication(),
                _appConfig.value.firebaseUrl,
                repository
            ) { progress ->
                _syncProgress.value = progress
            }
            _isSyncing.value = false
            onResult(success)
        }
    }

    fun triggerFullDownload(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = "Menghubungkan ke Firebase..."
            val success = FirebaseSyncManager.fullDownload(
                getApplication(),
                _appConfig.value.firebaseUrl,
                repository
            ) { progress ->
                _syncProgress.value = progress
            }
            // Refresh parent report if any active santri is set
            val active = _activeSantri.value
            if (active != null) {
                loadOrCreateReport(active.id, active.name, _selectedDateString.value)
            }
            _isSyncing.value = false
            onResult(success)
        }
    }

    fun triggerClearWeeklyAndDailyData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = "Memulai pembersihan data..."
            
            var firebaseSuccess = true
            val url = _appConfig.value.firebaseUrl
            if (url.trim().isNotEmpty()) {
                _syncProgress.value = "Menghapus data laporan di Firebase..."
                firebaseSuccess = FirebaseSyncManager.clearFirebaseReportsAndAttendance(
                    getApplication(),
                    url
                ) { progress ->
                    _syncProgress.value = progress
                }
            }
            
            _syncProgress.value = "Menghapus data laporan di penyimpanan HP..."
            repository.clearAllLocalReportsAndAttendance()
            
            _isSyncing.value = false
            onResult(firebaseSuccess)
        }
    }

    fun connectAndSyncFirebase(url: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = "Menghubungkan ke database..."
            
            val updatedConfig = _appConfig.value.copy(
                firebaseUrl = url,
                firebaseEnabled = url.trim().isNotEmpty()
            )
            repository.updateConfig(updatedConfig)
            _appConfig.value = updatedConfig
            
            if (url.trim().isNotEmpty()) {
                // Aktifkan realtime listener
                FirebaseSyncManager.startRealtimeSync(getApplication(), url, repository)
                
                _syncProgress.value = "Mengunduh data dari Firebase..."
                val success = FirebaseSyncManager.fullDownload(
                    getApplication(),
                    url,
                    repository
                ) { progress ->
                    _syncProgress.value = progress
                }
                
                // Refresh parent report if any active santri is set
                val active = _activeSantri.value
                if (active != null) {
                    loadOrCreateReport(active.id, active.name, _selectedDateString.value)
                }
                _isSyncing.value = false
                onResult(success)
            } else {
                FirebaseSyncManager.stopRealtimeSync()
                _isSyncing.value = false
                onResult(true)
            }
        }
    }

    fun addNewSantri(name: String, pin: String, gender: String) {
        viewModelScope.launch {
            val santri = Santri(name = name, pin = pin, gender = gender)
            val id = repository.insertSantri(santri)
            val config = _appConfig.value
            FirebaseSyncManager.syncSantri(getApplication(), config.firebaseUrl, config.firebaseEnabled, santri.copy(id = id.toInt()))
        }
    }

    fun updateSantri(santriId: Int, name: String, pin: String, gender: String) {
        viewModelScope.launch {
            val santri = Santri(id = santriId, name = name, pin = pin, gender = gender)
            repository.updateSantri(santri)
            val config = _appConfig.value
            FirebaseSyncManager.syncSantri(getApplication(), config.firebaseUrl, config.firebaseEnabled, santri)
        }
    }

    fun deleteSantri(santri: Santri) {
        viewModelScope.launch {
            repository.deleteSantri(santri)
        }
    }

    // Period & Weekly Report StateFlows
    val periodsList: StateFlow<List<EvaluationPeriod>> = repository.allPeriods
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activePeriod: StateFlow<EvaluationPeriod?> = repository.allPeriods
        .map { list -> list.find { it.isActive } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val weeklyReportsList: StateFlow<List<WeeklyReport>> = repository.allWeeklyReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createOrUpdatePeriod(id: Int = 0, name: String, startDate: String, endDate: String, isActive: Boolean) {
        viewModelScope.launch {
            val period = EvaluationPeriod(
                id = id,
                name = name,
                startDate = startDate,
                endDate = endDate,
                isActive = isActive
            )
            val generatedId = repository.savePeriod(period)
            val finalPeriod = if (id == 0) period.copy(id = generatedId.toInt()) else period
            val config = _appConfig.value
            FirebaseSyncManager.syncPeriod(getApplication(), config.firebaseUrl, config.firebaseEnabled, finalPeriod)
        }
    }

    fun activatePeriod(id: Int) {
        viewModelScope.launch {
            repository.activatePeriod(id)
            // Sync all periods to update isActive statuses
            val config = _appConfig.value
            if (config.firebaseEnabled && config.firebaseUrl.isNotEmpty()) {
                try {
                    repository.allPeriods.firstOrNull()?.forEach {
                        FirebaseSyncManager.syncPeriod(getApplication(), config.firebaseUrl, config.firebaseEnabled, it)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun deletePeriod(period: EvaluationPeriod) {
        viewModelScope.launch {
            repository.deletePeriod(period)
        }
    }

    fun saveWeeklyReport(wr: WeeklyReport) {
        viewModelScope.launch {
            val generatedId = repository.saveWeeklyReport(wr)
            val finalWr = if (wr.id == 0) wr.copy(id = generatedId.toInt()) else wr
            val config = _appConfig.value
            FirebaseSyncManager.syncWeeklyReport(getApplication(), config.firebaseUrl, config.firebaseEnabled, finalWr)
        }
    }

    fun updateWeeklyReport(wr: WeeklyReport) {
        viewModelScope.launch {
            repository.updateWeeklyReport(wr)
            val config = _appConfig.value
            FirebaseSyncManager.syncWeeklyReport(getApplication(), config.firebaseUrl, config.firebaseEnabled, wr)
        }
    }

    fun generateAutoNotesForSantri(santriId: Int, startDate: String, endDate: String): String {
        val filteredReports = reportList.value.filter {
            it.santriId == santriId && it.date >= startDate && it.date <= endDate
        }
        val filteredAttendance = allAttendanceList.value.filter {
            it.santriId == santriId && it.date >= startDate && it.date <= endDate
        }
        
        val totalDays = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = sdf.parse(startDate)
            val end = sdf.parse(endDate)
            val diff = end.time - start.time
            val days = (diff / (24 * 60 * 60 * 1000)).toInt() + 1
            if (days > 0) days else 7
        } catch (e: Exception) {
            7
        }
        
        return generateAutoNotesText(filteredReports, filteredAttendance, totalDays)
    }

    private fun generateAutoNotesText(reports: List<Report>, attendance: List<Attendance>, totalDays: Int): String {
        if (reports.isEmpty() && attendance.isEmpty()) {
            return "Belum ada rekaman laporan ibadah harian maupun absensi untuk periode ini."
        }
        
        var totalHaid = 0
        var activeDays = 0
        
        var completedPrayersCount = 0
        
        var totalZikir = 0
        var totalQuranOrIqro = 0
        var totalBakti = 0
        var totalTahajud = 0
        var totalWitir = 0
        
        for (r in reports) {
            if (r.isHaid) {
                totalHaid++
                continue
            }
            activeDays++
            
            fun processPrayer(waktu: String?) {
                if (waktu != null && waktu != "Tidak Shalat") {
                    completedPrayersCount++
                }
            }
            
            processPrayer(r.subuhWaktu)
            processPrayer(r.zuhurWaktu)
            processPrayer(r.asharWaktu)
            processPrayer(r.maghribWaktu)
            processPrayer(r.isyaWaktu)
            
            // Zikir
            if (r.zikirBacaan != null && r.zikirBacaan != "Tidak Melaksanakan") {
                totalZikir++
            }
            
            // Quran / Iqro
            if (!r.quranSurat.isNullOrEmpty() || !r.iqroJilid.isNullOrEmpty()) {
                totalQuranOrIqro++
            }
            
            // Bakti Ortu
            if (r.baktiJenis != null && r.baktiJenis != "Tidak Melaksanakan") {
                totalBakti++
            }
            
            // Tahajud
            if (r.tahajudWaktu != null && r.tahajudWaktu != "Tidak Shalat") {
                totalTahajud++
            }
            
            // Witir
            if (r.witirWaktu != null && r.witirWaktu != "Tidak Shalat") {
                totalWitir++
            }
        }
        
        val targetDays = maxOf(0, totalDays - totalHaid)
        val targetFardhu = targetDays * 5
        val prayerRatio = if (targetFardhu > 0) completedPrayersCount.toFloat() / targetFardhu else 1f
        
        // Attendance stats
        val totalHadir = attendance.count { it.status == "HADIR" }
        val totalSakit = attendance.count { it.status == "SAKIT" }
        val totalIzin = attendance.count { it.status == "IZIN" }
        val totalAlfa = attendance.count { it.status == "ALFA" }
        
        val sb = java.lang.StringBuilder()
        sb.append("📋 *REKAPITULASI EVALUASI IBADAH & ABSENSI*:\n\n")
        
        // 1. Absensi Recap
        sb.append("📌 *Kehadiran Belajar di Masjid*:\n")
        sb.append("• Hadir: $totalHadir hari\n")
        if (totalSakit > 0) sb.append("• Sakit: $totalSakit hari\n")
        if (totalIzin > 0) sb.append("• Izin: $totalIzin hari\n")
        if (totalAlfa > 0) sb.append("• Alfa: $totalAlfa hari (Tanpa Keterangan)\n")
        sb.append("\n")
        
        // 2. Worship Recap
        sb.append("🕌 *Hasil Rekapitulasi Ibadah Santri*:\n")
        if (targetFardhu > 0) {
            sb.append("• *Shalat Fardhu*: Terlaksana $completedPrayersCount dari $targetFardhu kali fardhu (Keberhasilan: ${(prayerRatio * 100).toInt()}%)\n")
        } else {
            sb.append("• *Shalat Fardhu*: Halangan (Haid) penuh pada periode ini.\n")
        }
        
        sb.append("• *Shalat Tahajud*: Melaksanakan $totalTahajud dari $targetDays malam\n")
        sb.append("• *Shalat Witir*: Melaksanakan $totalWitir dari $targetDays malam\n")
        sb.append("• *Zikir Harian*: Melaksanakan $totalZikir dari $targetDays hari\n")
        sb.append("• *Mengaji Al-Qur'an/Iqro*: Aktif mengaji $totalQuranOrIqro dari $targetDays hari\n")
        sb.append("• *Bakti Orang Tua*: Membantu pekerjaan rumah $totalBakti dari $targetDays hari\n")
        if (totalHaid > 0) sb.append("• *Status Halangan (Haid)*: $totalHaid hari\n")
        sb.append("\n")
        
        // 3. Polite customized message based on levels
        if (prayerRatio >= 0.85) {
            sb.append("🌟 *Tingkat Kualitas Ibadah: Sangat Baik / Istimewa* 🌟\n")
            sb.append("Assalamu'alaikum Warahmatullahi Wabarakatuh,\n")
            sb.append("Ayah/Bunda yang dirahmati Allah, kami sangat bersyukur dan bangga atas kesungguhan ananda dalam menjaga ibadahnya pekan ini. ")
            sb.append("Ananda telah menunjukkan kedisiplinan luar biasa dalam shalat 5 waktu serta ibadah-ibadah sunnah lainnya.\n\n")
            sb.append("Semoga ananda senantiasa istiqomah, menjadi penyejuk hati orang tua, serta tumbuh menjadi generasi shalih/shalihah yang berbakti. ")
            sb.append("Kami mohon kepada Ayah/Bunda untuk terus memberikan apresiasi dan memotivasi ananda agar tetap semangat beribadah di rumah. Jazakumullah Khairan Katsiran.")
        } else if (prayerRatio >= 0.50) {
            sb.append("📈 *Tingkat Kualitas Ibadah: Cukup Baik / Berkembang* 📈\n")
            sb.append("Assalamu'alaikum Warahmatullahi Wabarakatuh,\n")
            sb.append("Ayah/Bunda yang dirahmati Allah, alhamdulillah perkembangan ibadah ananda pekan ini sudah cukup baik. ")
            sb.append("Meskipun demikian, masih terdapat beberapa shalat fardhu yang terlewat atau belum terlaksana dengan konsisten.\n\n")
            sb.append("Kami sangat mengharapkan bantuan Ayah/Bunda di rumah untuk terus mendampingi, mengingatkan, dan memberikan motivasi kepada ananda. ")
            sb.append("Mari kita bimbing ananda dengan kelembutan agar di pekan berikutnya ibadah ananda dapat semakin tertib dan sempurna. Terima kasih atas kerja sama luar biasa dari Ayah/Bunda.")
        } else {
            sb.append("🌱 *Tingkat Kualitas Ibadah: Perlu Pendampingan & Pembiasaan* 🌱\n")
            sb.append("Assalamu'alaikum Warahmatullahi Wabarakatuh,\n")
            sb.append("Ayah/Bunda yang dirahmati Allah, kami mengucapkan terima kasih banyak atas kesediaan Ayah/Bunda mendampingi ananda selama pekan ini. ")
            sb.append("Berdasarkan rekapitulasi, ibadah harian ananda saat ini masih memerlukan perhatian lebih serta pembiasaan yang lebih intensif di rumah.\n\n")
            sb.append("Ibadah adalah pondasi utama masa depan anak-anak kita. Oleh karena itu, kami sangat memohon bantuan dan kesabaran Ayah/Bunda untuk merangkul, membimbing, ")
            sb.append("dan melatih ananda dengan penuh kasih sayang agar tidak melewatkan shalat fardhu 5 waktu. Semoga setiap ikhtiar kita dalam membimbing ananda bernilai pahala besar di sisi Allah SWT.")
        }
        
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseSyncManager.stopRealtimeSync()
    }
}
