package com.example.ui.settings.appearance

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class CustomFontItem(
    val name: String,
    val displayTitle: String,
    val file: File
)

@Composable
fun CustomFontScreen(
    currentFontPath: String?,
    currentFontName: String?,
    onFontSelected: (path: String?, name: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isOperating by remember { mutableStateOf(false) }
    var fontList by remember { mutableStateOf<List<CustomFontItem>>(emptyList()) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedForDelete by remember { mutableStateOf<Set<String>>(emptySet()) }

    BackHandler(enabled = isDeleteMode) {
        isDeleteMode = false
        selectedForDelete = emptySet()
    }

    fun loadFontList() {
        val fontsDir = File(context.filesDir, "custom_fonts")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }
        val files = fontsDir.listFiles { file ->
            val ext = file.extension.lowercase()
            file.isFile && (ext == "ttf" || ext == "otf")
        } ?: emptyArray()
        fontList = files.sortedBy { it.name.lowercase() }.map { file ->
            val cleanTitle = file.nameWithoutExtension
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifBlank { file.name }
            CustomFontItem(
                name = file.name,
                displayTitle = cleanTitle,
                file = file
            )
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            loadFontList()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isOperating = true
            val (savedFile, originalName, errorMsg) = withContext(Dispatchers.IO) {
                importCustomFont(context, uri)
            }
            if (savedFile != null && originalName != null) {
                withContext(Dispatchers.IO) {
                    loadFontList()
                }
                onFontSelected(savedFile.absolutePath, originalName)
                Toast.makeText(context, "Font diterapkan: $originalName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    context,
                    errorMsg ?: "Gagal mengimpor font. Pastikan file valid (.ttf / .otf).",
                    Toast.LENGTH_LONG
                ).show()
            }
            isOperating = false
        }
    }

    val activeFontLabel = remember(currentFontPath, currentFontName, fontList) {
        if (currentFontPath.isNullOrBlank()) {
            "System Default"
        } else {
            fontList.find { it.file.absolutePath == currentFontPath }?.displayTitle
                ?: currentFontName?.substringBeforeLast('.')?.replace('_', ' ')
                ?: "Custom Font"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TYPOGRAPHY PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = activeFontLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Midnight City",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "M83 • Hurry Up, We're Dreaming",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "The quick brown fox jumps over the lazy dog • 01:24 / 04:03",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }

        if (!isDeleteMode) {
            Button(
                onClick = {
                    openDocumentLauncher.launch(
                        arrayOf(
                            "font/ttf",
                            "font/otf",
                            "application/octet-stream",
                            "font/*",
                            "application/x-font-ttf",
                            "application/x-font-opentype"
                        )
                    )
                },
                enabled = !isOperating,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_add_font")
            ) {
                if (isOperating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import File Font (.ttf / .otf)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        val allPaths = fontList.map { it.file.absolutePath }.toSet()
                        selectedForDelete = if (selectedForDelete.size == allPaths.size) emptySet() else allPaths
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text(
                        text = if (selectedForDelete.size == fontList.size && fontList.isNotEmpty()) "Batal Pilih" else "Pilih Semua",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        val targets = selectedForDelete.toList()
                        if (targets.isEmpty()) return@Button
                        scope.launch {
                            isOperating = true
                            val wasCurrentDeleted = currentFontPath != null && targets.contains(currentFontPath)
                            withContext(Dispatchers.IO) {
                                targets.forEach { path -> deleteFontFile(context, path) }
                                loadFontList()
                            }
                            if (wasCurrentDeleted) {
                                onFontSelected(null, null)
                            }
                            selectedForDelete = emptySet()
                            isDeleteMode = false
                            isOperating = false
                            Toast.makeText(context, "${targets.size} font dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedForDelete.isNotEmpty() && !isOperating,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hapus (${selectedForDelete.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDeleteMode) "Pilih Font untuk Dihapus" else "Available Fonts (${fontList.size + 1})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (fontList.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            isDeleteMode = !isDeleteMode
                            selectedForDelete = emptySet()
                        }
                    ) {
                        if (!isDeleteMode) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Hapus Font",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Selesai",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (!isDeleteMode) {
                val isDefaultSelected = currentFontPath.isNullOrBlank()
                FontSelectionCard(
                    title = "System Default",
                    subtitle = "Font bawaan sistem Android",
                    previewFontFamily = FontFamily.Default,
                    isSelected = isDefaultSelected,
                    isDeleteMode = false,
                    isCheckedForDelete = false,
                    onClick = { onFontSelected(null, null) },
                    onLongClick = null,
                    modifier = Modifier.testTag("font_card_default")
                )
            }

            fontList.forEach { item ->
                val path = item.file.absolutePath
                val isSelected = currentFontPath == path
                val isCheckedForDelete = selectedForDelete.contains(path)
                var itemFontFamily by remember(path) { mutableStateOf<FontFamily>(FontFamily.Default) }
                LaunchedEffect(path) {
                    itemFontFamily = withContext(Dispatchers.IO) {
                        runCatching {
                            FontFamily(Typeface.createFromFile(item.file))
                        }.getOrDefault(FontFamily.Default)
                    }
                }

                FontSelectionCard(
                    title = item.displayTitle,
                    subtitle = item.name,
                    previewFontFamily = itemFontFamily,
                    isSelected = isSelected,
                    isDeleteMode = isDeleteMode,
                    isCheckedForDelete = isCheckedForDelete,
                    onClick = {
                        if (isDeleteMode) {
                            selectedForDelete = if (isCheckedForDelete) {
                                selectedForDelete - path
                            } else {
                                selectedForDelete + path
                            }
                        } else {
                            onFontSelected(path, item.displayTitle)
                        }
                    },
                    onLongClick = {
                        if (!isDeleteMode) {
                            isDeleteMode = true
                            selectedForDelete = setOf(path)
                        }
                    },
                    modifier = Modifier.testTag("font_card_${item.name}")
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FontSelectionCard(
    title: String,
    subtitle: String,
    previewFontFamily: FontFamily,
    isSelected: Boolean,
    isDeleteMode: Boolean,
    isCheckedForDelete: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val highlightCard = if (isDeleteMode) isCheckedForDelete else isSelected

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlightCard) {
                if (isDeleteMode) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
                }
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = if (highlightCard) {
            BorderStroke(
                width = 1.5.dp,
                color = if (isDeleteMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        } else {
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected && !isDeleteMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = previewFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = if (isSelected && !isDeleteMode) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = previewFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.5.sp
                    ),
                    color = if (isSelected && !isDeleteMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isDeleteMode) {
                Checkbox(
                    checked = isCheckedForDelete,
                    onCheckedChange = { onClick() }
                )
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
            }
        }
    }
}

private fun importCustomFont(context: Context, uri: Uri): Triple<File?, String?, String?> {
    try {
        var displayName: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        displayName = cursor.getString(idx)
                    }
                }
            }
        }
        if (displayName == null) {
            displayName = uri.lastPathSegment ?: "font.ttf"
        }

        val rawName = displayName!!
        val ext = rawName.substringAfterLast('.', "").lowercase()
        if (ext != "ttf" && ext != "otf") {
            return Triple(null, null, "Format file tidak didukung. Harap pilih file .ttf atau .otf.")
        }

        val fontsDir = File(context.filesDir, "custom_fonts")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }

        val baseName = rawName.substringBeforeLast('.')
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "font" }

        var candidateFile = File(fontsDir, "$baseName.$ext")
        var counter = 1
        while (candidateFile.exists()) {
            candidateFile = File(fontsDir, "${baseName}_$counter.$ext")
            counter++
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(candidateFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: return Triple(null, null, "Gagal membaca aliran data font.")

        val typeface = try {
            Typeface.createFromFile(candidateFile)
        } catch (_: Exception) {
            null
        }
        if (typeface == null) {
            candidateFile.delete()
            return Triple(null, null, "File font tidak valid atau rusak.")
        }

        val cleanTitle = baseName.replace('_', ' ').trim()
        return Triple(candidateFile, cleanTitle, null)
    } catch (e: Exception) {
        return Triple(null, null, "Kesalahan saat mengimpor font: ${e.localizedMessage}")
    }
}

private fun deleteFontFile(context: Context, path: String) {
    try {
        val fontsDir = File(context.filesDir, "custom_fonts").canonicalPath
        val targetFile = File(path)
        if (targetFile.canonicalPath.startsWith(fontsDir) && targetFile.exists() && targetFile.isFile) {
            targetFile.delete()
        }
    } catch (_: Exception) {
    }
}
