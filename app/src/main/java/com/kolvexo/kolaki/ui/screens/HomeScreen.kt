package com.kolvexo.kolaki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kolvexo.kolaki.data.PackRules
import com.kolvexo.kolaki.data.StickerPack
import com.kolvexo.kolaki.ui.components.BrutalButton
import com.kolvexo.kolaki.ui.components.BrutalIconButton
import com.kolvexo.kolaki.ui.components.BrutalSurface
import com.kolvexo.kolaki.ui.components.ColorSquares
import com.kolvexo.kolaki.ui.components.KolaKiLogo
import com.kolvexo.kolaki.ui.components.KolaMascot
import com.kolvexo.kolaki.ui.components.MascotMood
import com.kolvexo.kolaki.ui.components.StatusChip
import com.kolvexo.kolaki.ui.theme.Amarelo
import com.kolvexo.kolaki.ui.theme.CinzaCafe
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Lime
import com.kolvexo.kolaki.ui.theme.Preto
import com.kolvexo.kolaki.ui.theme.Rosa
import com.kolvexo.kolaki.ui.theme.Roxo

@Composable
fun HomeScreen(
    packs: List<StickerPack>,
    onOpenSettings: () -> Unit,
    onOpenPack: (String) -> Unit,
    onCreatePack: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KolaKiLogo(height = 27.dp)
                Spacer(Modifier.weight(1f))
                BrutalIconButton(
                    Icons.Outlined.Settings,
                    "Configurações",
                    onOpenSettings,
                )
            }

            if (packs.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    KolaMascot(Modifier.size(200.dp), MascotMood.SLEEPY)
                    Spacer(Modifier.height(26.dp))
                    Text(
                        "Nada colado ainda.",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Bora criar o primeiro pacote e começar a mandar no grupo do zap?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(30.dp))
                    BrutalButton(
                        "Criar primeiro pacote",
                        { showCreate = true },
                        Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Spacer(Modifier.height(64.dp))
                Text(
                    "Meus Pacotes",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(22.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 82.dp),
                ) {
                    items(packs, key = { it.identifier }) { pack ->
                        PackCard(pack, onClick = { onOpenPack(pack.identifier) })
                    }
                }
            }
        }

        BrutalIconButton(
            Icons.Default.Add,
            "Criar pacote",
            onClick = { showCreate = true },
            modifier = Modifier.align(Alignment.BottomEnd).size(64.dp),
            backgroundColor = Coral,
        )
    }

    if (showCreate) {
        CreatePackDialog(
            onDismiss = { showCreate = false },
            onCreate = {
                showCreate = false
                onCreatePack(it)
            },
        )
    }
}

@Composable
private fun PackCard(pack: StickerPack, onClick: () -> Unit) {
    val accent = listOf(Lime, Roxo, Rosa)[kotlin.math.abs(pack.identifier.hashCode()) % 3]
    BrutalSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.clickable(onClick = onClick)) {
            Box(Modifier.fillMaxWidth().height(8.dp).background(accent))
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorSquares(accent = accent)
                Column(Modifier.padding(start = 18.dp).weight(1f)) {
                    Text(pack.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${pack.stickers.size}/${PackRules.MAX_STICKERS} figs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(
                    text = when {
                        pack.isReady -> "Pronto"
                        pack.stickers.isEmpty() -> "Novo"
                        else -> "Em andamento"
                    },
                    color = if (pack.isReady) Lime else if (pack.stickers.isEmpty()) Amarelo else Coral,
                )
            }
        }
    }
}

@Composable
private fun CreatePackDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        BrutalSurface(Modifier.fillMaxWidth(), shadowOffset = 7.dp) {
            Column(Modifier.padding(22.dp)) {
                Text("Novo pacote", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Dá um nome pra sua próxima leva de figs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(128) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex.: Memes do grupo") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrutalButton(
                        "Cancelar",
                        onDismiss,
                        Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                    BrutalButton(
                        "Criar",
                        { onCreate(name) },
                        Modifier.weight(1f),
                        enabled = name.isNotBlank(),
                    )
                }
            }
        }
    }
}
