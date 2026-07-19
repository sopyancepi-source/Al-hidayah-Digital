package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShalatForm(
    title: String,
    primaryColor: Color,
    initialWaktu: String?,
    initialFoto: String?,
    initialCara: String?,
    onSave: (String, String?, String) -> Unit,
    onClose: () -> Unit
) {
    var waktu by remember {
        mutableStateOf(
            if (initialWaktu != "Tidak Shalat") {
                initialWaktu ?: SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                }.format(Date()) + " WIB"
            } else {
                SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
                }.format(Date()) + " WIB"
            }
        )
    }
    var fotoBase64 by remember { mutableStateOf(if (initialWaktu != "Tidak Shalat") initialFoto else null) }
    var caraShalat by remember { mutableStateOf(if (initialWaktu != "Tidak Shalat") (initialCara ?: "Berjamaah") else "Berjamaah") }
    var isCameraOpen by remember { mutableStateOf(false) }

    var statusMelaksanakan by remember {
        mutableStateOf(if (initialWaktu == "Tidak Shalat") "Tidak Melaksanakan" else "Melaksanakan")
    }

    val reasons = listOf("Sakit", "Halangan / Haid", "Tertidur / Lupa", "Safar (Bepergian)", "Belum Diwajibkan (Belum)", "Lainnya")
    var selectedReasonOption by remember {
        mutableStateOf(
            if (initialWaktu == "Tidak Shalat") {
                val cleanedCara = initialCara?.removePrefix("Tidak Shalat: ") ?: "Sakit"
                if (reasons.dropLast(1).contains(cleanedCara)) cleanedCara else "Lainnya"
            } else {
                "Sakit"
            }
        )
    }
    var customReasonText by remember {
        mutableStateOf(
            if (initialWaktu == "Tidak Shalat") {
                val cleanedCara = initialCara?.removePrefix("Tidak Shalat: ") ?: ""
                if (reasons.dropLast(1).contains(cleanedCara)) "" else cleanedCara
            } else {
                ""
            }
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Form Header with specific colorful theme
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "FORM SHALAT $title",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = primaryColor
            )
        )

        if (isCameraOpen) {
            // Simulated camera overlay
            SimulatedCameraOverlay(
                activityName = "Shalat $title",
                primaryColor = primaryColor,
                onCaptured = { base64 ->
                    fotoBase64 = base64
                    isCameraOpen = false
                },
                onClose = { isCameraOpen = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🕌",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Laporan Aktivitas Shalat $title",
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Text(
                                text = "Silakan isi detail pelaksanaan shalat atau alasan jika berhalangan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Status Pelaksanaan Switcher
                Text(
                    text = "Status Pelaksanaan Shalat",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val statusOptions = listOf("Melaksanakan", "Tidak Melaksanakan")
                    statusOptions.forEach { option ->
                        val isSelected = statusMelaksanakan == option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { statusMelaksanakan = option },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (statusMelaksanakan == "Melaksanakan") {
                    // 1. Waktu Form
                    Text(
                        text = "1. Waktu Pelaksanaan Shalat",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    OutlinedTextField(
                        value = waktu,
                        onValueChange = { waktu = it },
                        label = { Text("Jam Pelaksanaan (e.g. 18:15 WIB)") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = "Waktu", tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 2. Cara Shalat (Munfarid / Berjamaah)
                    Text(
                        text = "2. Cara Shalat",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val options = listOf("Berjamaah", "Munfarid")
                        options.forEach { option ->
                            val isSelected = caraShalat == option
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { caraShalat = option },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 3. Foto Form (Must be direct photo)
                    Text(
                        text = "3. Foto Pelaksanaan (Wajib Foto Langsung)",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    if (fotoBase64 != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, primaryColor, RoundedCornerShape(12.dp))
                        ) {
                            val bitmap = remember(fotoBase64) {
                                try {
                                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto Shalat",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Gambar Gagal Dimuat", color = Color.White)
                                }
                            }

                            // Remove Photo overlay button
                            IconButton(
                                onClick = { fotoBase64 = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Foto", tint = Color.White)
                            }
                        }
                    } else {
                        Button(
                            onClick = { isCameraOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Buka Kamera")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AMBIL FOTO LANGSUNG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "*Sesuai instruksi guru, pengambilan gambar harus dilakukan langsung menggunakan kamera untuk validitas ibadah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Alasan Tidak Melaksanakan Shalat
                    Text(
                        text = "Alasan Tidak Melaksanakan Shalat",
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    reasons.forEach { reason ->
                        val isReasonSelected = selectedReasonOption == reason
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReasonOption = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isReasonSelected,
                                onClick = { selectedReasonOption = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (selectedReasonOption == "Lainnya") {
                        OutlinedTextField(
                            value = customReasonText,
                            onValueChange = { customReasonText = it },
                            label = { Text("Tuliskan alasan lainnya secara jujur...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Save Button
                val isSaveEnabled = if (statusMelaksanakan == "Melaksanakan") {
                    fotoBase64 != null
                } else {
                    selectedReasonOption != "Lainnya" || customReasonText.isNotBlank()
                }

                Button(
                    onClick = {
                        if (statusMelaksanakan == "Melaksanakan") {
                            onSave(waktu, fotoBase64, caraShalat)
                        } else {
                            val finalReason = if (selectedReasonOption == "Lainnya") customReasonText else selectedReasonOption
                            onSave("Tidak Shalat", null, "Tidak Shalat: $finalReason")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    enabled = isSaveEnabled
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIMPAN LAPORAN SHALAT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranForm(
    initialSurat: String?,
    initialAyat: String?,
    initialJilid: String?,
    initialHalaman: String?,
    onSave: (isQuran: Boolean, field1: String, field2: String) -> Unit,
    onClose: () -> Unit
) {
    var isQuranMode by remember { mutableStateOf(initialSurat != null || initialJilid == null) }
    var surat by remember { mutableStateOf(initialSurat ?: "") }
    var ayat by remember { mutableStateOf(initialAyat ?: "") }
    var jilid by remember { mutableStateOf(initialJilid ?: "") }
    var halaman by remember { mutableStateOf(initialHalaman ?: "") }

    val primaryColor = Color(0x004D40 or -0x1000000) // Deep Teal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "FORM MENGAJI (BACA QUR'AN/IQRO)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color(0xFF00695C)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Select Choice Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isQuranMode = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isQuranMode) Color(0xFF00695C) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Al-Qur'an", fontWeight = FontWeight.Bold, color = if (isQuranMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isQuranMode = false },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isQuranMode) Color(0xFF00695C) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Iqro", fontWeight = FontWeight.Bold, color = if (!isQuranMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (isQuranMode) {
                Text("Detail Mengaji Al-Qur'an", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                OutlinedTextField(
                    value = surat,
                    onValueChange = { surat = it },
                    label = { Text("Nama Surat (e.g. Al-Baqarah)") },
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = "Surat", tint = Color(0xFF00695C)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ayat,
                    onValueChange = { ayat = it },
                    label = { Text("Ayat (e.g. 1-10)") },
                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Ayat", tint = Color(0xFF00695C)) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Detail Mengaji Iqro", fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                OutlinedTextField(
                    value = jilid,
                    onValueChange = { jilid = it },
                    label = { Text("Jilid (e.g. 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.AutoStories, contentDescription = "Jilid", tint = Color(0xFF00695C)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = halaman,
                    onValueChange = { halaman = it },
                    label = { Text("Halaman (e.g. 12)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = "Halaman", tint = Color(0xFF00695C)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isQuranMode) {
                        if (surat.isNotBlank() && ayat.isNotBlank()) {
                            onSave(true, surat, ayat)
                        }
                    } else {
                        if (jilid.isNotBlank() && halaman.isNotBlank()) {
                            onSave(false, jilid, halaman)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                enabled = if (isQuranMode) (surat.isNotBlank() && ayat.isNotBlank()) else (jilid.isNotBlank() && halaman.isNotBlank())
            ) {
                Icon(Icons.Default.Save, contentDescription = "Simpan")
                Spacer(modifier = Modifier.width(8.dp))
                Text("SIMPAN LAPORAN MENGAJI", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZikirForm(
    initialBacaan: String?,
    initialJumlah: Int,
    onSave: (String, Int) -> Unit,
    onClose: () -> Unit
) {
    var statusMelaksanakan by remember {
        mutableStateOf(if (initialBacaan == "Tidak Melaksanakan") "Tidak Melaksanakan" else "Melaksanakan")
    }
    var bacaan by remember {
        mutableStateOf(
            if (initialBacaan != null && initialBacaan != "Tidak Melaksanakan") initialBacaan else "Subhanallah"
        )
    }
    var jumlah by remember { mutableIntStateOf(if (initialJumlah > 0) initialJumlah else 33) }

    val zikirOptions = listOf("Subhanallah", "Alhamdulillah", "Allahu Akbar", "Istighfar", "Shalawat Nabi")
    val primaryColor = Color(0xFF2E7D32) // Forest Green

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "FORM LAPORAN ZIKIR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = primaryColor
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Status Pelaksanaan Switcher
            Text(
                text = "Status Pelaksanaan Zikir",
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val statusOptions = listOf("Melaksanakan", "Tidak Melaksanakan")
                statusOptions.forEach { option ->
                    val isSelected = statusMelaksanakan == option
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { statusMelaksanakan = option },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (statusMelaksanakan == "Melaksanakan") {
                Text("1. Pilih atau Tulis Bacaan Zikir", fontWeight = FontWeight.Bold, color = primaryColor)

                // Instead of custom layout, simple column of chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    zikirOptions.chunked(3).forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowList.forEach { option ->
                                val isSelected = bacaan == option
                                Card(
                                    onClick = { bacaan = option },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bacaan,
                    onValueChange = { bacaan = it },
                    label = { Text("Atau Tulis Zikir Sendiri") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Zikir", tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Counter
                Text("2. Jumlah Zikir (Berapa Kali)", fontWeight = FontWeight.Bold, color = primaryColor)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { if (jumlah > 1) jumlah -= 1 },
                        modifier = Modifier
                            .size(56.dp)
                            .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = primaryColor)
                    }

                    Text(
                        text = jumlah.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 40.dp)
                    )

                    IconButton(
                        onClick = { jumlah += 1 },
                        modifier = Modifier
                            .size(56.dp)
                            .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = primaryColor)
                    }
                }

                // Quick increment cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(33, 100, 300).forEach { amt ->
                        Button(
                            onClick = { jumlah = amt },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "$amt x", color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "😔",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Tidak Melaksanakan Zikir",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                text = "Silakan simpan laporan ini secara jujur jika anak berhalangan melaksanakan zikir hari ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (statusMelaksanakan == "Melaksanakan") {
                        onSave(bacaan, jumlah)
                    } else {
                        onSave("Tidak Melaksanakan", 0)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (statusMelaksanakan == "Melaksanakan") primaryColor else Color(0xFFD32F2F)
                ),
                enabled = if (statusMelaksanakan == "Melaksanakan") (bacaan.isNotBlank() && jumlah > 0) else true
            ) {
                Icon(Icons.Default.Save, contentDescription = "Simpan")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (statusMelaksanakan == "Melaksanakan") "SIMPAN LAPORAN ZIKIR" else "SIMPAN (TIDAK MELAKSANAKAN)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaktiForm(
    initialJenis: String?,
    initialFoto: String?,
    onSave: (String, String?) -> Unit,
    onClose: () -> Unit
) {
    var statusMelaksanakan by remember {
        mutableStateOf(if (initialJenis == "Tidak Melaksanakan") "Tidak Melaksanakan" else "Melaksanakan")
    }
    var jenisBakti by remember {
        mutableStateOf(
            if (initialJenis != null && initialJenis != "Tidak Melaksanakan") initialJenis else ""
        )
    }
    var fotoBase64 by remember {
        mutableStateOf(
            if (initialJenis != null && initialJenis != "Tidak Melaksanakan") initialFoto else null
        )
    }
    var isCameraOpen by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFFC2185B) // Vibrant pink/magenta

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "FORM BAKTI ORANG TUA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = primaryColor
            )
        )

        if (isCameraOpen) {
            SimulatedCameraOverlay(
                activityName = "Bakti Orang Tua",
                primaryColor = primaryColor,
                onCaptured = { base64 ->
                    fotoBase64 = base64
                    isCameraOpen = false
                },
                onClose = { isCameraOpen = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Pelaksanaan Switcher
                Text(
                    text = "Status Pelaksanaan Bakti Orang Tua",
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val statusOptions = listOf("Melaksanakan", "Tidak Melaksanakan")
                    statusOptions.forEach { option ->
                        val isSelected = statusMelaksanakan == option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { statusMelaksanakan = option },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (statusMelaksanakan == "Melaksanakan") {
                    Text("1. Deskripsi Bakti Orang Tua Hari Ini", fontWeight = FontWeight.Bold, color = primaryColor)
                    OutlinedTextField(
                        value = jenisBakti,
                        onValueChange = { jenisBakti = it },
                        label = { Text("Contoh: Membantu Ibu mencuci piring") },
                        placeholder = { Text("Tulis aktivitas bakti yang dilakukan anak...") },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = "Bakti", tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Text("Saran Bakti Orang Tua:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    val suggestions = listOf("Membantu menyapu rumah", "Merapikan tempat tidur sendiri", "Mencuci piring makan sendiri", "Membuang sampah pada tempatnya")
                    suggestions.forEach { sug ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { jenisBakti = sug },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = "Suggest", tint = primaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sug, fontSize = 12.sp)
                            }
                        }
                    }

                    Text("2. Foto Bukti Aktivitas Bakti (Wajib)", fontWeight = FontWeight.Bold, color = primaryColor)

                    if (fotoBase64 != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, primaryColor, RoundedCornerShape(12.dp))
                        ) {
                            val bitmap = remember(fotoBase64) {
                                try {
                                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto Bakti",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            IconButton(
                                onClick = { fotoBase64 = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                            }
                        }
                    } else {
                        Button(
                            onClick = { isCameraOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Kamera")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("AMBIL FOTO BAKTI LANGSUNG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "😔",
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "Tidak Melaksanakan Bakti Orang Tua",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = "Silakan simpan laporan ini secara jujur jika anak berhalangan membantu orang tua hari ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val isSaveEnabled = if (statusMelaksanakan == "Melaksanakan") {
                    jenisBakti.isNotBlank() && fotoBase64 != null
                } else {
                    true
                }

                Button(
                    onClick = {
                        if (statusMelaksanakan == "Melaksanakan") {
                            onSave(jenisBakti, fotoBase64)
                        } else {
                            onSave("Tidak Melaksanakan", null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (statusMelaksanakan == "Melaksanakan") primaryColor else Color(0xFFD32F2F)
                    ),
                    enabled = isSaveEnabled
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Simpan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (statusMelaksanakan == "Melaksanakan") "SIMPAN LAPORAN BAKTI" else "SIMPAN (TIDAK MELAKSANAKAN)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifikasiForm(
    initialNamaOrtu: String?,
    missingPrayers: List<String> = emptyList(),
    onSave: (String, String) -> Unit,
    onClose: () -> Unit
) {
    var namaOrtu by remember { mutableStateOf(initialNamaOrtu ?: "") }
    var signatureBase64 by remember { mutableStateOf<String?>(null) }
    var showMissingWarningDialog by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF37474F) // Classic Blue Grey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "VERIFIKASI ORANG TUA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = primaryColor
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("1. Tulis Nama Lengkap Orang Tua / Wali", fontWeight = FontWeight.Bold, color = primaryColor)
            OutlinedTextField(
                value = namaOrtu,
                onValueChange = { namaOrtu = it },
                label = { Text("Nama Orang Tua") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Nama", tint = primaryColor) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Text("2. Bubuhkan Tanda Tangan Orang Tua", fontWeight = FontWeight.Bold, color = primaryColor)

            if (signatureBase64 != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, primaryColor, RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    val bitmap = remember(signatureBase64) {
                        try {
                            val bytes = Base64.decode(signatureBase64, Base64.DEFAULT)
                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Tanda Tangan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                    IconButton(
                        onClick = { signatureBase64 = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White)
                    }
                }
            } else {
                SignaturePad(
                    modifier = Modifier.fillMaxWidth(),
                    onSignatureSaved = { base64 ->
                        signatureBase64 = base64
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val sig = signatureBase64
                    if (namaOrtu.isNotBlank() && sig != null) {
                        if (missingPrayers.isNotEmpty()) {
                            showMissingWarningDialog = true
                        } else {
                            onSave(namaOrtu, sig)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                enabled = namaOrtu.isNotBlank() && signatureBase64 != null
            ) {
                Icon(Icons.Default.Verified, contentDescription = "Verifikasi")
                Spacer(modifier = Modifier.width(8.dp))
                Text("KIRIM & VERIFIKASI SEKARANG", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showMissingWarningDialog) {
        AlertDialog(
            onDismissRequest = { showMissingWarningDialog = false },
            icon = { Text("⚠️", fontSize = 36.sp) },
            title = { Text("Ibadah Belum Lengkap", fontWeight = FontWeight.Bold, color = Color(0xFFD84315)) },
            text = {
                Text(
                    text = "Perhatian: Shalat fardhu berikut belum dilaporkan:\n" +
                           missingPrayers.joinToString(", ") { " • $it" } + "\n\n" +
                           "Jika Anda mengirim sekarang, laporan hari ini akan terkunci dan anak Anda tidak dapat melaporkan shalat tersebut lagi di aplikasi ini.\n\n" +
                           "Apakah Anda yakin ingin tetap mengesahkan dan mengirim laporan?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMissingWarningDialog = false
                        val sig = signatureBase64
                        if (sig != null) {
                            onSave(namaOrtu, sig)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Tetap Kirim")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMissingWarningDialog = false }
                ) {
                    Text("Kembali & Lengkapi")
                }
            }
        )
    }
}

@Composable
fun SimulatedCameraOverlay(
    activityName: String,
    primaryColor: Color,
    onCaptured: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val photoBase64 = WorshipUtils.compressBitmapToBase64(bitmap, 75)
            onCaptured(photoBase64)
        } else {
            android.widget.Toast.makeText(context, "Batal mengambil foto", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            android.widget.Toast.makeText(context, "Izin kamera diperlukan untuk mengakses kamera asli perangkat.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Close button at top-left
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(40.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Batal", tint = Color.White)
        }

        // Main camera UI container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Viewfinder representation
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.DarkGray.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Camera Icon",
                        tint = primaryColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ambil Bukti Foto",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Fokus: $activityName",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action section: select camera mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PILIH METODE PENGAMBILAN GAMBAR",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Button 1: Device Camera (Real)
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                cameraLauncher.launch()
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Kamera Perangkat", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Kamera Perangkat (Asli)", fontWeight = FontWeight.Bold)
                    }

                    // Button 2: Simulated Camera (Fallback)
                    OutlinedButton(
                        onClick = {
                            val photoBase64 = WorshipUtils.createSimulatedPhoto(activityName)
                            onCaptured(photoBase64)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Kamera Simulasi", modifier = Modifier.size(20.dp), tint = Color.Yellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ambil Foto Otomatis (Simulasi)", fontWeight = FontWeight.Normal)
                    }
                }
            }
        }
    }
}
