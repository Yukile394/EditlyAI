package com.editlyai.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private data class SnowFlake(val x: Float, val startY: Float, val speed: Float, val radius: Float, val drift: Float)
private data class StarDot(val x: Float, val y: Float, val brightness: Float, val phase: Float)

/**
 * Ana sayfa arka planı: koyu mavi-mor gradyan üzerinde yavaşça yanıp sönen
 * yıldızlar + yavaşça düşen kar taneleri. Tamamen dekoratif, veriyle
 * ilişkisi yok; performans için sabit sayıda parçacık kullanılıyor ve
 * tek bir animasyon zamanlayıcısı (infiniteTransition) ile sürülüyor.
 */
@Composable
fun SnowBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow_bg")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_time"
    )

    val flakes = remember {
        List(50) {
            SnowFlake(
                x = Random.nextFloat(),
                startY = Random.nextFloat(),
                speed = 0.25f + Random.nextFloat() * 0.6f,
                radius = 1.5f + Random.nextFloat() * 2.5f,
                drift = (Random.nextFloat() - 0.5f) * 0.04f
            )
        }
    }
    val stars = remember {
        List(30) {
            StarDot(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.55f,
                brightness = 0.4f + Random.nextFloat() * 0.6f,
                phase = Random.nextFloat() * 6.28f
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1128), Color(0xFF152A57), Color(0xFF23417F))
                )
            )

            stars.forEach { star ->
                val twinkle = (sin(time * 2 * Math.PI + star.phase).toFloat() + 1f) / 2f
                drawCircle(
                    color = Color.White.copy(alpha = (0.25f + twinkle * 0.6f) * star.brightness),
                    radius = 1.6f,
                    center = Offset(star.x * size.width, star.y * size.height)
                )
            }

            flakes.forEach { flake ->
                val y = ((flake.startY + time * flake.speed) % 1f) * size.height
                val sway = sin(time * 2 * Math.PI + flake.x * 12).toFloat() * flake.drift
                val x = ((flake.x + sway) % 1f).coerceIn(0f, 1f) * size.width
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = flake.radius,
                    center = Offset(x, y)
                )
            }
        }
        content()
    }
}
