package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EvaluationPeriod
import com.example.data.WeeklyReport
import com.example.ui.viewmodel.AlHidayahViewModel
import com.example.ui.viewmodel.Role
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruEvaluasiMingguanTab(
    viewModel: AlHidayahViewModel,
    userRole: Role
) {
    val periods by viewModel.periodsList.collectAsStateWithLifecycle()
    val activePeriod by viewModel.activePeriod.collectAsStateWithLifecycle()
    val santriList by viewModel.santriList.collectAsStateWithLifecycle()
    val weeklyReports by viewModel.weeklyReportsList.collectAsStateWithLifecycle()

    var showCreatePeriodDialog by remember { mutableStateOf(false) }
    var selectedSantriForReport by remember { mutableStateOf<com.example.data.Santri?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Period Management
        if (userRole != Role.ADMIN1 && userRole != Role.ADMIN2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 Kelola Periode Evaluasi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C)
                        )
                        
                        Button(
                            onClick = { showCreatePeriodDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Periode Baru", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activePeriod != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2F1))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = Color(0xFF004D40),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Periode Aktif: ${activePeriod!!.name}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF004D40),
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Rentang Tanggal: ${activePeriod!!.startDate} s/d ${activePeriod!!.endDate}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF004D40).copy(alpha = 0.8f)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Belum ada periode evaluasi yang aktif! Mohon aktifkan atau buat periode baru.",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFC62828),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (periods.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Daftar Semua Periode:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        periods.forEach { period ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(
                                        1.dp,
                                        if (period.isActive) Color(0xFF00695C) else Color.LightGray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = period.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (period.isActive) Color(0xFF00695C) else Color.DarkGray
                                    )
                                    Text(
                                        text = "${period.startDate} s/d ${period.endDate}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (!period.isActive) {
                                        TextButton(
                                            onClick = { viewModel.activatePeriod(period.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Aktifkan", fontSize = 11.sp, color = Color(0xFF00695C))
                                        }
                                    } else {
                                        Text(
                                            text = "Sedang Aktif",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF004D40),
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.deletePeriod(period) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Section 2: Student Weekly Reports
        item {
            Text(
                text = "📚 Evaluasi Perkembangan Mingguan Santri",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }

        if (activePeriod == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Text(
                        text = if (userRole == Role.ADMIN1 || userRole == Role.ADMIN2) {
                            "Belum ada periode evaluasi aktif yang diatur oleh Ustaz Ngaji. Hubungi Ustaz untuk mengaktifkan periode evaluasi baru."
                        } else {
                            "Silakan buat dan aktifkan periode evaluasi terlebih dahulu untuk mulai menilai perkembangan santri."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(santriList) { santri ->
                val activePeriodId = activePeriod?.id ?: 0
                val reportForSantri = weeklyReports.find { it.santriId == santri.id && it.periodId == activePeriodId }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSantriForReport = santri },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (santri.gender == "P") Color(0xFFFCE4EC) else Color(0xFFE0F2F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (santri.gender == "P") "🧕" else "👦",
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = santri.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "PIN: ${santri.pin}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        reportForSantri == null -> Color(0xFFECEFF1)
                                        reportForSantri.isSigned -> Color(0xFFE8F5E9)
                                        else -> Color(0xFFFFF8E1)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    reportForSantri == null -> "🔴 Belum Dibuat"
                                    reportForSantri.isSigned -> "🟢 Selesai & Ditandatangani"
                                    else -> "🟡 Menunggu TTD Ortu"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    reportForSantri == null -> Color(0xFF546E7A)
                                    reportForSantri.isSigned -> Color(0xFF2E7D32)
                                    else -> Color(0xFFF57F17)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreatePeriodDialog) {
        CreatePeriodDialog(
            onDismiss = { showCreatePeriodDialog = false },
            onSave = { name, start, end, active ->
                viewModel.createOrUpdatePeriod(name = name, startDate = start, endDate = end, isActive = active)
                showCreatePeriodDialog = false
            }
        )
    }

    if (selectedSantriForReport != null) {
        val s = selectedSantriForReport!!
        val activeP = activePeriod!!
        val report = weeklyReports.find { it.santriId == s.id && it.periodId == activeP.id }
            ?: WeeklyReport(
                periodId = activeP.id,
                santriId = s.id,
                periodName = activeP.name,
                startDate = activeP.startDate,
                endDate = activeP.endDate,
                autoNotes = viewModel.generateAutoNotesForSantri(s.id, activeP.startDate, activeP.endDate)
            )

        EditWeeklyReportDialog(
            santriName = s.name,
            weeklyReport = report,
            userRole = userRole,
            onDismiss = { selectedSantriForReport = null },
            onSave = { updatedReport ->
                viewModel.saveWeeklyReport(updatedReport)
                selectedSantriForReport = null
            }
        )
    }
}

@Composable
fun CreatePeriodDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var makeActive by remember { mutableStateOf(true) }

    // Prefill logical dates (e.g. today s/d 7 days from now)
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
        startDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        endDate = sdf.format(cal.time)
        name = "Periode Minggu Ini"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tentukan Periode Evaluasi Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Periode") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Tanggal Mulai (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Tanggal Selesai (yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = makeActive, onCheckedChange = { makeActive = it })
                    Text("Jadikan Periode Aktif Sekarang")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && startDate.isNotEmpty() && endDate.isNotEmpty()) {
                        onSave(name, startDate, endDate, makeActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
            ) {
                Text("Simpan & Aktifkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeeklyReportDialog(
    santriName: String,
    weeklyReport: WeeklyReport,
    userRole: Role,
    onDismiss: () -> Unit,
    onSave: (WeeklyReport) -> Unit
) {
    var ustazNotes by remember {
        mutableStateOf(
            if (weeklyReport.ustazNotes.isEmpty()) {
                if (weeklyReport.parentNotes.isNotEmpty() && userRole == Role.GURU) {
                    weeklyReport.parentNotes
                } else if (weeklyReport.adminNotes.isNotEmpty() && userRole == Role.GURU) {
                    weeklyReport.adminNotes
                } else {
                    ""
                }
            } else {
                weeklyReport.ustazNotes
            }
        )
    }
    var adminNotes by remember { mutableStateOf(weeklyReport.adminNotes) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Evaluasi: $santriName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C)
                        )
                        Text(
                            text = "${weeklyReport.periodName} (${weeklyReport.startDate} s/d ${weeklyReport.endDate})",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // System Auto-Notes Card OR Parent Feedback Card
                    if (userRole != Role.ADMIN1 && userRole != Role.ADMIN2) {
                        if (weeklyReport.isSigned && weeklyReport.parentNotes.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Feedback,
                                            contentDescription = "Feedback Ortu",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "💬 Catatan Balik / Feedback Orang Tua:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = weeklyReport.parentNotes,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Oleh Orang Tua: ${weeklyReport.parentSignedName}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F8))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🤖 Penilaian Otomatis Aplikasi:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF37474F)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = weeklyReport.autoNotes.ifEmpty { "Belum ada laporan harian yang diisi pada periode ini." },
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Admin view vs Ustaz view
                    if (userRole == Role.ADMIN1 || userRole == Role.ADMIN2) {
                        // Admin Manual Input Mode
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F51B5).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Info Admin", tint = Color(0xFF3F51B5))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Laporan mingguan Admin ini ditulis secara manual dan dikirim ke Ustaz Ngaji, untuk kemudian disatukan ke dalam Laporan Mingguan Ustaz.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1A237E),
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = adminNotes,
                            onValueChange = { adminNotes = it },
                            label = { Text("🛠️ Catatan Admin Pengajar (Manual)") },
                            placeholder = { Text("Tulis catatan perkembangan santri selama di masjid pekan ini...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )
                    } else if (userRole == Role.GURU) {
                        // Ustaz Ngaji Mode: can see Admin notes and merge them, and can edit Ustaz notes
                        if (weeklyReport.adminNotes.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB39DDB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = "Admin Notes Icon", tint = Color(0xFF5E35B1), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Laporan Masuk dari Admin Pengajar Pekan Ini:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF5E35B1)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = weeklyReport.adminNotes,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (!ustazNotes.contains(weeklyReport.adminNotes)) {
                                                ustazNotes = if (ustazNotes.isEmpty()) {
                                                    weeklyReport.adminNotes
                                                } else {
                                                    ustazNotes + "\n\n" + weeklyReport.adminNotes
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E35B1)),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Gabung", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Satukan Catatan Admin ke Laporan Ustaz", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ustazNotes,
                            onValueChange = { ustazNotes = it },
                            label = { Text("✍️ Catatan Ustaz Ngaji") },
                            placeholder = { Text("Berikan ulasan pembelajaran, akhlak, hafalan, atau kedisiplinan santri...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )
                    }

                    // Parent Signature Display (If Signed) - HIDE from ADMIN1/ADMIN2
                    if (weeklyReport.isSigned && userRole != Role.ADMIN1 && userRole != Role.ADMIN2) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "✅ Sudah Dibaca & Ditandatangani Orang Tua",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Nama Ortu: ${weeklyReport.parentSignedName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (weeklyReport.parentNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Catatan Balik Ortu: \"${weeklyReport.parentNotes}\"",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(weeklyReport.signedTimestamp))
                                        Text(
                                            text = "Waktu: $dateStr WIB",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
 
                                    // Decode and show signature image
                                    val signatureBitmap = remember(weeklyReport.parentSignature) {
                                        if (!weeklyReport.parentSignature.isNullOrEmpty()) {
                                            try {
                                                val bytes = Base64.decode(weeklyReport.parentSignature, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                            } catch (e: Exception) {
                                                null
                                            }
                                        } else null
                                    }
 
                                    if (signatureBitmap != null) {
                                        Image(
                                            bitmap = signatureBitmap,
                                            contentDescription = "Signature",
                                            modifier = Modifier
                                                .size(100.dp, 50.dp)
                                                .border(1.dp, Color(0xFF81C784), RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
 
                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
 
                    val isUserAdmin = userRole == Role.ADMIN1 || userRole == Role.ADMIN2
                    val buttonText = if (isUserAdmin) "Simpan & Kirim ke Ustaz" else "Simpan & Kirim ke Ortu"
 
                    Button(
                        onClick = {
                            val savedReport = weeklyReport.copy(
                                ustazNotes = ustazNotes,
                                adminNotes = adminNotes,
                                isSentToParent = weeklyReport.isSentToParent || (userRole == Role.GURU)
                            )
                            onSave(savedReport)
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C))
                    ) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
fun UnsignedWeeklyReportOverlay(
    weeklyReport: WeeklyReport,
    viewModel: AlHidayahViewModel
) {
    var showFullReport by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🚨 PERHATIAN ORANG TUA! 🚨",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Laporan Evaluasi Perkembangan Mingguan Santri telah dikirim oleh Guru Ngaji untuk periode:\n\n*${weeklyReport.periodName}*",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )

                Text(
                    text = "Sesuai aturan, Anda wajib membaca, memberikan catatan balik, dan menandatangani laporan evaluasi mingguan ini terlebih dahulu sebelum dapat melanjutkan mengisi laporan ibadah harian santri.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = { showFullReport = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = "Buka")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Laporan & Tanda Tangani", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showFullReport) {
        ParentSignWeeklyReportDialog(
            weeklyReport = weeklyReport,
            onDismiss = { showFullReport = false },
            onSignSaved = { name, signature, feedback ->
                val signed = weeklyReport.copy(
                    parentSignedName = name,
                    parentSignature = signature,
                    parentNotes = feedback,
                    isSigned = true,
                    signedTimestamp = System.currentTimeMillis()
                )
                viewModel.updateWeeklyReport(signed)
                showFullReport = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSignWeeklyReportDialog(
    weeklyReport: WeeklyReport,
    onDismiss: () -> Unit,
    onSignSaved: (String, String, String) -> Unit
) {
    var parentName by remember { mutableStateOf("") }
    var parentNotes by remember { mutableStateOf("") }
    var signatureBase64 by remember { mutableStateOf<String?>(null) }
    var signatureError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laporan Mingguan Santri",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C)
                        )
                        Text(
                            text = "Periode: ${weeklyReport.startDate} s/d ${weeklyReport.endDate}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Batal")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // System Auto Notes
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7F8))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🤖 Penilaian Otomatis Aplikasi:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF37474F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = weeklyReport.autoNotes.ifEmpty { "Belum ada laporan harian pada periode ini." },
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Teacher/Ustaz Notes
                    if (weeklyReport.ustazNotes.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF80CBC4))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "✍️ Catatan Ustaz Ngaji:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF004D40)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = weeklyReport.ustazNotes,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Admin Notes
                    if (weeklyReport.adminNotes.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB39DDB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🛠️ Catatan Admin Pengajar:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF311B92)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = weeklyReport.adminNotes,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Feedbacks and signature section
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "✍️ Verifikasi & Tanda Tangan Orang Tua",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )

                            OutlinedTextField(
                                value = parentName,
                                onValueChange = { parentName = it },
                                label = { Text("Nama Orang Tua") },
                                placeholder = { Text("Masukkan nama lengkap Anda...") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = parentName.isEmpty() && signatureBase64 != null
                            )

                            OutlinedTextField(
                                value = parentNotes,
                                onValueChange = { parentNotes = it },
                                label = { Text("Catatan Balik ke Ustaz (Opsional)") },
                                placeholder = { Text("Tulis pesan atau tanggapan Anda mengenai perkembangan anak...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Draw interactive signature
                            SignaturePad(
                                modifier = Modifier.fillMaxWidth(),
                                onSignatureSaved = { base64 ->
                                    signatureBase64 = base64
                                    signatureError = false
                                },
                                onCleared = {
                                    signatureBase64 = null
                                }
                            )

                            if (signatureError) {
                                Text(
                                    text = "Mohon bubuhkan dan simpan tanda tangan Anda terlebih dahulu!",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (signatureBase64 != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F5E9))
                                        .border(1.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Check, contentDescription = "OK", tint = Color(0xFF2E7D32))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tanda tangan berhasil disimpan secara terkompresi!",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Submit Row
                Button(
                    onClick = {
                        if (parentName.isEmpty()) {
                            // Give some simple indicator
                            return@Button
                        }
                        if (signatureBase64 == null) {
                            signatureError = true
                            return@Button
                        }
                        
                        onSignSaved(parentName, signatureBase64!!, parentNotes)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = parentName.isNotEmpty() && signatureBase64 != null
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Kirim")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim Evaluasi & Buka Kunci Harian", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
