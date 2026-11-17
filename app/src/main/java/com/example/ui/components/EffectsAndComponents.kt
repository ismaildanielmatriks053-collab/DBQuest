package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.RoseLoss
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DifficultyBadge(difficulty: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (difficulty) {
        "Beginner" -> EmeraldWin.copy(alpha = 0.16f) to EmeraldWin
        "Advanced" -> RoseLoss.copy(alpha = 0.16f) to RoseLoss
        else -> AmberGold.copy(alpha = 0.16f) to AmberGold
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = difficulty,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class ConfettiParticle(
    val xFraction: Float,
    val startYFraction: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float,
    val color: Color,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

/**
 * Full-screen Winning Special Effect: layered confetti burst + expanding emerald/gold shockwave rings!
 */
@Composable
fun VictoryFireworksCanvas(modifier: Modifier = Modifier) {
    val particles = remember {
        val palette = listOf(EmeraldWin, CyanPrimary, AmberGold, VioletSecondary, Color.White)
        List(75) { idx ->
            val angle = Random.nextFloat() * 6.28318f
            val speed = 0.18f + Random.nextFloat() * 0.65f
            ConfettiParticle(
                xFraction = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f,
                startYFraction = 0.34f + (Random.nextFloat() - 0.5f) * 0.1f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 0.25f,
                sizeDp = 5f + Random.nextFloat() * 7f,
                color = palette[idx % palette.size],
                rotationSpeed = (Random.nextFloat() - 0.5f) * 360f,
                isCircle = idx % 3 == 0
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
        )
    }

    val infinite = rememberInfiniteTransition(label = "victory_aura")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val center = Offset(size.width * 0.5f, size.height * 0.32f)

        // Ambient emerald/gold radial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    EmeraldWin.copy(alpha = 0.28f * pulse),
                    AmberGold.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.65f * pulse
            ),
            center = center,
            radius = size.minDimension * 0.65f * pulse
        )

        // Expanding shockwave rings on initial burst
        if (t < 0.85f) {
            val ringAlpha = (1f - (t / 0.85f)).coerceIn(0f, 1f) * 0.55f
            drawCircle(
                color = CyanPrimary.copy(alpha = ringAlpha),
                radius = size.minDimension * 0.7f * t,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx() * (1f - t))
            )
            drawCircle(
                color = AmberGold.copy(alpha = ringAlpha * 0.8f),
                radius = size.minDimension * 0.48f * t,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx() * (1f - t))
            )
        }

        // Confetti burst with gravity
        val alpha = if (t < 0.75f) 1f else ((1f - t) / 0.25f).coerceIn(0f, 1f)
        particles.forEach { p ->
            val px = size.width * (p.xFraction + p.vx * t)
            val py = size.height * (p.startYFraction + p.vy * t + 0.55f * t * t)
            val radiusPx = p.sizeDp.dp.toPx()
            if (p.isCircle) {
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = radiusPx * 0.6f,
                    center = Offset(px, py)
                )
            } else {
                rotate(degrees = p.rotationSpeed * t, pivot = Offset(px, py)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(px - radiusPx * 0.5f, py - radiusPx * 0.3f),
                        size = androidx.compose.ui.geometry.Size(radiusPx, radiusPx * 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen Loss Special Effect: crimson/amber warning pulse rings + falling glitch sparks!
 */
@Composable
fun DefeatPulseCanvas(modifier: Modifier = Modifier) {
    val embers = remember {
        List(40) { idx ->
            ConfettiParticle(
                xFraction = Random.nextFloat(),
                startYFraction = 0.05f + Random.nextFloat() * 0.4f,
                vx = (Random.nextFloat() - 0.5f) * 0.08f,
                vy = 0.25f + Random.nextFloat() * 0.45f,
                sizeDp = 3f + Random.nextFloat() * 5f,
                color = if (idx % 2 == 0) RoseLoss else AmberGold,
                rotationSpeed = 0f,
                isCircle = true
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = LinearEasing)
        )
    }

    val infinite = rememberInfiniteTransition(label = "defeat_pulse")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val center = Offset(size.width * 0.5f, size.height * 0.30f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RoseLoss.copy(alpha = glowAlpha),
                    Color.Transparent
                ),
                center = center,
                radius = size.minDimension * 0.7f
            ),
            center = center,
            radius = size.minDimension * 0.7f
        )

        // Alert ripple
        val wave = (t * 2f) % 1f
        drawCircle(
            color = RoseLoss.copy(alpha = (1f - wave) * 0.4f),
            radius = size.minDimension * 0.55f * wave,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )

        // Falling rollback sparks
        val fade = (1f - t * 0.7f).coerceIn(0.2f, 1f)
        embers.forEach { e ->
            val px = size.width * (e.xFraction + e.vx * t)
            val py = size.height * ((e.startYFraction + e.vy * t) % 1f)
            drawCircle(
                color = e.color.copy(alpha = fade * 0.7f),
                radius = e.sizeDp.dp.toPx() * 0.5f,
                center = Offset(px, py)
            )
        }
    }
}

/**
 * Triggers audio fanfare + tactile vibration for Winning or Loss special effects.
 */
suspend fun playOutcomeSpecialFeedback(context: Context, won: Boolean) {
    withContext(Dispatchers.Default) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (won) longArrayOf(0, 70, 50, 110) else longArrayOf(0, 160, 60, 160)
                    val amps = if (won) intArrayOf(0, 140, 0, 220) else intArrayOf(0, 200, 0, 140)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                }
            }
        } catch (_: Exception) {
            // Safe ignore if device lacks vibrator permission/hardware
        }

        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            if (won) {
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 120)
                delay(130)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
                delay(240)
            } else {
                toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 260)
                delay(280)
            }
            toneGen.release()
        } catch (_: Exception) {
            // Safe ignore if audio device is unavailable in headless environment
        }
    }
}
