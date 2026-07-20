package com.example

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Report
import com.example.data.Santri
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AlHidayahViewModel
import com.example.ui.viewmodel.Role
import com.example.ui.viewmodel.WorshipType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen() {
    val viewModel: AlHidayahViewModel = viewModel()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val activeForm by viewModel.activeForm.collectAsStateWithLifecycle()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppFooter()
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = Triple(currentRole, isAuthenticated, activeForm),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "MainAppScreenTransitions"
            ) { (role, authed, form) ->
                when {
                    role == Role.NONE -> {
                        RoleSelectionScreen(viewModel = viewModel, onRoleSelected = { viewModel.selectRole(it) })
                    }
                    !authed -> {
                        PinVerificationScreen(
                            role = role,
                            viewModel = viewModel,
                            onBack = { viewModel.logout() }
                        )
                    }
                    role == Role.PARENT && form != null -> {
                        ParentFormContainer(
                            type = form,
                            viewModel = viewModel,
                            onClose = { viewModel.closeForm() }
                        )
                    }
                    role == Role.PARENT -> {
                        ParentDashboardScreen(viewModel = viewModel)
                    }
                    role == Role.GURU -> {
                        GuruDashboardScreen(viewModel = viewModel)
                    }
                    role == Role.ADMIN1 -> {
                        AdminPortalScreen(adminNumber = 1, viewModel = viewModel)
                    }
                    role == Role.ADMIN2 -> {
                        AdminPortalScreen(adminNumber = 2, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F7F8))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "@2026 cepisopyan | Empowering Communities through Devoted Technology",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

// 1. Role Selection Screen (Vibrant, Scrollable, and Connected)
@Composable
fun RoleSelectionScreen(viewModel: AlHidayahViewModel, onRoleSelected: (Role) -> Unit) {
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    var showConnectDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F8))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Decorative Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF00695C))
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🕌",
                    fontSize = 54.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "AL-HIDAYAH HUB",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Kp. Ciloa, Connecting Parents & Teachers",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE0F2F1)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                    Color(0xFFE0F2F1) else Color(0xFFECEFF1)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                    Color(0xFF00695C).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isSyncing) Icons.Default.Sync
                            else if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                            Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Database Connection Status",
                        tint = if (isSyncing) Color(0xFFEF6C00)
                            else if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                            Color(0xFF00695C) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isSyncing) "SINKRONISASI DATA..."
                                else if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                                "Koneksi: TERHUBUNG" else "Koneksi: OFFLINE (LOKAL)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSyncing) Color(0xFFEF6C00)
                                else if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                                Color(0xFF004D40) else Color.DarkGray
                        )
                        Text(
                            text = if (isSyncing) syncProgress
                                else if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                                "Otomatis terhubung ke Ustaz" else "Hubungkan agar laporan masuk ke Ustaz",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Button(
                    onClick = { showConnectDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                            Color(0xFF00695C) else Color(0xFFEF6C00)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) 
                            "Ubah Koneksi" else "Hubungkan 🔗",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pilih Mode Akses Aplikasi:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        // Menu Cards with distinct visual themes to avoid visual boredom
        RoleMenuCard(
            title = "Mode Orang Tua Santri",
            description = "Isi laporan aktivitas ibadah harian santri di rumah.",
            icon = "👨‍👩‍👦",
            containerColor = Color(0xFFE8F5E9),
            accentColor = Color(0xFF2E7D32),
            onClick = { onRoleSelected(Role.PARENT) }
        )

        RoleMenuCard(
            title = "Mode Guru Ngaji",
            description = "Pantau laporan ibadah realtime & kelola santri.",
            icon = "👳‍♂️",
            containerColor = Color(0xFFE0F2F1),
            accentColor = Color(0xFF00695C),
            onClick = { onRoleSelected(Role.GURU) }
        )

        RoleMenuCard(
            title = "Mode Admin 1",
            description = "Akses dashboard administrasi al-hidayah pertama.",
            icon = "⚙️",
            containerColor = Color(0xFFE8EAF6),
            accentColor = Color(0xFF283593),
            onClick = { onRoleSelected(Role.ADMIN1) }
        )

        RoleMenuCard(
            title = "Mode Admin 2",
            description = "Akses dashboard administrasi al-hidayah kedua.",
            icon = "🛡️",
            containerColor = Color(0xFFFFF3E0),
            accentColor = Color(0xFFEF6C00),
            onClick = { onRoleSelected(Role.ADMIN2) }
        )
    }

    if (showConnectDialog) {
        var firebaseUrlInput by remember { mutableStateOf(appConfig.firebaseUrl) }
        
        AlertDialog(
            onDismissRequest = { if (!isSyncing) showConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔗", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("Hubungkan ke Database Ustaz", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Masukkan URL Firebase Realtime Database yang diberikan oleh Ustaz agar data laporan harian, absensi, dan data santri tersinkronisasi secara otomatis.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    
                    OutlinedTextField(
                        value = firebaseUrlInput,
                        onValueChange = { firebaseUrlInput = it },
                        label = { Text("URL Database Firebase") },
                        placeholder = { Text("https://nama-database.firebaseio.com/") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Firebase URL", tint = Color(0xFFEF6C00)) },
                        singleLine = true,
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFEF6C00))
                            Text(syncProgress, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF6C00))
                        }
                    } else if (appConfig.firebaseUrl.isNotEmpty() && appConfig.firebaseEnabled) {
                        Text(
                            "Status saat ini: Terhubung ke database. Jika Anda ingin memutus koneksi, kosongkan URL di atas lalu klik Simpan.",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUrl = firebaseUrlInput.trim()
                        if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("https://")) {
                            Toast.makeText(context, "URL harus diawali dengan https://", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.connectAndSyncFirebase(cleanUrl) { success ->
                                if (success) {
                                    Toast.makeText(context, "Database berhasil terhubung dan disinkronkan!", Toast.LENGTH_LONG).show()
                                    showConnectDialog = false
                                } else {
                                    Toast.makeText(context, "Koneksi disimpan, namun gagal mengunduh data dari Firebase. Silakan periksa koneksi internet Anda.", Toast.LENGTH_LONG).show()
                                    showConnectDialog = false
                                }
                            }
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00))
                ) {
                    Text(if (isSyncing) "Menghubungkan..." else "Simpan & Sinkronkan", color = Color.White)
                }
            },
            dismissButton = {
                if (!isSyncing) {
                    OutlinedButton(onClick = { showConnectDialog = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }
}

@Composable
fun RoleMenuCard(
    title: String,
    description: String,
    icon: String,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = accentColor
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}

// 2. PIN Verification Screen with highly responsive numeric keypad
@Composable
fun PinVerificationScreen(
    role: Role,
    viewModel: AlHidayahViewModel,
    onBack: () -> Unit
) {
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    var enteredPin by remember { mutableStateOf("") }
    val context = LocalContext.current

    val roleName = when (role) {
        Role.PARENT -> "Orang Tua (Masuk lewat PIN Anak)"
        Role.GURU -> "Guru Ngaji"
        Role.ADMIN1 -> "Admin 1"
        Role.ADMIN2 -> "Admin 2"
        Role.NONE -> ""
    }

    val primaryColor = when (role) {
        Role.PARENT -> Color(0xFF2E7D32)
        Role.GURU -> Color(0xFF00695C)
        Role.ADMIN1 -> Color(0xFF283593)
        Role.ADMIN2 -> Color(0xFFEF6C00)
        Role.NONE -> Color.Black
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F8))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Back Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = primaryColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "KEMBALI",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = primaryColor
            )
        }

        // Header Information
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = "PENGAMAN PIN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mode: $roleName",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            
            if (role == Role.PARENT) {
                Text(
                    text = "*Gunakan PIN Ahmad (1001), Siti (1002), atau Yusuf (1003) untuk mencoba.",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (role == Role.GURU) {
                if (appConfig.guruPin == "1234") {
                    Text(
                        text = "*Gunakan PIN Guru default: 1234",
                        fontSize = 11.sp,
                        color = Color(0xFF00695C),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Pin Dots Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            for (i in 1..4) {
                val isFilled = enteredPin.length >= i
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) primaryColor else Color.LightGray)
                        .border(1.dp, Color.Gray, CircleShape)
                )
            }
        }

        // Keypad Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("✖", "0", "✔")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    when (key) {
                                        "✖" -> Color(0xFFEF9A9A)
                                        "✔" -> Color(0xFFA5D6A7)
                                        else -> Color.White
                                    }
                                )
                                .border(1.dp, Color.LightGray, CircleShape)
                                .clickable {
                                    when (key) {
                                        "✖" -> {
                                            if (enteredPin.isNotEmpty()) enteredPin =
                                                enteredPin.dropLast(1)
                                        }
                                        "✔" -> {
                                            if (enteredPin.length == 4) {
                                                viewModel.loginWithPin(enteredPin) { ok ->
                                                    if (!ok) {
                                                        enteredPin = ""
                                                        Toast
                                                            .makeText(
                                                                context,
                                                                "PIN salah! Silakan coba lagi.",
                                                                Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                    }
                                                }
                                            } else {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "PIN harus 4 digit!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                if (enteredPin.length == 4) {
                                                    // Auto-trigger on 4 digits for swift experience
                                                    viewModel.loginWithPin(enteredPin) { ok ->
                                                        if (!ok) {
                                                            enteredPin = ""
                                                            Toast
                                                                .makeText(
                                                                    context,
                                                                    "PIN salah! Silakan coba lagi.",
                                                                    Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (key) {
                                    "✖" -> Color(0xFFB71C1C)
                                    "✔" -> Color(0xFF1B5E20)
                                    else -> Color.DarkGray
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 3. Parent Form Switch Container
@Composable
fun ParentFormContainer(
    type: WorshipType,
    viewModel: AlHidayahViewModel,
    onClose: () -> Unit
) {
    val report by viewModel.parentReport.collectAsStateWithLifecycle()
    val r = report ?: return

    when (type) {
        WorshipType.SUBUH -> ShalatForm(
            title = "SUBUH",
            primaryColor = Color(0xFF00838F),
            initialWaktu = r.subuhWaktu,
            initialFoto = r.subuhFoto,
            initialCara = r.subuhCara,
            onSave = { waktu, foto, cara -> viewModel.saveSubuh(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.ZUHUR -> ShalatForm(
            title = "ZUHUR",
            primaryColor = Color(0xFFF57F17),
            initialWaktu = r.zuhurWaktu,
            initialFoto = r.zuhurFoto,
            initialCara = r.zuhurCara,
            onSave = { waktu, foto, cara -> viewModel.saveZuhur(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.ASHAR -> ShalatForm(
            title = "ASHAR",
            primaryColor = Color(0xFFEF6C00),
            initialWaktu = r.asharWaktu,
            initialFoto = r.asharFoto,
            initialCara = r.asharCara,
            onSave = { waktu, foto, cara -> viewModel.saveAshar(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.MAGHRIB -> ShalatForm(
            title = "MAGHRIB",
            primaryColor = Color(0xFFD84315),
            initialWaktu = r.maghribWaktu,
            initialFoto = r.maghribFoto,
            initialCara = r.maghribCara,
            onSave = { waktu, foto, cara -> viewModel.saveMaghrib(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.ISYA -> ShalatForm(
            title = "ISYA",
            primaryColor = Color(0xFF283593),
            initialWaktu = r.isyaWaktu,
            initialFoto = r.isyaFoto,
            initialCara = r.isyaCara,
            onSave = { waktu, foto, cara -> viewModel.saveIsya(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.TAHAJUD -> ShalatForm(
            title = "TAHAJUD (SUNNAH)",
            primaryColor = Color(0xFF6A1B9A),
            initialWaktu = r.tahajudWaktu,
            initialFoto = r.tahajudFoto,
            initialCara = r.tahajudCara,
            onSave = { waktu, foto, cara -> viewModel.saveTahajud(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.WITIR -> ShalatForm(
            title = "WITIR (SUNNAH)",
            primaryColor = Color(0xFF4527A0),
            initialWaktu = r.witirWaktu,
            initialFoto = r.witirFoto,
            initialCara = r.witirCara,
            onSave = { waktu, foto, cara -> viewModel.saveWitir(waktu, foto, cara) },
            onClose = onClose
        )
        WorshipType.ZIKIR -> ZikirForm(
            initialBacaan = r.zikirBacaan,
            initialJumlah = r.zikirJumlah,
            onSave = { bacaan, jumlah -> viewModel.saveZikir(bacaan, jumlah) },
            onClose = onClose
        )
        WorshipType.QURAN -> QuranForm(
            initialSurat = r.quranSurat,
            initialAyat = r.quranAyat,
            initialJilid = r.iqroJilid,
            initialHalaman = r.iqroHalaman,
            onSave = { isQuran, f1, f2 ->
                if (isQuran) viewModel.saveQuran(f1, f2) else viewModel.saveIqro(f1, f2)
            },
            onClose = onClose
        )
        WorshipType.BAKTI -> BaktiForm(
            initialJenis = r.baktiJenis,
            initialFoto = r.baktiFoto,
            onSave = { jenis, foto -> viewModel.saveBakti(jenis, foto) },
            onClose = onClose
        )
        WorshipType.VERIFIKASI -> {
            val missingPrayers = mutableListOf<String>()
            if (!r.isHaid) {
                if (r.maghribWaktu == null) missingPrayers.add("Maghrib")
                if (r.isyaWaktu == null) missingPrayers.add("Isya")
                if (r.subuhWaktu == null) missingPrayers.add("Subuh")
                if (r.zuhurWaktu == null) missingPrayers.add("Zuhur")
                if (r.asharWaktu == null) missingPrayers.add("Ashar")
            }
            
            VerifikasiForm(
                initialNamaOrtu = r.parentName,
                missingPrayers = missingPrayers,
                onSave = { nama, sig -> viewModel.saveVerifikasi(nama, sig) },
                onClose = onClose
            )
        }
    }
}

// 4. Parent Dashboard (Checklist of worship activities)
@Composable
fun ParentDashboardScreen(viewModel: AlHidayahViewModel) {
    val activeSantri by viewModel.activeSantri.collectAsStateWithLifecycle()
    val report by viewModel.parentReport.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDateString.collectAsStateWithLifecycle()
    val weeklyReports by viewModel.weeklyReportsList.collectAsStateWithLifecycle()

    val santri = activeSantri ?: return
    val r = report ?: Report(santriId = santri.id, santriName = santri.name, date = selectedDate)

    val unsignedWeeklyReport = weeklyReports.find { it.santriId == santri.id && !it.isSigned && it.isSentToParent }

    val isVerified = r.parentName.isNotEmpty() && r.parentSignature != null

    var showWarningDialog by remember { mutableStateOf(false) }
    var warningTitle by remember { mutableStateOf("") }
    var warningMessage by remember { mutableStateOf("") }
    var warningIcon by remember { mutableStateOf("⚠️") }

    val onCardClick = { type: WorshipType ->
        if (isVerified && type != WorshipType.VERIFIKASI) {
            warningTitle = "Laporan Terkunci"
            warningMessage = "Laporan hari ini sudah ditandatangani oleh Orang Tua dan dikirim ke Guru. Laporan yang sudah dikirim tidak dapat diubah lagi demi menjaga kejujuran santri.\n\nJika ada kekeliruan, mohon hubungi Guru Ngaji secara langsung."
            warningIcon = "🔒"
            showWarningDialog = true
        } else {
            val checkResult = com.example.ui.components.WorshipUtils.checkPrayerTime(type, selectedDate)
            if (!checkResult.first) {
                warningTitle = "Belum Waktunya"
                warningMessage = checkResult.second
                warningIcon = "⏳"
                showWarningDialog = true
            } else {
                viewModel.openForm(type)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7F8))
        ) {
        // Dashboard header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Selamat Datang,",
                        fontSize = 12.sp,
                        color = Color(0xFFA5D6A7),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ortu dari: ${santri.name}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Masjid Al-Hidayah Kp. Ciloa",
                        fontSize = 11.sp,
                        color = Color(0xFFC8E6C9)
                    )
                }

                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                }
            }
        }

        // Date Switcher row
        ParentDateSwitcherRow(
            selectedDate = selectedDate,
            onDateChanged = { viewModel.setDate(it) }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // High-fidelity dynamic submission status banner
            item {
                if (isVerified) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🕌", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text(
                                    text = "Laporan Sudah Terkirim & Terkunci",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Telah disahkan oleh: ${r.parentName}\nLaporan tersimpan aman dan tidak dapat diubah lagi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFF176))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📝", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                            Column {
                                Text(
                                    text = "Laporan Sedang Diisi",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57F17),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Silakan isi aktivitas harian anak. Klik tombol di bawah setelah selesai untuk menandatangani dan mengirim ke Guru.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
            }

            if (santri.gender == "Perempuan") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (r.isHaid) Color(0xFFFFEBEE) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (r.isHaid) Color(0xFFE57373) else Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🌸", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                Column {
                                    Text(
                                        text = "Santriwati Sedang Haid / Halangan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (r.isHaid) Color(0xFFC62828) else Color.DarkGray
                                    )
                                    Text(
                                        text = if (r.isHaid) "Hanya Zikir dan Bakti Ortu yang dinilai" else "Khusus santriwati (perempuan) yang berhalangan shalat",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            Switch(
                                checked = r.isHaid,
                                onCheckedChange = { isChecked ->
                                    if (!isVerified) {
                                        viewModel.saveHaidStatus(isChecked)
                                    }
                                },
                                enabled = !isVerified,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFC62828),
                                    checkedTrackColor = Color(0xFFFFCDD2),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "A. LAPORAN SHALAT 5 WAKTU (SIKLUS HIJRIAH)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Maghrib",
                    subtitle = "Sore Kemarin (Awal siklus hari baru)",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.maghribWaktu != null) {
                        if (r.maghribWaktu == "Tidak Shalat") "Tidak Shalat: ${r.maghribCara?.removePrefix("Tidak Shalat: ")}"
                        else "${r.maghribWaktu} (${r.maghribCara})"
                    } else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.maghribWaktu != null,
                    primaryColor = Color(0xFFD84315),
                    icon = "🌇",
                    photoBase64 = if (r.isHaid) null else r.maghribFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.MAGHRIB)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Isya",
                    subtitle = "Malam Kemarin",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.isyaWaktu != null) {
                        if (r.isyaWaktu == "Tidak Shalat") "Tidak Shalat: ${r.isyaCara?.removePrefix("Tidak Shalat: ")}"
                        else "${r.isyaWaktu} (${r.isyaCara})"
                    } else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.isyaWaktu != null,
                    primaryColor = Color(0xFF283593),
                    icon = "🌙",
                    photoBase64 = if (r.isHaid) null else r.isyaFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.ISYA)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Subuh",
                    subtitle = "Pagi Hari Ini",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.subuhWaktu != null) {
                        if (r.subuhWaktu == "Tidak Shalat") "Tidak Shalat: ${r.subuhCara?.removePrefix("Tidak Shalat: ")}"
                        else "${r.subuhWaktu} (${r.subuhCara})"
                    } else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.subuhWaktu != null,
                    primaryColor = Color(0xFF00838F),
                    icon = "🌅",
                    photoBase64 = if (r.isHaid) null else r.subuhFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.SUBUH)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Zuhur",
                    subtitle = "Siang Hari Ini",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.zuhurWaktu != null) {
                        if (r.zuhurWaktu == "Tidak Shalat") "Tidak Shalat: ${r.zuhurCara?.removePrefix("Tidak Shalat: ")}"
                        else "${r.zuhurWaktu} (${r.zuhurCara})"
                    } else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.zuhurWaktu != null,
                    primaryColor = Color(0xFFF57F17),
                    icon = "☀️",
                    photoBase64 = if (r.isHaid) null else r.zuhurFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.ZUHUR)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Ashar",
                    subtitle = "Sore Hari Ini (Penutup laporan)",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.asharWaktu != null) {
                        if (r.asharWaktu == "Tidak Shalat") "Tidak Shalat: ${r.asharCara?.removePrefix("Tidak Shalat: ")}"
                        else "${r.asharWaktu} (${r.asharCara})"
                    } else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.asharWaktu != null,
                    primaryColor = Color(0xFFEF6C00),
                    icon = "🌤️",
                    photoBase64 = if (r.isHaid) null else r.asharFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.ASHAR)
                        }
                    }
                )
            }

            item {
                Text(
                    text = "B. LAPORAN IBADAH SUNAH & BAKTI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Tahajud",
                    subtitle = "Pelaksanaan shalat sunah di sepertiga malam",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.tahajudWaktu != null) "${r.tahajudWaktu} (${r.tahajudCara})" else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.tahajudWaktu != null,
                    primaryColor = Color(0xFF6A1B9A),
                    icon = "🌌",
                    photoBase64 = if (r.isHaid) null else r.tahajudFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.TAHAJUD)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Shalat Witir",
                    subtitle = "Pelaksanaan shalat sunah witir penutup shalat malam",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else if (r.witirWaktu != null) "${r.witirWaktu} (${r.witirCara})" else "Belum dilaporkan",
                    isCompleted = r.isHaid || r.witirWaktu != null,
                    primaryColor = Color(0xFF4527A0),
                    icon = "✨",
                    photoBase64 = if (r.isHaid) null else r.witirFoto,
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.WITIR)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Zikir Harian",
                    subtitle = "Zikir yang dibaca beserta jumlah hitungannya",
                    statusText = if (r.zikirBacaan != null) {
                        if (r.zikirBacaan == "Tidak Melaksanakan") "Tidak Melaksanakan"
                        else "${r.zikirBacaan} (${r.zikirJumlah} kali)"
                    } else "Belum dilaporkan",
                    isCompleted = r.zikirBacaan != null,
                    primaryColor = Color(0xFF2E7D32),
                    icon = "📿",
                    onClick = { onCardClick(WorshipType.ZIKIR) }
                )
            }

            item {
                ParentActivityCard(
                    title = "Baca Al-Qur'an / Iqro",
                    subtitle = "Laporan mengaji ayat surat quran atau halaman iqro",
                    statusText = if (r.isHaid) "🌸 Sedang Haid (Bebas Halangan)" else when {
                        r.quranSurat != null -> "Surat ${r.quranSurat} Ayat ${r.quranAyat}"
                        r.iqroJilid != null -> "Iqro Jilid ${r.iqroJilid} Halaman ${r.iqroHalaman}"
                        else -> "Belum dilaporkan"
                    },
                    isCompleted = r.isHaid || (r.quranSurat != null || r.iqroJilid != null),
                    primaryColor = Color(0xFF00695C),
                    icon = "📖",
                    onClick = { 
                        if (!r.isHaid) {
                            onCardClick(WorshipType.QURAN)
                        }
                    }
                )
            }

            item {
                ParentActivityCard(
                    title = "Bakti Orang Tua",
                    subtitle = "Jenis perbuatan bakti membantu bapak/ibu di rumah",
                    statusText = if (r.baktiJenis != null) r.baktiJenis!! else "Belum dilaporkan",
                    isCompleted = r.baktiJenis != null,
                    primaryColor = Color(0xFFC2185B),
                    icon = "❤️",
                    photoBase64 = r.baktiFoto,
                    onClick = { onCardClick(WorshipType.BAKTI) }
                )
            }

            item {
                Text(
                    text = "C. VERIFIKASI ORANG TUA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF37474F),
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
            }

            item {
                ParentActivityCard(
                    title = "Tanda Tangan Verifikasi",
                    subtitle = "Tanda tangan ortu untuk mengesahkan aktivitas harian",
                    statusText = if (r.parentName.isNotEmpty()) "Disahkan oleh: ${r.parentName}" else "Belum diverifikasi",
                    isCompleted = r.parentName.isNotEmpty() && r.parentSignature != null,
                    primaryColor = Color(0xFF37474F),
                    icon = "✍️",
                    photoBase64 = r.parentSignature,
                    onClick = { viewModel.openForm(WorshipType.VERIFIKASI) }
                )
            }

            // Elegant, big CTA button at the bottom of the checklist to sign and submit
            if (!isVerified) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.openForm(WorshipType.VERIFIKASI) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "Kirim")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TANDA TANGANI & KIRIM LAPORAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (unsignedWeeklyReport != null) {
        com.example.ui.components.UnsignedWeeklyReportOverlay(
            weeklyReport = unsignedWeeklyReport,
            viewModel = viewModel
        )
    }
}

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            icon = { Text(warningIcon, fontSize = 36.sp) },
            title = { Text(warningTitle, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
            text = { Text(warningMessage, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { showWarningDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Mengerti")
                }
            }
        )
    }
}

@Composable
fun ParentDateSwitcherRow(
    selectedDate: String,
    onDateChanged: (String) -> Unit
) {
    val jakartaTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = jakartaTimeZone
    }
    val formatDisplay = SimpleDateFormat("EEE, dd MMM", Locale("id", "ID")).apply {
        timeZone = jakartaTimeZone
    }
    
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance(jakartaTimeZone)
        cal.add(Calendar.DAY_OF_YEAR, -2)
        list.add(cal.time) // 2 days ago
        cal.add(Calendar.DAY_OF_YEAR, 1)
        list.add(cal.time) // yesterday
        cal.add(Calendar.DAY_OF_YEAR, 1)
        list.add(cal.time) // today
        list
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Pilih Tanggal Laporan:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { date ->
                    val dateStr = sdf.format(date)
                    val isSelected = dateStr == selectedDate
                    
                    val displayText = when {
                        dateStr == sdf.format(Date()) -> "Hari Ini"
                        dateStr == sdf.format(Calendar.getInstance(jakartaTimeZone).apply { add(Calendar.DAY_OF_YEAR, -1) }.time) -> "Kemarin"
                        else -> formatDisplay.format(date)
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDateChanged(dateStr) },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF2E7D32) else Color(0xFFECEFF1)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentActivityCard(
    title: String,
    subtitle: String,
    statusText: String,
    isCompleted: Boolean,
    primaryColor: Color,
    icon: String,
    photoBase64: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(primaryColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryColor
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) {
                        if (statusText.startsWith("Tidak Shalat") || statusText.contains("Tidak Melaksanakan")) Color(0xFFC62828)
                        else Color(0xFF2E7D32)
                    } else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (photoBase64 != null) {
                val bitmap = remember(photoBase64) {
                    try {
                        val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isCompleted) "Selesai" else "Belum",
                tint = if (isCompleted) {
                    if (statusText.startsWith("Tidak Shalat")) Color(0xFFC62828)
                    else Color(0xFF2E7D32)
                } else Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// 5. Guru Ngaji Dashboard (Four Tabs: Laporan Realtime, Rekap Laporan, Kelola Santri, Pengaturan PIN)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuruDashboardScreen(viewModel: AlHidayahViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Laporan Realtime", "Rekap Laporan", "Evaluasi Mingguan", "Kelola Santri", "PIN & Firebase")

    val reports by viewModel.reportList.collectAsStateWithLifecycle()
    val santriList by viewModel.santriList.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterDate by remember {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }.format(Date())
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F8))
    ) {
        // Teacher Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF00695C))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Masjid Al-Hidayah Kp. Ciloa",
                        fontSize = 11.sp,
                        color = Color(0xFFB2DFDB),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Portal Guru Ngaji 👳‍♂️",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Keluar", tint = Color.White)
                }
            }
        }

        // Tab selection row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF00695C)
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> GuruLaporanTab(
                reports = reports,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                filterDate = filterDate,
                onFilterDateChange = { filterDate = it }
            )
            1 -> GuruRekapLaporanTab(
                reports = reports,
                filterDate = filterDate,
                onFilterDateChange = { filterDate = it }
            )
            2 -> com.example.ui.components.GuruEvaluasiMingguanTab(
                viewModel = viewModel,
                userRole = com.example.ui.viewmodel.Role.GURU
            )
            3 -> GuruKelolaSantriTab(
                santriList = santriList,
                onAddSantri = { name, pin, gender -> viewModel.addNewSantri(name, pin, gender) },
                onDeleteSantri = { viewModel.deleteSantri(it) },
                onUpdateSantri = { id, name, pin, gender -> viewModel.updateSantri(id, name, pin, gender) }
            )
            4 -> GuruAturPinTab(
                appConfig = appConfig,
                viewModel = viewModel,
                onSavePins = { g, a1, a2 ->
                    viewModel.updateSystemPins(g, a1, a2)
                },
                onSaveFirebase = { url, enabled ->
                    viewModel.updateFirebaseConfig(url, enabled)
                }
            )
        }
    }
}

// 5a. Laporan Realtime Tab Composable
@Composable
fun GuruLaporanTab(
    reports: List<Report>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterDate: String,
    onFilterDateChange: (String) -> Unit
) {
    val filteredReports = reports.filter { report ->
        val name = report.santriName ?: ""
        val d = report.date ?: ""
        val matchesSearch = name.contains(searchQuery, ignoreCase = true)
        val matchesDate = d == filterDate
        matchesSearch && matchesDate
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search & Date Filter controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Cari nama santri...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF00695C)) },
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = filterDate,
                onValueChange = onFilterDateChange,
                label = { Text("Format: YYYY-MM-DD") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = Color(0xFF00695C)) },
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Laporan Masuk (${filteredReports.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            // Date selector helpers (Hari Ini vs Kemarin)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val jakartaTimeZone = TimeZone.getTimeZone("Asia/Jakarta")
                val helperSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = jakartaTimeZone
                }
                val todayStr = helperSdf.format(Date())
                val yesterdayStr = helperSdf.format(Calendar.getInstance(jakartaTimeZone).apply { add(Calendar.DAY_OF_YEAR, -1) }.time)

                Button(
                    onClick = { onFilterDateChange(todayStr) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterDate == todayStr) Color(0xFF00695C) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Hari Ini", fontSize = 10.sp, color = if (filterDate == todayStr) Color.White else Color.DarkGray)
                }

                Button(
                    onClick = { onFilterDateChange(yesterdayStr) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterDate == yesterdayStr) Color(0xFF00695C) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Kemarin", fontSize = 10.sp, color = if (filterDate == yesterdayStr) Color.White else Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📬", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Belum ada laporan dari orang tua santri\npada tanggal yang dipilih.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReports) { r ->
                    TeacherReportDetailsCard(report = r)
                }
            }
        }
    }
}

// Data class to support evaluation in Teacher Portal
data class ReportEvaluation(
    val isSempurna: Boolean,
    val isHaid: Boolean,
    val temuanList: List<String>,
    val details: String
)

fun evaluateReport(r: Report?): ReportEvaluation {
    if (r == null) {
        return ReportEvaluation(
            isSempurna = false,
            isHaid = false,
            temuanList = listOf("Data laporan kosong"),
            details = "Error: Laporan kosong"
        )
    }

    val getMinutesFromTime = { timeStr: String? ->
        if (timeStr != null && timeStr != "Tidak Shalat" && timeStr.isNotEmpty()) {
            try {
                val clean = timeStr.substringBefore(" ").trim()
                val parts = clean.split(":")
                if (parts.size >= 2) {
                    val hour = parts[0].toIntOrNull()
                    val minute = parts[1].toIntOrNull()
                    if (hour != null && minute != null) {
                        hour * 60 + minute
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    return try {
        val temuan = mutableListOf<String>()
        val isHaid = r.isHaid ?: false
        
        if (isHaid) {
            // Only Zikir and Bakti Ortu are checked during Haid
            val missingZikir = r.zikirBacaan.isNullOrEmpty() || (r.zikirJumlah ?: 0) == 0
            if (missingZikir) {
                temuan.add("Tidak Zikir Harian")
            }
            val missingBakti = r.baktiJenis.isNullOrEmpty()
            if (missingBakti) {
                temuan.add("Tidak Bakti Orang Tua")
            }
            
            val isSempurna = temuan.isEmpty()
            val details = if (isSempurna) {
                "Ibadah Haid Sempurna (Zikir & Bakti Lengkap)"
            } else {
                "Halangan Haid, tetapi: ${temuan.joinToString(", ")}"
            }
            
            ReportEvaluation(
                isSempurna = isSempurna,
                isHaid = true,
                temuanList = temuan,
                details = details
            )
        } else {
            // Standard evaluation: all 5 prayers, zikir, quran/iqro, bakti are required
            if (r.subuhWaktu == null || r.subuhWaktu == "Tidak Shalat") temuan.add("Tidak Shalat Subuh")
            if (r.zuhurWaktu == null || r.zuhurWaktu == "Tidak Shalat") temuan.add("Tidak Shalat Zuhur")
            if (r.asharWaktu == null || r.asharWaktu == "Tidak Shalat") temuan.add("Tidak Shalat Ashar")
            if (r.maghribWaktu == null || r.maghribWaktu == "Tidak Shalat") temuan.add("Tidak Shalat Maghrib")
            if (r.isyaWaktu == null || r.isyaWaktu == "Tidak Shalat") temuan.add("Tidak Shalat Isya")
            
            // Check late/out-of-bounds prayer times
            val subuhMin = getMinutesFromTime(r.subuhWaktu)
            if (subuhMin != null) {
                if (subuhMin < 240) { // Before 04:00 AM
                    temuan.add("Shalat Subuh terlalu awal (${r.subuhWaktu})")
                } else if (subuhMin > 375) { // After 06:15 AM (Sunrise limit)
                    if (subuhMin >= 705) { // After 11:45 AM
                        temuan.add("Shalat Subuh dilaksanakan di waktu Zuhur (${r.subuhWaktu})")
                    } else {
                        temuan.add("Shalat Subuh kesiangan/di luar waktu (${r.subuhWaktu})")
                    }
                }
            }
            
            val zuhurMin = getMinutesFromTime(r.zuhurWaktu)
            if (zuhurMin != null) {
                if (zuhurMin < 705) { // Before 11:45 AM
                    temuan.add("Shalat Zuhur terlalu awal (${r.zuhurWaktu})")
                } else if (zuhurMin >= 900) { // After 15:00 PM (Ashar starts)
                    if (zuhurMin >= 1140) { // After 19:00 PM
                        temuan.add("Shalat Zuhur dilaksanakan di waktu Isya (${r.zuhurWaktu})")
                    } else if (zuhurMin >= 1065) { // After 17:45 PM
                        temuan.add("Shalat Zuhur dilaksanakan di waktu Maghrib (${r.zuhurWaktu})")
                    } else {
                        temuan.add("Shalat Zuhur dilaksanakan di waktu Ashar (${r.zuhurWaktu})")
                    }
                }
            }
            
            val asharMin = getMinutesFromTime(r.asharWaktu)
            if (asharMin != null) {
                if (asharMin < 900) { // Before 15:00 PM
                    temuan.add("Shalat Ashar terlalu awal (${r.asharWaktu})")
                } else if (asharMin >= 1065) { // After 17:45 PM (Maghrib starts)
                    if (asharMin >= 1140) { // After 19:00 PM
                        temuan.add("Shalat Ashar dilaksanakan di waktu Isya (${r.asharWaktu})")
                    } else {
                        temuan.add("Shalat Ashar dilaksanakan di waktu Maghrib (${r.asharWaktu})")
                    }
                }
            }
            
            val maghribMin = getMinutesFromTime(r.maghribWaktu)
            if (maghribMin != null) {
                if (maghribMin < 1065) { // Before 17:45 PM
                    temuan.add("Shalat Maghrib terlalu awal (${r.maghribWaktu})")
                } else if (maghribMin >= 1140) { // After 19:00 PM (Isya starts)
                    temuan.add("Shalat Maghrib dilaksanakan di waktu Isya (${r.maghribWaktu})")
                }
            }
            
            val isyaMin = getMinutesFromTime(r.isyaWaktu)
            if (isyaMin != null) {
                if (isyaMin in 240..1139) { // 04:00 AM to 18:59 PM (daytime)
                    temuan.add("Shalat Isya dilaksanakan di luar waktu (${r.isyaWaktu})")
                }
            }
            
            val missingQuran = r.quranSurat.isNullOrEmpty() && r.iqroJilid.isNullOrEmpty()
            if (missingQuran) {
                temuan.add("Tidak Mengaji")
            }
            
            val missingZikir = r.zikirBacaan.isNullOrEmpty() || (r.zikirJumlah ?: 0) == 0
            if (missingZikir) {
                temuan.add("Tidak Zikir Harian")
            }
            
            val missingBakti = r.baktiJenis.isNullOrEmpty()
            if (missingBakti) {
                temuan.add("Tidak Bakti Orang Tua")
            }
            
            val isSempurna = temuan.isEmpty()
            val details = if (isSempurna) {
                "Sempurna Ibadah"
            } else {
                "Ada Temuan: ${temuan.joinToString(", ")}"
            }
            
            ReportEvaluation(
                isSempurna = isSempurna,
                isHaid = false,
                temuanList = temuan,
                details = details
            )
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        ReportEvaluation(
            isSempurna = false,
            isHaid = false,
            temuanList = listOf("Error Evaluasi: ${e.localizedMessage ?: "Unknown"}"),
            details = "Error: ${e.message ?: "Unknown"}"
        )
    }
}

@Composable
fun GuruRekapLaporanTab(
    reports: List<Report>?,
    filterDate: String,
    onFilterDateChange: (String) -> Unit
) {
    val dailyReports = (reports ?: emptyList()).filter { it != null && (it.date ?: "") == filterDate }
    
    // Calculate statistics
    val totalReports = dailyReports.size
    val evaluated = dailyReports.map { it to evaluateReport(it) }
    
    val sempurnaCount = evaluated.count { it.second != null && it.second.isSempurna }
    val temuanCount = evaluated.count { it.second != null && !it.second.isSempurna && !it.second.isHaid }
    val haidCount = evaluated.count { it.second != null && it.second.isHaid }
    
    // Filter selection state for list: 0 = Semua, 1 = Sempurna, 2 = Ada Temuan, 3 = Sedang Haid
    var selectedFilter by remember { mutableIntStateOf(0) }
    var expandedReportId by remember(filterDate, selectedFilter) { mutableStateOf<Int?>(null) }
    
    val displayedEvaluations = when (selectedFilter) {
        1 -> evaluated.filter { it.second != null && it.second.isSempurna }
        2 -> evaluated.filter { it.second != null && !it.second.isSempurna && !it.second.isHaid }
        3 -> evaluated.filter { it.second != null && it.second.isHaid }
        else -> evaluated
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Date selector and helpers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterDate,
                onValueChange = onFilterDateChange,
                label = { Text("Format: YYYY-MM-DD") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = Color(0xFF00695C)) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            val todayStr = remember {
                try {
                    val helperSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                    }
                    helperSdf.format(Date())
                } catch (e: Exception) {
                    ""
                }
            }
            
            val yesterdayStr = remember {
                try {
                    val helperSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                    }
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"))
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                    helperSdf.format(cal.time)
                } catch (e: Exception) {
                    ""
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onFilterDateChange(todayStr) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterDate == todayStr) Color(0xFF00695C) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Hari Ini", fontSize = 10.sp, color = if (filterDate == todayStr) Color.White else Color.DarkGray)
                }

                Button(
                    onClick = { onFilterDateChange(yesterdayStr) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterDate == yesterdayStr) Color(0xFF00695C) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Kemarin", fontSize = 10.sp, color = if (filterDate == yesterdayStr) Color.White else Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid/Summary cards of stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Total Laporan
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📬 Total", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("$totalReports", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Sempurna
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🟢 Sempurna", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("$sempurnaCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }
            }

            // Temuan
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔴 Temuan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    Text("$temuanCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                }
            }

            // Haid
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                border = BorderStroke(1.dp, Color(0xFFCE93D8)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🌸 Haid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                    Text("$haidCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf("Semua", "Sempurna", "Temuan", "Haid")
            filters.forEachIndexed { idx, label ->
                val isSelected = selectedFilter == idx
                val chipBg = when (idx) {
                    1 -> if (isSelected) Color(0xFF2E7D32) else Color(0xFFE8F5E9)
                    2 -> if (isSelected) Color(0xFFC62828) else Color(0xFFFFEBEE)
                    3 -> if (isSelected) Color(0xFF6A1B9A) else Color(0xFFF3E5F5)
                    else -> if (isSelected) Color(0xFF37474F) else Color(0xFFECEFF1)
                }
                val chipText = when (idx) {
                    1 -> if (isSelected) Color.White else Color(0xFF2E7D32)
                    2 -> if (isSelected) Color.White else Color(0xFFC62828)
                    3 -> if (isSelected) Color.White else Color(0xFF6A1B9A)
                    else -> if (isSelected) Color.White else Color(0xFF37474F)
                }
                
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilter = idx },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = chipBg)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = chipText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // List of evaluations
        if (displayedEvaluations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada laporan yang sesuai filter.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayedEvaluations) { pair ->
                    if (pair != null) {
                        val report = pair.first
                        val eval = pair.second
                        if (report != null && eval != null) {
                            val isExpanded = expandedReportId == report.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedReportId = if (isExpanded) null else report.id
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header: Santri Name and Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = report.santriName ?: "Santri",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.DarkGray
                                        )
                                        
                                        val badgeBg = when {
                                            eval.isHaid -> Color(0xFFF3E5F5)
                                            eval.isSempurna -> Color(0xFFE8F5E9)
                                            else -> Color(0xFFFFEBEE)
                                        }
                                        val badgeTextColor = when {
                                            eval.isHaid -> Color(0xFF6A1B9A)
                                            eval.isSempurna -> Color(0xFF2E7D32)
                                            else -> Color(0xFFC62828)
                                        }
                                        val badgeLabel = when {
                                            eval.isHaid -> "🌸 Haid"
                                            eval.isSempurna -> "🟢 Sempurna"
                                            else -> "🔴 Ada Temuan"
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Card(
                                                shape = RoundedCornerShape(4.dp),
                                                colors = CardDefaults.cardColors(containerColor = badgeBg)
                                            ) {
                                                Text(
                                                    text = badgeLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeTextColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(6.dp))
                                            
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Expand",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Findings / Evaluation details
                                        if (eval.temuanList.isNotEmpty()) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (eval.isHaid) Color(0xFFFBF4FC) else Color(0xFFFFF8F8)
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = "Temuan Evaluasi:",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (eval.isHaid) Color(0xFF6A1B9A) else Color(0xFFC62828)
                                                    )
                                                    eval.temuanList.forEach { temuan ->
                                                        Text(
                                                            text = "• $temuan",
                                                            fontSize = 10.sp,
                                                            color = if (eval.isHaid) Color(0xFF4A148C) else Color(0xFFB71C1C)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("🏆", fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (eval.isHaid) "Ibadah halangan diselesaikan dengan sempurna!" else "Sempurna! Semua ibadah harian terlaksana.",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF33691E)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Footer: parent verification and timestamp
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Penanggung Jawab (Ortu):",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                                val parentNameText = report.parentName ?: ""
                                                Text(
                                                    text = if (parentNameText.isNotEmpty()) parentNameText else "Belum ditandatangani",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.DarkGray
                                                )
                                            }
                                            
                                            val timeFormat = try {
                                                SimpleDateFormat("HH:mm WIB", Locale.getDefault()).apply {
                                                    timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                                                }
                                            } catch (e: Throwable) {
                                                null
                                            }
                                            val reportTimestamp = report.timestamp ?: 0L
                                            val formattedTime = if (timeFormat != null && reportTimestamp > 0L) {
                                                try {
                                                    timeFormat.format(Date(reportTimestamp))
                                                } catch (e: Throwable) {
                                                    "-"
                                                }
                                            } else {
                                                "-"
                                            }
                                            Text(
                                                text = "Dilaporkan: $formattedTime",
                                                fontSize = 9.sp,
                                                color = Color.Gray
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
    }
}

@Composable
fun TeacherReportDetailsCard(report: Report) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.santriName ?: "Santri",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF00695C)
                    )
                    Text(
                        text = "Tanggal: ${report.date ?: ""}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val count = listOf(
                        report.subuhWaktu, report.zuhurWaktu, report.asharWaktu,
                        report.maghribWaktu, report.isyaWaktu, report.tahajudWaktu,
                        report.witirWaktu, report.zikirBacaan,
                        if (report.quranSurat != null || report.iqroJilid != null) "mengaji" else null,
                        report.baktiJenis,
                        if (report.parentSignature != null) "signed" else null
                    ).filterNotNull().size

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0F2F1), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$count/11 Kegiatan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00695C)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.Gray
                    )
                }
            }

            // Expanded view containing all details, compressed pictures, signatures
            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    
                    // 1. Shalat 5 Waktu Details
                    Text("🕌 Shalat Fardhu 5 Waktu:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF00695C))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorshipActivityTinyStatus(name = "Maghrib", isDone = report.maghribWaktu != null, info = report.maghribWaktu, photo = report.maghribFoto, color = Color(0xFFD84315), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                        WorshipActivityTinyStatus(name = "Isya", isDone = report.isyaWaktu != null, info = report.isyaWaktu, photo = report.isyaFoto, color = Color(0xFF283593), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                        WorshipActivityTinyStatus(name = "Subuh", isDone = report.subuhWaktu != null, info = report.subuhWaktu, photo = report.subuhFoto, color = Color(0xFF00838F), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                        WorshipActivityTinyStatus(name = "Zuhur", isDone = report.zuhurWaktu != null, info = report.zuhurWaktu, photo = report.zuhurFoto, color = Color(0xFFF57F17), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                        WorshipActivityTinyStatus(name = "Ashar", isDone = report.asharWaktu != null, info = report.asharWaktu, photo = report.asharFoto, color = Color(0xFFEF6C00), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                    }

                    // Missed prayers reasons
                    val missedPrayers = mutableListOf<Pair<String, String>>()
                    if (report.maghribWaktu == "Tidak Shalat") missedPrayers.add("Maghrib" to (report.maghribCara ?: "Sakit"))
                    if (report.isyaWaktu == "Tidak Shalat") missedPrayers.add("Isya" to (report.isyaCara ?: "Sakit"))
                    if (report.subuhWaktu == "Tidak Shalat") missedPrayers.add("Subuh" to (report.subuhCara ?: "Sakit"))
                    if (report.zuhurWaktu == "Tidak Shalat") missedPrayers.add("Zuhur" to (report.zuhurCara ?: "Sakit"))
                    if (report.asharWaktu == "Tidak Shalat") missedPrayers.add("Ashar" to (report.asharCara ?: "Sakit"))
                    
                    if (missedPrayers.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "⚠️ Alasan Berhalangan Shalat:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                missedPrayers.forEach { (name, reason) ->
                                    val reasonClean = reason.removePrefix("Tidak Shalat: ")
                                    Text(
                                        text = "• Shalat $name: $reasonClean",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB71C1C),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 2. Ibadah Sunnah Details
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("✨ Ibadah Sunnah & Bacaan:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF6A1B9A))
                    
                    // Tahajud & Witir Status Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorshipActivityTinyStatus(name = "Tahajud", isDone = report.tahajudWaktu != null, info = report.tahajudWaktu, photo = report.tahajudFoto, color = Color(0xFF6A1B9A), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                        WorshipActivityTinyStatus(name = "Witir", isDone = report.witirWaktu != null, info = report.witirWaktu, photo = report.witirFoto, color = Color(0xFF4527A0), isHaid = report.isHaid == true, modifier = Modifier.weight(1f))
                    }

                    // Zikir & Mengaji
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "📿 Zikir: " + (if (report.zikirBacaan != null) {
                                    if (report.zikirBacaan == "Tidak Melaksanakan") "Tidak Melaksanakan ❌"
                                    else "${report.zikirBacaan} (${report.zikirJumlah}x)"
                                } else "Belum dilaporkan"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (report.zikirBacaan == "Tidak Melaksanakan") Color(0xFFC62828) else Color.Unspecified
                            )
                            Text(
                                text = "📖 Mengaji: " + (when {
                                    report.quranSurat != null -> "Al-Qur'an (Surat ${report.quranSurat} Ayat ${report.quranAyat})"
                                    report.iqroJilid != null -> "Iqro (Jilid ${report.iqroJilid} Halaman ${report.iqroHalaman})"
                                    else -> "Belum dilaporkan"
                                }),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 3. Bakti Ortu Details
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("❤️ Bakti Orang Tua:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC2185B))
                    if (report.baktiJenis != null) {
                        val isTidakMelaksanakanBakti = report.baktiJenis == "Tidak Melaksanakan"
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTidakMelaksanakanBakti) Color(0xFFFFEBEE) else Color(0xFFFCE4EC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isTidakMelaksanakanBakti) "Tidak Melaksanakan ❌" else report.baktiJenis!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTidakMelaksanakanBakti) Color(0xFFC62828) else Color(0xFF880E4F),
                                    modifier = Modifier.weight(1f)
                                )
                                if (report.baktiFoto != null) {
                                    val bitmap = remember(report.baktiFoto) {
                                        try {
                                            val bytes = Base64.decode(report.baktiFoto, Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        } catch (e: Exception) { null }
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Bakti Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Belum dilaporkan", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    }

                    // 4. Verification & Signature display
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("✍️ Verifikasi & Tanda Tangan Orang Tua:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF37474F))
                    
                    if (report.parentSignature != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Nama Ortu/Wali:", fontSize = 10.sp, color = Color.Gray)
                                    Text(report.parentName ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
                                    Text("Tanda Tangan Terlampir", fontSize = 9.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                                }

                                val sigBitmap = remember(report.parentSignature) {
                                    try {
                                        val bytes = Base64.decode(report.parentSignature, Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (e: Exception) { null }
                                }
                                if (sigBitmap != null) {
                                    Image(
                                        bitmap = sigBitmap.asImageBitmap(),
                                        contentDescription = "Signature",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(width = 80.dp, height = 40.dp)
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Belum ditandatangani oleh orang tua.", fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorshipActivityTinyStatus(
    name: String,
    isDone: Boolean,
    info: String?,
    photo: String?,
    color: Color,
    isHaid: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isTidakShalat = info == "Tidak Shalat"
    val finalCardColor = if (isTidakShalat) {
        Color(0xFFFFEBEE)
    } else if (isHaid && !isDone) {
        Color(0xFFF3E5F5)
    } else if (isDone) {
        color.copy(alpha = 0.08f)
    } else {
        Color(0xFFEEEEEE)
    }

    val finalBorderColor = if (isTidakShalat) {
        Color(0xFFD32F2F)
    } else if (isHaid && !isDone) {
        Color(0xFFBA68C8)
    } else if (isDone) {
        color
    } else {
        Color.LightGray
    }

    val finalTextColor = if (isTidakShalat) {
        Color(0xFFC62828)
    } else if (isHaid && !isDone) {
        Color(0xFF6A1B9A)
    } else if (isDone) {
        color
    } else {
        Color.Gray
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = finalCardColor),
        border = BorderStroke(1.dp, finalBorderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = finalTextColor,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            if (isTidakShalat) {
                Text(
                    text = "Absen",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            } else if (isDone && photo != null) {
                val bitmap = remember(photo) {
                    try {
                        val bytes = Base64.decode(photo, Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            } else {
                Text(
                    text = if (isDone) "Selesai" else if (isHaid) "Haid" else "❌",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) color else if (isHaid) Color(0xFF6A1B9A) else Color.Red
                )
            }
        }
    }
}

// 5b. Kelola Santri Tab Composable (Add, View, Delete, Edit PIN for Santri)
@Composable
fun GuruKelolaSantriTab(
    santriList: List<Santri>,
    onAddSantri: (String, String, String) -> Unit,
    onDeleteSantri: (Santri) -> Unit,
    onUpdateSantri: (Int, String, String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var genderInput by remember { mutableStateOf("Perempuan") } // Default value
    val context = LocalContext.current
 
    // Edit states
    var editingSantri by remember { mutableStateOf<Santri?>(null) }
    var editNameInput by remember { mutableStateOf("") }
    var editPinInput by remember { mutableStateOf("") }
    var editGenderInput by remember { mutableStateOf("Perempuan") }
 
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        
        if (editingSantri != null) {
            // Edit Santri form
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                border = BorderStroke(2.dp, Color(0xFF00695C))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ubah Data Santri:", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                    
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
 
                    OutlinedTextField(
                        value = editPinInput,
                        onValueChange = { editPinInput = it },
                        label = { Text("PIN Orang Tua (4 Digit Angka)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    // Gender selection for Edit
                    Text("Jenis Kelamin:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf("Laki-laki", "Perempuan")
                        options.forEach { option ->
                            val isSelected = editGenderInput == option
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { editGenderInput = option },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF00695C) else Color(0xFFECEFF1)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { editingSantri = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
 
                        Button(
                            onClick = {
                                val s = editingSantri!!
                                if (editNameInput.isNotBlank() && editPinInput.length == 4) {
                                    onUpdateSantri(s.id, editNameInput, editPinInput, editGenderInput)
                                    editingSantri = null
                                    Toast.makeText(context, "Data berhasil diubah!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PIN harus 4 digit!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Simpan Perubahan")
                        }
                    }
                }
            }
        } else {
            // Add Santri card form
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tambah Santri Baru Masjid Al-Hidayah:", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                    
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap Santri") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
 
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN Orang Tua (4 Angka)") },
                        leadingIcon = { Icon(Icons.Default.Password, contentDescription = "PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gender selection for Add
                    Text("Jenis Kelamin:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf("Laki-laki", "Perempuan")
                        options.forEach { option ->
                            val isSelected = genderInput == option
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { genderInput = option },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF00695C) else Color(0xFFECEFF1)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
 
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && pinInput.length == 4) {
                                onAddSantri(nameInput, pinInput, genderInput)
                                nameInput = ""
                                pinInput = ""
                                // reset to default option
                                genderInput = "Perempuan"
                                Toast.makeText(context, "Santri berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lengkapi nama & PIN harus 4 digit angka!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TAMBAH SANTRI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
 
        // List of Santri
        Text("Daftar Santri Terdaftar (${santriList.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
 
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(santriList) { s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(s.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val genderIcon = if (s.gender == "Perempuan") "🌸" else "👦"
                                Text("$genderIcon ${s.gender}", fontSize = 12.sp, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = "PIN", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PIN Orang Tua: ${s.pin}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
 
                        Row {
                            IconButton(
                                onClick = {
                                    editingSantri = s
                                    editNameInput = s.name
                                    editPinInput = s.pin
                                    editGenderInput = s.gender
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF00695C))
                            }
 
                            IconButton(
                                onClick = {
                                    onDeleteSantri(s)
                                    Toast.makeText(context, "Santri dihapus!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5c. Atur PIN & Firebase Tab Composable
@Composable
fun GuruAturPinTab(
    appConfig: com.example.data.AppConfig,
    viewModel: com.example.ui.viewmodel.AlHidayahViewModel,
    onSavePins: (String, String, String) -> Unit,
    onSaveFirebase: (String, Boolean) -> Unit
) {
    var guruPinInput by remember { mutableStateOf(appConfig.guruPin) }
    var admin1PinInput by remember { mutableStateOf(appConfig.admin1Pin) }
    var admin2PinInput by remember { mutableStateOf(appConfig.admin2Pin) }
    
    // Firebase states
    var firebaseUrlInput by remember { 
        mutableStateOf(
            if (appConfig.firebaseUrl.isEmpty()) com.example.data.FirebaseSyncManager.DEFAULT_FIREBASE_URL else appConfig.firebaseUrl
        ) 
    }
    var firebaseEnabledInput by remember { 
        mutableStateOf(
            if (appConfig.firebaseUrl.isEmpty()) com.example.data.FirebaseSyncManager.DEFAULT_FIREBASE_ENABLED else appConfig.firebaseEnabled
        ) 
    }
    
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF00695C).copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔑", fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text("Pengaturan PIN Pengaman", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                    Text("Sebagai Guru, Anda berhak mengatur seluruh PIN pengaman portal.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Ubah PIN Keamanan Portal:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        OutlinedTextField(
            value = guruPinInput,
            onValueChange = { guruPinInput = it },
            label = { Text("PIN Guru Ngaji (4 Digit)") },
            leadingIcon = { Icon(Icons.Default.Security, contentDescription = "Guru PIN", tint = Color(0xFF00695C)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = admin1PinInput,
            onValueChange = { admin1PinInput = it },
            label = { Text("PIN Admin 1 (4 Digit)") },
            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin 1 PIN", tint = Color(0xFF283593)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = admin2PinInput,
            onValueChange = { admin2PinInput = it },
            label = { Text("PIN Admin 2 (4 Digit)") },
            leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin 2 PIN", tint = Color(0xFFEF6C00)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                if (guruPinInput.length == 4 && admin1PinInput.length == 4 && admin2PinInput.length == 4) {
                    onSavePins(guruPinInput, admin1PinInput, admin2PinInput)
                    Toast.makeText(context, "Semua PIN berhasil disimpan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Kesalahan: Setiap PIN harus terdiri dari 4 digit angka!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = "Save")
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIMPAN SEMUA PIN PORTAL", fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Section 2: Firebase Realtime Database Config
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF6C00).copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text("Firebase Realtime Database", fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00))
                    Text("Hubungkan aplikasi ini ke database Firebase Anda sendiri untuk sinkronisasi data antar perangkat.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Aktifkan Sinkronisasi Firebase", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Kirim/unduh data secara otomatis ke Firebase", fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = firebaseEnabledInput,
                onCheckedChange = { firebaseEnabledInput = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEF6C00), checkedTrackColor = Color(0xFFEF6C00).copy(alpha = 0.5f))
            )
        }

        OutlinedTextField(
            value = firebaseUrlInput,
            onValueChange = { firebaseUrlInput = it },
            label = { Text("URL Firebase Realtime Database") },
            placeholder = { Text("https://nama-project-default-rtdb.firebaseio.com/") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Firebase URL", tint = Color(0xFFEF6C00)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val cleanUrl = firebaseUrlInput.trim()
                if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("https://")) {
                    Toast.makeText(context, "URL Firebase harus diawali dengan https://", Toast.LENGTH_LONG).show()
                } else {
                    onSaveFirebase(cleanUrl, firebaseEnabledInput)
                    Toast.makeText(context, "Konfigurasi Firebase berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.Default.Cloud, contentDescription = "Cloud")
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIMPAN KONFIGURASI FIREBASE", fontWeight = FontWeight.Bold)
        }

        if (appConfig.firebaseUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText(
                        "Al-Hidayah Database URL",
                        """
                        *KONEKSI APLIKASI AL-HIDAYAH HUB*
                        
                        Bapak/Ibu Wali Santri, silakan hubungkan aplikasi Al-Hidayah Anda ke database pusat dengan menyalin link di bawah ini:
                        
                        URL Database:
                        ${appConfig.firebaseUrl}
                        
                        Langkah-langkah:
                        1. Buka aplikasi Al-Hidayah Hub di HP Anda.
                        2. Di halaman awal, klik tombol "Hubungkan 🔗" di bagian kanan atas bar koneksi.
                        3. Tempelkan/paste URL di atas ke dalam kolom yang tersedia.
                        4. Klik "Simpan & Sinkronkan".
                        5. Selesai! Data nama anak, PIN, dan laporan akan terhubung langsung ke Ustaz secara realtime.
                        """.trimIndent()
                    )
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Petunjuk & URL berhasil disalin! Silakan bagikan ke WhatsApp Orang Tua Santri.", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Config", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SALIN PETUNJUK & URL WALI SANTRI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Aksi Sinkronisasi Manual:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Card(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Gunakan aksi di bawah untuk melakukan backup data ke Firebase atau restore data ke perangkat baru ini.",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.triggerFullUpload { success ->
                                    if (success) {
                                        Toast.makeText(context, "Berhasil mengunggah semua data lokal ke Firebase!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Gagal mengunggah data. Silakan periksa URL dan koneksi Anda.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Backup Ke Firebase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.triggerFullDownload { success ->
                                    if (success) {
                                        Toast.makeText(context, "Berhasil mengunduh dan menyinkronkan data dari Firebase!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Gagal mengunduh data. Silakan periksa URL dan aturan kemanan (Security Rules) Firebase Anda.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Restore Dari Firebase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isSyncing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFEF6C00),
                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                            )
                            Text(syncProgress, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF6C00))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.06f)),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("Zona Bahaya (Pembersihan Data)", fontWeight = FontWeight.Bold, color = Color.Red)
                }
                
                Text(
                    "Gunakan fitur ini secara berkala untuk menghapus laporan mingguan, laporan harian, dan absensi lama agar kuota database Firebase Realtime gratisan Anda tidak penuh (kuota gratis terbatas 1GB).",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                
                Button(
                    onClick = { showDeleteConfirmationDialog = true },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Data", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HAPUS SEMUA LAPORAN & ABSENSI", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    Text("Konfirmasi Hapus Data", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tindakan ini akan menghapus data berikut secara permanen:",
                        fontWeight = FontWeight.Medium
                    )
                    Text("• Seluruh Laporan Harian Santri (termasuk foto-foto)", fontSize = 13.sp)
                    Text("• Seluruh Catatan Absensi", fontSize = 13.sp)
                    Text("• Seluruh Laporan Evaluasi Mingguan/Bulanan", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Data tersebut akan dihapus secara permanen baik di HP ini maupun di Firebase Realtime Database Anda (jika terhubung). Ini sangat berguna untuk mengosongkan kuota Firebase Gratisan Anda.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Catatan: Data profil Santri dan Periode Evaluasi TIDAK akan dihapus, sehingga Anda tidak perlu mendaftarkan santri ulang.",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        viewModel.triggerClearWeeklyAndDailyData { success ->
                            if (success) {
                                Toast.makeText(context, "Pembersihan data berhasil dilakukan!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Pembersihan data lokal berhasil, namun gagal menghapus di Firebase. Silakan periksa koneksi internet Anda.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Ya, Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmationDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

// 6. Admin Portals (Admin 1 & Admin 2 Dashboards)
fun changeDateByDays(dateStr: String, days: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val date = sdf.parse(dateStr) ?: Date()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
            time = date
            add(Calendar.DAY_OF_YEAR, days)
        }
        sdf.format(cal.time)
    } catch (e: Exception) {
        dateStr
    }
}

fun formatDisplayDate(dateStr: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val date = sdfInput.parse(dateStr) ?: return dateStr
        val sdfOutput = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        sdfOutput.format(date)
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun AdminPortalScreen(adminNumber: Int, viewModel: AlHidayahViewModel) {
    val santriList by viewModel.santriList.collectAsStateWithLifecycle()
    val reports by viewModel.reportList.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDateString.collectAsStateWithLifecycle()
    val attendanceList by viewModel.attendanceList.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var activeTab by remember { mutableStateOf(0) } // 0 = Ringkasan, 1 = Absensi, 2 = Kelola Santri
    
    val accentColor = if (adminNumber == 1) Color(0xFF283593) else Color(0xFFEF6C00)
    val containerColor = if (adminNumber == 1) Color(0xFFE8EAF6) else Color(0xFFFFF3E0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F8))
    ) {
        // Admin header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Masjid Al-Hidayah Kp. Ciloa",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Portal Admin $adminNumber ⚙️",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.White,
            contentColor = accentColor
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Ringkasan", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Ringkasan") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Absensi", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Absensi") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Kelola Santri", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Kelola Santri") }
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text("Laporan Mingguan", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Assignment, contentDescription = "Laporan Mingguan") }
            )
        }

        when (activeTab) {
            0 -> {
                // Tab Ringkasan
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Blocks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Total Santri", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Text("${santriList.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Laporan Terkumpul", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                            Text("${reports.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                }

                // Info Notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
                        Column {
                            Text(
                                text = "Catatan Pengembang:",
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Text(
                                text = "Fitur manajemen data, ekspor laporan, dan audit tingkat lanjut untuk Admin $adminNumber akan dikonfigurasi pada tahap pengembangan selanjutnya sesuai hasil diskusi dengan Guru.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                // Quick list of active students
                Text("Daftar Santri Aktif (Ringkasan):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(santriList) { s ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(containerColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(s.name.take(1), fontWeight = FontWeight.Bold, color = accentColor)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
            }
            1 -> {
                // Tab Absensi
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date switcher row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val prev = changeDateByDays(selectedDate, -1)
                            viewModel.setDate(prev)
                        }
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Kemarin", tint = accentColor)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDisplayDate(selectedDate),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                        val sdfToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
                        }
                        val todayStr = sdfToday.format(Date())
                        if (selectedDate == todayStr) {
                            Text(
                                text = "Hari Ini",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val next = changeDateByDays(selectedDate, 1)
                            viewModel.setDate(next)
                        }
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Besok", tint = accentColor)
                    }
                }

                // Set All to Hadir button
                Button(
                    onClick = {
                        val attendancesToSave = santriList.map { s ->
                            com.example.data.Attendance(
                                santriId = s.id,
                                santriName = s.name,
                                date = selectedDate,
                                status = "HADIR"
                            )
                        }
                        viewModel.saveAllAttendance(attendancesToSave)
                        Toast.makeText(context, "Semua santri disetel HADIR", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Setel Semua Hadir")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SETEL SEMUA HADIR HARI INI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // List of Santri for Attendance
                Text("Status Absensi Santri:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(santriList) { s ->
                        val existingAtt = attendanceList.find { it.santriId == s.id }
                        val currentStatus = existingAtt?.status ?: "HADIR" // default is HADIR

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(containerColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(s.name.take(1), fontWeight = FontWeight.Bold, color = accentColor)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Row of status selectors: HADIR, SAKIT, IZIN, HAID (if female), ALFA
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val statuses = if (s.gender == "Perempuan") {
                                        listOf("HADIR", "SAKIT", "IZIN", "HAID", "ALFA")
                                    } else {
                                        listOf("HADIR", "SAKIT", "IZIN", "ALFA")
                                    }
                                    statuses.forEach { status ->
                                        val isSelected = currentStatus == status
                                        val buttonColor = when (status) {
                                            "HADIR" -> Color(0xFF4CAF50)
                                            "SAKIT" -> Color(0xFFFF9800)
                                            "IZIN" -> Color(0xFF2196F3)
                                            "HAID" -> Color(0xFF9C27B0) // Purple for HAID
                                            else -> Color(0xFFF44336) // ALFA
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) buttonColor else Color(0xFFEEEEEE))
                                                .clickable {
                                                    viewModel.saveAttendance(s.id, s.name, selectedDate, status)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = status,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else Color.Gray
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
            2 -> {
                GuruKelolaSantriTab(
                    santriList = santriList,
                    onAddSantri = { name, pin, gender -> viewModel.addNewSantri(name, pin, gender) },
                    onDeleteSantri = { viewModel.deleteSantri(it) },
                    onUpdateSantri = { id, name, pin, gender -> viewModel.updateSantri(id, name, pin, gender) }
                )
            }
            3 -> {
                com.example.ui.components.GuruEvaluasiMingguanTab(
                    viewModel = viewModel,
                    userRole = if (adminNumber == 1) com.example.ui.viewmodel.Role.ADMIN1 else com.example.ui.viewmodel.Role.ADMIN2
                )
            }
        }
    }
}
