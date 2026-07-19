package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import java.io.ByteArrayOutputStream

object WorshipUtils {

    // Compresses a bitmap to small JPG byte array and encodes as Base64 (typically < 3KB)
    fun compressBitmapToBase64(bitmap: Bitmap, quality: Int = 50): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Creates a programmatically drawn beautiful, colorful thumbnail representing the activity
    fun createSimulatedPhoto(activityName: String): String {
        // Create a 200x200 pixel image
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Select color background based on activity
        val (bgColor, accentColor, iconChar) = when (activityName.uppercase()) {
            "SUBUH" -> Triple(Color.parseColor("#E0F7FA"), Color.parseColor("#00838F"), "🌅")
            "ZUHUR" -> Triple(Color.parseColor("#FFFDE7"), Color.parseColor("#F57F17"), "☀️")
            "ASHAR" -> Triple(Color.parseColor("#FFF3E0"), Color.parseColor("#EF6C00"), "🌤️")
            "MAGHRIB" -> Triple(Color.parseColor("#FBE9E7"), Color.parseColor("#D84315"), "🌇")
            "ISYA" -> Triple(Color.parseColor("#E8EAF6"), Color.parseColor("#283593"), "🌙")
            "TAHAJUD" -> Triple(Color.parseColor("#F3E5F5"), Color.parseColor("#6A1B9A"), "🌌")
            "WITIR" -> Triple(Color.parseColor("#EDE7F6"), Color.parseColor("#4527A0"), "✨")
            "BACA QUR'AN", "BACA QURAN" -> Triple(Color.parseColor("#E0F2F1"), Color.parseColor("#00695C"), "📖")
            "BAKTI ORTU" -> Triple(Color.parseColor("#FCE4EC"), Color.parseColor("#C2185B"), "❤️")
            else -> Triple(Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"), "🕌")
        }

        // Draw background
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Draw decorative border
        paint.color = accentColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawRect(3f, 3f, (size - 3).toFloat(), (size - 3).toFloat(), paint)

        // Draw a soft inner gradient-like circle
        paint.color = (accentColor and 0x00FFFFFF) or 0x1E000000
        paint.style = Paint.Style.FILL
        canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), 60f, paint)

        // Draw activity icon
        paint.textSize = 55f
        paint.style = Paint.Style.FILL
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 64f
            textAlign = Paint.Align.CENTER
        }
        val yPos = (size / 2) - ((iconPaint.descent() + iconPaint.ascent()) / 2)
        canvas.drawText(iconChar, (size / 2).toFloat(), yPos - 15f, iconPaint)

        // Draw Label text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(activityName, (size / 2).toFloat(), yPos + 45f, textPaint)

        // Draw secondary visual text
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("AL-HIDAYAH", (size / 2).toFloat(), yPos + 65f, subTextPaint)

        val result = compressBitmapToBase64(bitmap, 45) // high compression for ultra-small size
        bitmap.recycle()
        return result
    }

    // Anti-cheating time validations for daily fardhu prayers
    fun checkPrayerTime(type: com.example.ui.viewmodel.WorshipType, selectedDate: String): Pair<Boolean, String> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
        }
        val todayStr = sdf.format(java.util.Date())
        if (selectedDate != todayStr) {
            // Past dates can always be filled
            return Pair(true, "")
        }
        
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta"))
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val currentMinutes = hour * 60 + minute
        
        return when (type) {
            com.example.ui.viewmodel.WorshipType.SUBUH -> {
                val startMinutes = 4 * 60 // 04:00 AM
                if (currentMinutes < startMinutes) {
                    Pair(false, "Waktu Shalat Subuh belum masuk. Shalat Subuh hari ini baru dapat dilaporkan mulai pukul 04:00 WIB.")
                } else {
                    Pair(true, "")
                }
            }
            com.example.ui.viewmodel.WorshipType.ZUHUR -> {
                val startMinutes = 11 * 60 + 45 // 11:45 AM
                if (currentMinutes < startMinutes) {
                    Pair(false, "Waktu Shalat Zuhur belum masuk. Shalat Zuhur hari ini baru dapat dilaporkan mulai pukul 11:45 WIB.")
                } else {
                    Pair(true, "")
                }
            }
            com.example.ui.viewmodel.WorshipType.ASHAR -> {
                val startMinutes = 15 * 60 // 15:00 PM
                if (currentMinutes < startMinutes) {
                    Pair(false, "Waktu Shalat Ashar belum masuk. Shalat Ashar hari ini baru dapat dilaporkan mulai pukul 15:00 WIB.")
                } else {
                    Pair(true, "")
                }
            }
            com.example.ui.viewmodel.WorshipType.MAGHRIB -> {
                val startMinutes = 17 * 60 + 45 // 17:45 PM
                if (currentMinutes < startMinutes) {
                    Pair(false, "Waktu Shalat Maghrib belum masuk. Shalat Maghrib hari ini baru dapat dilaporkan mulai pukul 17:45 WIB.")
                } else {
                    Pair(true, "")
                }
            }
            com.example.ui.viewmodel.WorshipType.ISYA -> {
                val startMinutes = 19 * 60 // 19:00 PM
                if (currentMinutes < startMinutes) {
                    Pair(false, "Waktu Shalat Isya belum masuk. Shalat Isya hari ini baru dapat dilaporkan mulai pukul 19:00 WIB.")
                } else {
                    Pair(true, "")
                }
            }
            else -> Pair(true, "")
        }
    }
}
