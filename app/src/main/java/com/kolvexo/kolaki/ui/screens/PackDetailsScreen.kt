package com.kolvexo.kolaki.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kolvexo.kolaki.data.PackRules
import com.kolvexo.kolaki.data.StickerItem
import com.kolvexo.kolaki.data.StickerPack
import com.kolvexo.kolaki.ui.components.BrutalButton
import com.kolvexo.kolaki.ui.components.BrutalIconButton
import com.kolvexo.kolaki.ui.components.BrutalSurface
import com.kolvexo.kolaki.ui.components.StatusChip
import com.kolvexo.kolaki.ui.theme.Amarelo
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Lime
import com.kolvexo.kolaki.ui.theme.Preto
import java.io.File

@Composable
fun PackDetailsScreen(
    pack: StickerPack,
    processing: Boolean,
    fileFor: (String) -> File,
    onBack: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit,
    onEditSticker: (String) -> Unit,
    onAddToWhatsApp: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(PackRules.MAX_STICKERS),
    ) { uris -> if (uris.isNotEmpty()) onImagesSelected(uris) }
    val openPicker = {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val missing = (PackRules.MIN_STICKERS - pack.stickers.size).coerceAtLeast(0)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrutalIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text(pack.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Criado por você",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${pack.stickers.size} de ${PackRules.MAX_STICKERS} figurinhas",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                StatusChip(
                    text = if (pack.isReady) "Pronto" else "Em andamento",
                    color = if (pack.isReady) Lime else Coral,
                )
            }
            Spacer(Modifier.height(22.dp))

            AnimatedVisibility(!pack.isReady) {
                BrutalSurface(
                    Modifier.fillMaxWidth(),
                    backgroundColor = Amarelo,
                    contentColor = Preto,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.WarningAmber, null, Modifier.size(28.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                if (missing == 1) {
                                    "Só falta mais 1 figurinha"
                                } else {
                                    "Adicione mais $missing figurinhas"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "O WhatsApp pede pelo menos ${PackRules.MIN_STICKERS} por pacote.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            val cells = buildList<GridItem> {
                pack.stickers.forEach { add(GridItem.Sticker(it)) }
                repeat(minOf(3, PackRules.MAX_STICKERS - pack.stickers.size)) { add(GridItem.Empty) }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cells) { item ->
                    when (item) {
                        is GridItem.Sticker -> StickerCell(
                            file = fileFor(item.sticker.fileName),
                            onClick = { onEditSticker(item.sticker.fileName) },
                        )
                        GridItem.Empty -> EmptyCell(openPicker)
                    }
                }
            }

            if (pack.stickers.size < PackRules.MAX_STICKERS) {
                BrutalButton(
                    "Adicionar imagens",
                    openPicker,
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Default.Add,
                )
            }
            BrutalButton(
                "Adicionar ao WhatsApp",
                onAddToWhatsApp,
                Modifier.fillMaxWidth(),
                enabled = pack.isReady && !processing,
            )
        }

        if (processing) {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = .86f)),
                contentAlignment = Alignment.Center,
            ) {
                BrutalSurface(backgroundColor = Amarelo) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = Preto, strokeWidth = 3.dp)
                        Text(
                            "Ajeitando as figurinhas…",
                            Modifier.padding(start = 14.dp),
                            color = Preto,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface GridItem {
    data class Sticker(val sticker: StickerItem) : GridItem
    data object Empty : GridItem
}

@Composable
private fun StickerCell(file: File, onClick: () -> Unit) {
    BrutalSurface(Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(12.dp), shadowOffset = 4.dp) {
        AsyncImage(
            model = file,
            contentDescription = "Figurinha",
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun EmptyCell(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Add, "Adicionar imagem", Modifier.size(30.dp))
    }
}
