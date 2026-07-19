package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureSaved: (String) -> Unit,
    onCleared: () -> Unit = {}
) {
    val points = remember { mutableStateListOf<Offset?>() }
    
    Column(modifier = modifier) {
        Text(
            text = "Sentuh & Gambar Tanda Tangan Anda di Bawah Ini:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .background(ComposeColor.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            points.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            points.add(change.position)
                        },
                        onDragEnd = {
                            points.add(null) // Separator between stroke segments
                        }
                    )
                }
        ) {
            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                if (points.isNotEmpty()) {
                    val path = ComposePath()
                    var isFirst = true
                    
                    for (i in 0 until points.size) {
                        val pt = points[i]
                        if (pt == null) {
                            isFirst = true
                        } else {
                            if (isFirst) {
                                path.moveTo(pt.x, pt.y)
                                isFirst = false
                            } else {
                                path.lineTo(pt.x, pt.y)
                            }
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = ComposeColor.Black,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }
            }
            
            if (points.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tulis Tanda Tangan Orang Tua Di Sini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ComposeColor.Gray.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    points.clear()
                    onCleared()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus")
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Button(
                onClick = {
                    if (points.isNotEmpty()) {
                        // Generate a small compressed bitmap signature
                        val width = 240
                        val height = 120
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            strokeWidth = 4f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                        }
                        
                        // Draw clean white background
                        canvas.drawColor(Color.WHITE)
                        
                        val validPoints = points.filterNotNull()
                        if (validPoints.isNotEmpty()) {
                            val minX = validPoints.minOf { it.x }
                            val maxX = validPoints.maxOf { it.x }
                            val minY = validPoints.minOf { it.y }
                            val maxY = validPoints.maxOf { it.y }
                            
                            val pathWidth = maxX - minX
                            val pathHeight = maxY - minY
                            
                            val scaleX = if (pathWidth > 0) (width - 24f) / pathWidth else 1f
                            val scaleY = if (pathHeight > 0) (height - 24f) / pathHeight else 1f
                            val scale = minOf(scaleX, scaleY).coerceAtMost(2f)
                            
                            val offsetX = (width - pathWidth * scale) / 2f - minX * scale
                            val offsetY = (height - pathHeight * scale) / 2f - minY * scale
                            
                            val nativePath = Path()
                            var isFirst = true
                            
                            for (i in 0 until points.size) {
                                val pt = points[i]
                                if (pt == null) {
                                    isFirst = true
                                } else {
                                    val drawX = pt.x * scale + offsetX
                                    val drawY = pt.y * scale + offsetY
                                    if (isFirst) {
                                        nativePath.moveTo(drawX, drawY)
                                        isFirst = false
                                    } else {
                                        nativePath.lineTo(drawX, drawY)
                                    }
                                }
                            }
                            canvas.drawPath(nativePath, paint)
                        }
                        
                        val base64 = WorshipUtils.compressBitmapToBase64(bitmap, 40)
                        bitmap.recycle()
                        onSignatureSaved(base64)
                    }
                },
                enabled = points.isNotEmpty(),
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Done, contentDescription = "Done")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan")
            }
        }
    }
}
