package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AlHidayahRepository(
    private val santriDao: SantriDao,
    private val reportDao: ReportDao,
    private val appConfigDao: AppConfigDao,
    private val attendanceDao: AttendanceDao,
    private val evaluationPeriodDao: EvaluationPeriodDao,
    private val weeklyReportDao: WeeklyReportDao
) {
    val allSantri: Flow<List<Santri>> = santriDao.getAllSantri()
    val allReports: Flow<List<Report>> = reportDao.getAllReports()
    val appConfigFlow: Flow<AppConfig?> = appConfigDao.getConfigFlow()

    // Evaluation Period APIs
    val allPeriods: Flow<List<EvaluationPeriod>> = evaluationPeriodDao.getAllPeriods()
    
    suspend fun getActivePeriod(): EvaluationPeriod? {
        return evaluationPeriodDao.getActivePeriod()
    }

    suspend fun savePeriod(period: EvaluationPeriod): Long {
        if (period.isActive) {
            evaluationPeriodDao.deactivateAllPeriods()
        }
        return evaluationPeriodDao.insertPeriod(period)
    }

    suspend fun deletePeriod(period: EvaluationPeriod) {
        evaluationPeriodDao.deletePeriod(period)
    }

    suspend fun activatePeriod(periodId: Int) {
        evaluationPeriodDao.deactivateAllPeriods()
        val all = evaluationPeriodDao.getAllPeriods().firstOrNull() ?: emptyList()
        val target = all.find { it.id == periodId }
        if (target != null) {
            evaluationPeriodDao.insertPeriod(target.copy(isActive = true))
        }
    }

    // Weekly Report APIs
    val allWeeklyReports: Flow<List<WeeklyReport>> = weeklyReportDao.getAllWeeklyReports()

    fun getWeeklyReportsForSantri(santriId: Int): Flow<List<WeeklyReport>> {
        return weeklyReportDao.getWeeklyReportsForSantri(santriId)
    }

    suspend fun getWeeklyReportForSantriAndPeriod(santriId: Int, periodId: Int): WeeklyReport? {
        return weeklyReportDao.getWeeklyReportForSantriAndPeriod(santriId, periodId)
    }

    suspend fun saveWeeklyReport(weeklyReport: WeeklyReport): Long {
        return weeklyReportDao.insertWeeklyReport(weeklyReport)
    }

    suspend fun updateWeeklyReport(weeklyReport: WeeklyReport) {
        weeklyReportDao.updateWeeklyReport(weeklyReport)
    }

    fun getAttendanceForDate(date: String): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceForDate(date)
    }

    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()

    suspend fun saveAttendance(attendance: Attendance) {
        attendanceDao.insertAttendance(attendance)
    }

    suspend fun saveAllAttendance(attendances: List<Attendance>) {
        attendanceDao.insertAllAttendance(attendances)
    }

    fun getReportsBySantri(santriId: Int): Flow<List<Report>> {
        return reportDao.getReportsBySantri(santriId)
    }

    suspend fun getReportForSantriAndDate(santriId: Int, date: String): Report? {
        return reportDao.getReportForSantriAndDate(santriId, date)
    }

    suspend fun insertReport(report: Report): Long {
        return reportDao.insertReport(report)
    }

    suspend fun updateReport(report: Report) {
        reportDao.updateReport(report)
    }

    suspend fun getSantriByPin(pin: String): Santri? {
        return santriDao.getSantriByPin(pin)
    }

    suspend fun getSantriById(id: Int): Santri? {
        return santriDao.getSantriById(id)
    }

    suspend fun insertSantri(santri: Santri): Long {
        return santriDao.insertSantri(santri)
    }

    suspend fun updateSantri(santri: Santri) {
        santriDao.updateSantri(santri)
    }

    suspend fun deleteSantri(santri: Santri) {
        santriDao.deleteSantri(santri)
    }

    suspend fun deleteAllSantri() {
        santriDao.deleteAllSantri()
    }

    suspend fun getConfig(): AppConfig {
        var config = appConfigDao.getConfig()
        if (config == null) {
            config = AppConfig()
            appConfigDao.insertConfig(config)
            
            // Seed default santri if none exist
            val existing = santriDao.getAllSantri().firstOrNull()
            if (existing.isNullOrEmpty()) {
                santriDao.insertSantri(Santri(name = "Ahmad Fauzi", pin = "1001"))
                santriDao.insertSantri(Santri(name = "Siti Fatimah", pin = "1002"))
                santriDao.insertSantri(Santri(name = "Yusuf Al-Amin", pin = "1003"))
            }

            // Seed default active period
            val existingPeriods = evaluationPeriodDao.getAllPeriods().firstOrNull()
            if (existingPeriods.isNullOrEmpty()) {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta"))
                cal.add(java.util.Calendar.DAY_OF_YEAR, -4)
                val start = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                val end = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                
                evaluationPeriodDao.insertPeriod(
                    EvaluationPeriod(
                        name = "Periode Minggu Ini",
                        startDate = start,
                        endDate = end,
                        isActive = true
                    )
                )
            }
        } else {
            // Check if URL is empty or blank, then automatically fill and enable it
            if (config.firebaseUrl.trim().isEmpty()) {
                config = config.copy(
                    firebaseUrl = "https://alhidayah-82b02-default-rtdb.asia-southeast1.firebasedatabase.app/",
                    firebaseEnabled = true
                )
                appConfigDao.updateConfig(config)
            }
            // Also ensure we have a period even if config was loaded
            val existingPeriods = evaluationPeriodDao.getAllPeriods().firstOrNull()
            if (existingPeriods.isNullOrEmpty()) {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta"))
                cal.add(java.util.Calendar.DAY_OF_YEAR, -4)
                val start = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
                val end = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                evaluationPeriodDao.insertPeriod(
                    EvaluationPeriod(
                        name = "Periode Minggu Ini",
                        startDate = start,
                        endDate = end,
                        isActive = true
                    )
                )
            }
        }
        return config
    }

    suspend fun updateConfig(config: AppConfig) {
        appConfigDao.updateConfig(config)
    }

    suspend fun clearAllLocalReportsAndAttendance() {
        reportDao.deleteAllReports()
        attendanceDao.deleteAllAttendance()
        weeklyReportDao.deleteAllWeeklyReports()
    }
}
