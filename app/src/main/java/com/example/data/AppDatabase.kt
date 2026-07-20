package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Santri::class,
        Report::class,
        AppConfig::class,
        Attendance::class,
        EvaluationPeriod::class,
        WeeklyReport::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun santriDao(): SantriDao
    abstract fun reportDao(): ReportDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun evaluationPeriodDao(): EvaluationPeriodDao
    abstract fun weeklyReportDao(): WeeklyReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "al_hidayah_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
