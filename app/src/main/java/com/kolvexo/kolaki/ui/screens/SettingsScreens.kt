package com.kolvexo.kolaki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kolvexo.kolaki.data.ThemeMode
import com.kolvexo.kolaki.ui.components.BrutalButton
import com.kolvexo.kolaki.ui.components.BrutalIconButton
import com.kolvexo.kolaki.ui.components.BrutalSurface
import com.kolvexo.kolaki.ui.components.KolaKiLogo
import com.kolvexo.kolaki.ui.components.SectionLabel
import com.kolvexo.kolaki.ui.theme.Branco
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Lime
import com.kolvexo.kolaki.ui.theme.Preto
import com.kolvexo.kolaki.ui.theme.Roxo

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    stickerCount: Int,
    usedBytes: Long,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onAbout: () -> Unit,
    onSourceCode: () -> Unit,
    onLicenses: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
            Text(
                "Configurações",
                Modifier.padding(start = 20.dp),
                style = MaterialTheme.typography.headlineLarge,
                color = if (MaterialTheme.colorScheme.background.luminance() < .5f) {
                    Branco
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            )
        }
        Spacer(Modifier.height(44.dp))
        SectionLabel("Aparência")
        Spacer(Modifier.height(14.dp))
        ThemeSelector(themeMode, onThemeChange)
        Spacer(Modifier.height(36.dp))
        SectionLabel("Armazenamento")
        Spacer(Modifier.height(14.dp))
        BrutalSurface(Modifier.fillMaxWidth()) {
            Column {
                SettingStat("Figurinhas salvas", stickerCount.toString(), Coral)
                Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onSurface))
                SettingStat("Espaço usado", formatBytes(usedBytes))
            }
        }
        Spacer(Modifier.height(36.dp))
        SectionLabel("Geral")
        Spacer(Modifier.height(14.dp))
        BrutalSurface(Modifier.fillMaxWidth()) {
            Column {
                SettingLink("Sobre o KolaKi", onAbout)
                Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onSurface))
                SettingLink("Código-fonte", onSourceCode)
                Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.onSurface))
                SettingLink("Licenças", onLicenses)
            }
        }
    }
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    BrutalSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(3.dp)) {
            listOf(
                ThemeMode.LIGHT to "Claro",
                ThemeMode.DARK to "Escuro",
                ThemeMode.SYSTEM to "Sistema",
            ).forEach { (mode, label) ->
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (selected == mode) Coral else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { onSelect(mode) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected == mode) Branco else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingStat(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

@Composable
private fun SettingLink(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Default.ChevronRight, null)
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onGitHub: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            Modifier.fillMaxWidth().background(Lime).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrutalIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Voltar",
                onBack,
                Modifier.align(Alignment.Start),
                backgroundColor = Branco,
            )
            Spacer(Modifier.height(42.dp))
            KolaKiLogo(height = 72.dp)
            Spacer(Modifier.height(70.dp))
            Text("v1.0.0", color = Preto, style = MaterialTheme.typography.bodyMedium)
        }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Cria. Kola. Manda.",
                color = Coral,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            BrutalSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Sem anúncio. Sem cadastro.", style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface))
                    Text("Sem assinatura.", style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface))
                    Text(
                        "Tudo acontece no seu celular.",
                        style = MaterialTheme.typography.titleLarge,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
            BrutalSurface(
                Modifier.fillMaxWidth(),
                backgroundColor = Roxo,
                contentColor = Branco,
            ) {
                Column(Modifier.fillMaxWidth().clickable(onClick = onGitHub).padding(20.dp)) {
                    Text("Código público para consulta", style = MaterialTheme.typography.titleLarge)
                    Text("Ver no GitHub →", color = Lime, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrutalIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
            Text("Licenças", Modifier.padding(start = 20.dp), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(40.dp))
        BrutalSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Código do KolaKi", style = MaterialTheme.typography.titleLarge)
                Text("Todos os direitos reservados", style = MaterialTheme.typography.bodyMedium)
                Text("Dela Gothic One, Nunito e DM Sans", style = MaterialTheme.typography.titleMedium)
                Text("SIL Open Font License 1.1", style = MaterialTheme.typography.bodyMedium)
                Text("Contrato de figurinhas do WhatsApp", style = MaterialTheme.typography.titleMedium)
                Text("Baseado na amostra oficial, licença BSD.", style = MaterialTheme.typography.bodyMedium)
                Text("FFmpegKit e FFmpeg", style = MaterialTheme.typography.titleMedium)
                Text("LGPL v3 / LGPL 2.1 ou posterior", style = MaterialTheme.typography.bodyMedium)
                Text("Coil e Smart Exception", style = MaterialTheme.typography.titleMedium)
                Text("Apache License 2.0", style = MaterialTheme.typography.bodyMedium)
                Text("libwebp", style = MaterialTheme.typography.titleMedium)
                Text("Licença BSD de 3 cláusulas", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SuccessScreen(onOpenWhatsApp: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Lime)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrutalSurface(
            modifier = Modifier.size(118.dp),
            backgroundColor = Color(0xFFFFD23F),
            shape = CircleShape,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, Modifier.size(58.dp), tint = Preto)
            }
        }
        Spacer(Modifier.height(42.dp))
        Text("PACOTE COLADO!", color = Preto, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text(
            "Já tá no WhatsApp. Vai lá mandar pras conversas!",
            color = Preto,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(150.dp))
        BrutalButton(
            "Ver pacote no Zap",
            onOpenWhatsApp,
            Modifier.fillMaxWidth(),
            backgroundColor = Branco,
            contentColor = Preto,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Voltar pro KolaKi",
            Modifier.clickable(onClick = onDone).padding(10.dp),
            color = Preto,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1024f / 1024f)
}
