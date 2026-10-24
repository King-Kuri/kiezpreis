package de.kiezpreis.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import de.kiezpreis.ui.theme.Kiez
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calm "aurora" backdrop: three large, soft light fields over a deep base. They drift very slowly
 * (60 s cycle); with reduced motion they stand still.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val c = Kiez.colors
    val reduced = Kiez.motion.reduced
    val phase = if (reduced) 0.2f else {
        val t = rememberInfiniteTransition(label = "aurora")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse), label = "phase").value
    }
    val strength = if (c.isDark) 0.30f else 0.55f
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(c.background, c.backgroundDeep, c.background)))
        val w = size.width
        val h = size.height
        val a = phase * 2f * Math.PI.toFloat()
        fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.35f), Color.Transparent),
                    center = Offset(cx, cy), radius = r,
                ),
                radius = r, center = Offset(cx, cy),
            )
        }
        blob(c.aurora1, w * (0.12f + 0.06f * sin(a)), h * (0.08f + 0.03f * cos(a)), w * 0.95f, strength)
        blob(c.aurora2, w * (0.98f - 0.05f * cos(a)), h * (0.30f + 0.04f * sin(a)), w * 0.80f, strength * 0.75f)
        blob(c.aurora3, w * (0.30f + 0.08f * cos(a)), h * (0.92f - 0.03f * sin(a)), w * 0.90f, strength * 0.55f)
    }
}
