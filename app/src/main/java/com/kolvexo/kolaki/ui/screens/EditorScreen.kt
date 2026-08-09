package com.kolvexo.kolaki.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kolvexo.kolaki.data.StickerEdit
import com.kolvexo.kolaki.ui.components.BrutalIconButton
import com.kolvexo.kolaki.ui.components.BrutalSurface
import com.kolvexo.kolaki.ui.theme.Branco
import com.kolvexo.kolaki.ui.theme.CinzaClaro
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Preto
import java.io.File
import kotlin.math.abs

@Composable
fun EditorScreen(
    file: File,
    animated: Boolean,
    imageVersion: Int,
    processing: Boolean,
    onClose: () -> Unit,
    onEdit: (StickerEdit, onComplete: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    fun resetCrop() {
        scale = 1f
        offset = Offset.Zero
    }

    fun clampOffset(value: Offset, atScale: Float): Offset {
        val maxX = viewportSize.width * (atScale - 1f) / 2f
        val maxY = viewportSize.height * (atScale - 1f) / 2f
        return Offset(
            value.x.coerceIn(-maxX, maxX),
            value.y.coerceIn(-maxY, maxY),
        )
    }

    fun cropEdit() = StickerEdit.Crop(
        scale = scale,
        offsetX = if (viewportSize.width == 0) 0f else offset.x / viewportSize.width,
        offsetY = if (viewportSize.height == 0) 0f else offset.y / viewportSize.height,
    )

    val hasCropChanges = scale > 1.001f || abs(offset.x) > 0.5f || abs(offset.y) > 0.5f
    val model = ImageRequest.Builder(context)
        .data(file)
        .memoryCacheKey("${file.absolutePath}-$imageVersion")
        .build()

    LaunchedEffect(imageVersion) { resetCrop() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrutalIconButton(Icons.Default.Close, "Cancelar edição", onClose)
                Text(
                    "Recortar figurinha",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                BrutalIconButton(
                    icon = Icons.Default.Check,
                    contentDescription = "Salvar e concluir",
                    onClick = {
                        if (!processing && hasCropChanges) onEdit(cropEdit(), onClose)
                        else if (!processing) onClose()
                    },
                    backgroundColor = Coral,
                )
            }

            Column(
                Modifier.weight(1f).padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BrutalSurface(
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    backgroundColor = CinzaClaro,
                    shape = RoundedCornerShape(18.dp),
                    shadowOffset = 0.dp,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(15.dp))
                            .onSizeChanged { viewportSize = it }
                            .pointerInput(imageVersion, viewportSize) {
                                detectTransformGestures { centroid, pan, gestureZoom, _ ->
                                    val oldScale = scale
                                    val newScale = (oldScale * gestureZoom).coerceIn(1f, 5f)
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val focalPoint = centroid - center - offset
                                    val proposed = offset + pan +
                                        focalPoint * (1f - newScale / oldScale)
                                    scale = newScale
                                    offset = clampOffset(proposed, newScale)
                                }
                            },
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = "Figurinha para recortar",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.FillBounds,
                        )
                        CropGrid(Modifier.fillMaxSize())
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Arraste para posicionar • use dois dedos para ampliar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zoom", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = scale,
                        onValueChange = { value ->
                            scale = value
                            offset = clampOffset(offset, value)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                        valueRange = 1f..5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Coral,
                            activeTrackColor = Preto,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                    Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    if (animated) "GIF animado • 512 × 512 px" else "Imagem estática • 512 × 512 px",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EditAction(
                    label = "Resetar",
                    icon = Icons.Default.Refresh,
                    onClick = ::resetCrop,
                    modifier = Modifier.weight(1f),
                )
                EditAction(
                    label = "Girar",
                    icon = Icons.AutoMirrored.Filled.RotateRight,
                    onClick = {
                        if (!processing) onEdit(StickerEdit.Rotate, ::resetCrop)
                    },
                    modifier = Modifier.weight(1f),
                )
                EditAction(
                    label = "Aplicar",
                    icon = Icons.Default.Crop,
                    onClick = {
                        if (!processing && hasCropChanges) onEdit(cropEdit(), ::resetCrop)
                    },
                    modifier = Modifier.weight(1f),
                    backgroundColor = Coral,
                    contentColor = Branco,
                )
            }
        }

        if (processing) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = .78f)),
                contentAlignment = Alignment.Center,
            ) {
                BrutalSurface(backgroundColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Coral, strokeWidth = 4.dp)
                        Text(
                            if (animated) "Processando todos os quadros…" else "Aplicando recorte…",
                            Modifier.padding(start = 14.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CropGrid(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val lineColor = Color.White.copy(alpha = .62f)
        val shadowColor = Color.Black.copy(alpha = .28f)
        for (part in 1..2) {
            val x = size.width * part / 3f
            val y = size.height * part / 3f
            drawLine(shadowColor, Offset(x + 1f, 0f), Offset(x + 1f, size.height), 2f)
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1.5f)
            drawLine(shadowColor, Offset(0f, y + 1f), Offset(size.width, y + 1f), 2f)
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.5f)
        }
        drawRect(Color.Black.copy(alpha = .72f), style = Stroke(4f))
    }
}

@Composable
private fun EditAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    BrutalSurface(
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
        shadowOffset = 4.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().height(76.dp).clickable(onClick = onClick).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, Modifier.size(25.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
