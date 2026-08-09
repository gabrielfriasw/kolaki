package com.kolvexo.kolaki.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kolvexo.kolaki.R
import com.kolvexo.kolaki.ui.components.BrutalButton
import com.kolvexo.kolaki.ui.components.KolaKiLogo
import com.kolvexo.kolaki.ui.theme.CinzaCafe
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Lime
import com.kolvexo.kolaki.ui.theme.Preto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (appeared) 1f else .82f, label = "logoScale")
    LaunchedEffect(Unit) {
        appeared = true
        delay(900)
        onFinished()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Lime)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KolaKiLogo(Modifier.scale(scale), height = 56.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Cria. Kola. Manda.",
                color = Preto,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val body: String,
    @DrawableRes val illustration: Int,
)

@Composable
fun OnboardingScreen(onSkip: () -> Unit, onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "Escolhe as imagens.",
            "Sua galeria vira figurinha no zap de um jeito rápido, direto e sem anúncios chatos.",
            R.drawable.tut1,
        ),
        OnboardingPage(
            "Monta o pacote.",
            "Recorta, gira e organiza. Tudo acontece no seu celular.",
            R.drawable.tut2,
        ),
        OnboardingPage(
            "Cola no WhatsApp.",
            "Um toque e o pacote já fica pronto pra mandar nas conversas.",
            R.drawable.tut3,
        ),
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KolaKiLogo(height = 23.dp)
            Spacer(Modifier.weight(1f))
            Text(
                "Pular",
                modifier = Modifier.clickable(onClick = onSkip).padding(8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
        ) { index ->
            val page = pages[index]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(page.illustration),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(52.dp))
                Text(
                    page.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (MaterialTheme.colorScheme.background == Color(0xFFFFF8F0)) CinzaCafe else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    Box(
                        Modifier
                            .size(if (index == pager.currentPage) 16.dp else 14.dp)
                            .background(if (index == pager.currentPage) Preto else Color.Transparent, CircleShape)
                            .border(2.dp, Preto, CircleShape),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            BrutalButton(
                text = if (pager.currentPage == pages.lastIndex) "Bora criar" else "Próximo",
                onClick = {
                    if (pager.currentPage == pages.lastIndex) onFinish()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth(.52f),
                backgroundColor = Coral,
            )
        }
    }

}
