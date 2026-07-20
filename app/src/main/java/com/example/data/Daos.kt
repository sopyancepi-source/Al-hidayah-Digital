package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SantriDao {
    @Query("SELECT * FROM santri ORDER BY name ASC")
    fun getAllSantri(): Flow<List<Santri>>

    @Query("SELECT * FROM santri WHERE pin = :pin LIMIT 1")
    suspend fun getSantriByPin(pin: String): Santri?

    @Query("SELECT * FROM santri WHERE id = :id LIMIT 1")
    suspend fun getSantriById(id: Int): Santri?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSantri(santri: Santri): Long

    @Update
    suspend fun updateSantri(santri: Santri)

    @Delete
    suspend fun deleteSantri(santri: Santri)

    @Query("DELETE FROM santri")
    suspend fun deleteAllSantri()
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE santriId = :santriId ORDER BY date DESC")
    fun getReportsBySantri(santriId: Int): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE santriId = :santriId AND date = :date LIMIT 1")
    suspend fun getReportForSantriAndDate(santriId: Int, date: String): Report?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report): Long

    @Update
    suspend fun updateReport(report: Report)

    @Query("DELETE FROM reports")
    suspend fun deleteAllReports()
}

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AppConfig?

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<AppConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppConfig)

    @Update
    suspend fun updateConfig(config: AppConfig)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAttendance(attendances: List<Attendance>)

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()
}

@Dao
interface EvaluationPeriodDao {
    @Query("SELECT * FROM evaluation_periods ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<EvaluationPeriod>>

    @Query("SELECT * FROM evaluation_periods WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePeriod(): EvaluationPeriod?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: EvaluationPeriod): Long

    @Update
    suspend fun updatePeriod(period: EvaluationPeriod)

    @Delete
    suspend fun deletePeriod(period: EvaluationPeriod)

    @Query("UPDATE evaluation_periods SET isActive = 0")
    suspend fun deactivateAllPeriods()
}

@Dao
interface WeeklyReportDao {
    @Query("SELECT * FROM weekly_reports ORDER BY id DESC")
    fun getAllWeeklyReports(): Flow<List<WeeklyReport>>

    @Query("SELECT * FROM weekly_reports WHERE santriId = :santriId ORDER BY id DESC")
    fun getWeeklyReportsForSantri(santriId: Int): Flow<List<WeeklyReport>>

    @Query("SELECT * FROM weekly_reports WHERE santriId = :santriId AND periodId = :periodId LIMIT 1")
    suspend fun getWeeklyReportForSantriAndPeriod(santriId: Int, periodId: Int): WeeklyReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyReport(weeklyReport: WeeklyReport): Long

    @Update
    suspend fun updateWeeklyReport(weeklyReport: WeeklyReport)

    @Query("DELETE FROM weekly_reports")
    suspend fun deleteAllWeeklyReports()
}

