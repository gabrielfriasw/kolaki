package com.kolvexo.kolaki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.kolvexo.kolaki.ui.theme.Amarelo
import com.kolvexo.kolaki.ui.theme.Branco
import com.kolvexo.kolaki.ui.theme.Coral
import com.kolvexo.kolaki.ui.theme.Preto
import com.kolvexo.kolaki.ui.theme.Rosa

enum class MascotMood { HAPPY, SLEEPY, EXCITED }

@Composable
fun KolaMascot(modifier: Modifier = Modifier, mood: MascotMood = MascotMood.HAPPY) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.025f
        val bodyW = size.width * 0.52f
        val bodyH = size.height * 0.58f
        val left = (size.width - bodyW) / 2f
        val top = size.height * 0.19f
        val corner = size.minDimension * 0.12f

        translate(stroke * 2.2f, stroke * 2.2f) {
            drawRoundRect(Preto, Offset(left, top), Size(bodyW, bodyH), CornerRadius(corner))
        }
        drawRoundRect(Coral, Offset(left, top), Size(bodyW, bodyH), CornerRadius(corner))
        drawRoundRect(
            Preto,
            Offset(left, top),
            Size(bodyW, bodyH),
            CornerRadius(corner),
            style = Stroke(stroke),
        )

        val fold = bodyW * 0.22f
        val foldPath = Path().apply {
            moveTo(left + bodyW - fold, top + bodyH)
            lineTo(left + bodyW - fold, top + bodyH - fold)
            quadraticTo(left + bodyW - fold, top + bodyH - fold * 1.12f, left + bodyW - fold * 0.78f, top + bodyH - fold * 1.12f)
            lineTo(left + bodyW, top + bodyH - fold * 1.12f)
            close()
        }
        drawPath(foldPath, Branco)
        drawPath(foldPath, Preto, style = Stroke(stroke))

        val eyeY = top + bodyH * 0.42f
        val eyeOffset = bodyW * 0.2f
        when (mood) {
            MascotMood.SLEEPY -> {
                drawArc(
                    Preto,
                    15f,
                    150f,
                    false,
                    topLeft = Offset(left + eyeOffset - 18f, eyeY - 6f),
                    size = Size(36f, 22f),
                    style = Stroke(stroke),
                )
                drawArc(
                    Preto,
                    15f,
                    150f,
                    false,
                    topLeft = Offset(left + bodyW - eyeOffset - 18f, eyeY - 6f),
                    size = Size(36f, 22f),
                    style = Stroke(stroke),
                )
                drawLine(Preto, Offset(left + bodyW * .39f, top + bodyH * .7f), Offset(left + bodyW * .61f, top + bodyH * .7f), stroke)
            }
            else -> {
                listOf(left + eyeOffset, left + bodyW - eyeOffset).forEach { x ->
                    drawCircle(Preto, bodyW * .095f, Offset(x, eyeY))
                    drawCircle(Branco, bodyW * .027f, Offset(x - bodyW * .025f, eyeY - bodyW * .03f))
                    if (mood == MascotMood.EXCITED) drawCircle(Amarelo, bodyW * .02f, Offset(x + bodyW * .02f, eyeY + bodyW * .025f))
                }
            }
        }

        if (mood == MascotMood.EXCITED) {
            val handSize = Size(bodyW * .34f, bodyH * .38f)
            val handCorner = CornerRadius(corner * .6f)
            val leftHand = Offset(left - bodyW * .34f, top + bodyH * .2f)
            val rightHand = Offset(left + bodyW, top + bodyH * .2f)
            drawRoundRect(Rosa, leftHand, handSize, handCorner)
            drawRoundRect(Preto, leftHand, handSize, handCorner, style = Stroke(stroke))
            drawRoundRect(Amarelo, rightHand, handSize, handCorner)
            drawRoundRect(Preto, rightHand, handSize, handCorner, style = Stroke(stroke))
        }
    }
}
