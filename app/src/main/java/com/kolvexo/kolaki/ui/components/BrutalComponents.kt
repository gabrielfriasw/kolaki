package com.kolvexo.kolaki.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kolvexo.kolaki.R
import com.kolvexo.kolaki.ui.theme.Amarelo
import com.kolvexo.kolaki.ui.theme.Branco
import com.kolvexo.kolaki.ui.theme.CinzaBorda
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Lime
import com.kolvexo.kolaki.ui.theme.Preto

@Composable
fun BrutalSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 3.dp,
    shadowOffset: Dp = 5.dp,
    shadowHorizontalOffset: Dp = shadowOffset,
    shadowColor: Color = Preto,
    content: @Composable () -> Unit,
) {
    val borderColor = if (MaterialTheme.colorScheme.background.luminance() < .5f) Branco else Preto
    Box(modifier = modifier.padding(end = shadowHorizontalOffset, bottom = shadowOffset)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(shadowHorizontalOffset, shadowOffset)
                .background(shadowColor, shape),
        )
        Box(
            Modifier
                .background(backgroundColor, shape)
                .border(BorderStroke(borderWidth, borderColor), shape),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                content = content,
            )
        }
    }
}

@Composable
fun BrutalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Coral,
    contentColor: Color = Branco,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed && enabled) 2.dp else 0.dp, label = "buttonShift")
    val shadow by animateDpAsState(if (pressed && enabled) 2.dp else 5.dp, label = "buttonShadow")
    val faceColor = if (enabled) backgroundColor else CinzaBorda
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    BrutalSurface(
        modifier = modifier.offset { IntOffset(0, shift.roundToPx()) },
        backgroundColor = faceColor,
        contentColor = if (enabled) contentColor else Color(0xFF5A5A5A),
        shape = RoundedCornerShape(14.dp),
        shadowOffset = shadow,
        shadowHorizontalOffset = 0.dp,
        shadowColor = duolingoShadow(faceColor, darkTheme),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
fun BrutalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    BrutalSurface(
        modifier = modifier.size(52.dp),
        backgroundColor = backgroundColor,
        shape = CircleShape,
        shadowOffset = 4.dp,
        shadowHorizontalOffset = 0.dp,
        shadowColor = duolingoShadow(backgroundColor, darkTheme),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, Modifier.size(25.dp))
        }
    }
}

private fun duolingoShadow(faceColor: Color, darkTheme: Boolean): Color = when {
    !darkTheme -> Preto
    faceColor.luminance() < .12f -> Color(0xFF514C47)
    else -> lerp(faceColor, Preto, .48f)
}

@Composable
fun KolaKiLogo(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    val darkWordmarkFilter = remember(darkTheme) {
        if (!darkTheme) {
            null
        } else {
            // clareia apenas a parte preta do wordmark no tema escuro
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0f, 0f, 0f, 0f, 255f,
                        0f, -1.9310344f, 0f, 0f, 255f,
                        0f, 0f, -4f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
    }
    Image(
        painter = painterResource(R.drawable.kolaki_logo),
        contentDescription = "KolaKi",
        modifier = modifier.width(height * (215f / 56f)).height(height),
        contentScale = ContentScale.Fit,
        colorFilter = darkWordmarkFilter,
    )
}

@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Amarelo,
) {
    Box(
        modifier
            .background(Preto, RoundedCornerShape(999.dp))
            .padding(end = 2.dp, bottom = 2.dp),
    ) {
        Box(
            Modifier
                .background(color, RoundedCornerShape(999.dp))
                .border(2.dp, Preto, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(text, color = Preto, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = TextUnit.Unspecified),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun ColorSquares(modifier: Modifier = Modifier, accent: Color = Lime) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(accent, MaterialTheme.colorScheme.background, Coral, Amarelo).chunked(2).let { columns ->
            columns.forEach { column ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    column.forEach { color ->
                        val shape = RoundedCornerShape(5.dp)
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(color, shape)
                                .border(2.dp, Preto, shape),
                        )
                    }
                }
            }
        }
    }
}
