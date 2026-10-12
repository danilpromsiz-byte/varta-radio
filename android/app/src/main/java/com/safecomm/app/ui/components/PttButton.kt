package com.safecomm.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.ui.theme.*

/**
 * 3D Convex (Выпуклая) Push-to-Talk Walkie-Talkie Button.
 *
 * Designed with skeuomorphic tactical tactile aesthetics:
 * - Raised outer metallic bezel ring with light/shadow edge beveling.
 * - 3D spherical convex gradient simulating a curved push dome.
 * - Upper specular highlight reflection arc.
 * - Mechanical inset feedback when held down.
 * - Text inside is strictly operational (speaker nickname moved to dedicated HUD banner).
 */
@Composable
fun PttButton(
    isRecording: Boolean,
    amplitude: Float,
    isReceivingVoice: Boolean,
    onStartTalk: () -> Unit,
    onStopTalk: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation when live
    val infiniteTransition = rememberInfiniteTransition(label = "ptt_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentScale = if (isRecording || isReceivingVoice) pulseScale else 1.0f

    // Dynamic amplitude ring scale
    val dynamicRingScale = if (isRecording) {
        1.0f + (amplitude * 0.40f)
    } else {
        1.0f
    }

    // 3D Convex Dome Gradients (light source at top center ~20% from top rim)
    Box(
        modifier = modifier
            .size(174.dp)
            .scale(currentScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing energy aura
        if (isRecording || isReceivingVoice) {
            Box(
                modifier = Modifier
                    .size(174.dp)
                    .scale(dynamicRingScale)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) Color(0x33E53935)
                        else Color(0x33FFA726)
                    )
            )
        }

        // --- LAYER 1: 3D Outer Tactical Bezel Ring (164dp) ---
        Box(
            modifier = Modifier
                .size(164.dp)
                .shadow(
                    elevation = if (isRecording) 6.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = if (isRecording) Color(0xFFE53935) else Color(0xFF0D47A1)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF546E7A), // Crisp metallic bezel top
                            Color(0xFF263238),
                            Color(0xFF0E1418)  // Dark bezel bottom
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEEFFFFFF), // Bright top metallic bezel reflection
                            Color(0x33FFFFFF),
                            Color(0xDD000000)  // Deep bottom shadow edge
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // --- LAYER 2: Inner Convex Dome Button (152dp, seamless convex spherical dome) ---
            Box(
                modifier = Modifier
                    .size(if (isRecording) 146.dp else 152.dp) // Mechanical depression on press
                    .shadow(
                        elevation = if (isRecording) 2.dp else 12.dp,
                        shape = CircleShape,
                        spotColor = Color.Black
                    )
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF), // Pure white razor-sharp top specular edge
                                Color(0x66FFFFFF),
                                Color(0x22FFFFFF),
                                Color(0xAA000000)  // Ambient occlusion at bottom
                            )
                        ),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onStartTalk()
                                tryAwaitRelease()
                                onStopTalk()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // 3D Spherical Convex Dome Graphics
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerX = w * 0.5f
                    val lightY = h * 0.32f
                    val domeRadius = w * 0.68f

                    // 1. 3D Spherical Convex Radial Base (Light radiating from top-center)
                    val domeColors = when {
                        isRecording -> listOf(
                            Color(0xFFFF8A80), // Brilliant top convex crest
                            Color(0xFFFF5252),
                            Color(0xFFD32F2F),
                            Color(0xFF850000),
                            Color(0xFF3B0000)  // Deep bottom convex shadow
                        )
                        isReceivingVoice -> listOf(
                            Color(0xFFFFF59D), // Brilliant top convex crest
                            Color(0xFFFFD54F),
                            Color(0xFFFFA726),
                            Color(0xFFE65100),
                            Color(0xFF5D1A00)  // Deep bottom convex shadow
                        )
                        else -> listOf(
                            Color(0xFFB3E5FC), // Brilliant top convex crest
                            Color(0xFF4FC3F7),
                            Color(0xFF1976D2),
                            Color(0xFF0D47A1),
                            Color(0xFF031B4D)  // Deep bottom convex shadow
                        )
                    }

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = domeColors,
                            center = Offset(centerX, lightY),
                            radius = domeRadius
                        ),
                        radius = w / 2f,
                        center = Offset(w / 2f, h / 2f)
                    )

                    // 2. Full-perimeter Ambient Occlusion Edge Bevel (Darkens 360-deg rim so dome physically pops out)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color(0x33000000),
                                Color(0x88000000)
                            ),
                            center = Offset(w / 2f, h / 2f),
                            radius = w / 2f
                        ),
                        radius = w / 2f,
                        center = Offset(w / 2f, h / 2f)
                    )

                    // 3. 3D Specular Highlight Bulb at top (Luminous spherical gloss)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xDDFFFFFF),
                                Color(0x66FFFFFF),
                                Color(0x18FFFFFF),
                                Color.Transparent
                            ),
                            center = Offset(centerX, lightY),
                            radius = w * 0.36f
                        ),
                        radius = w * 0.36f,
                        center = Offset(centerX, lightY)
                    )

                    // 4. Razor-sharp upper rim light bevel arc along top contour
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xEEFFFFFF),
                                Color(0x55FFFFFF),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h * 0.30f
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 2.5f * density)
                    )
                }

                // Button Labels & Tactical Icon
                val context = androidx.compose.ui.platform.LocalContext.current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "PTT Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isRecording -> com.safecomm.app.ui.i18n.I18n.t(context, "В ЭФИРЕ", "В ЕФІРІ", "TRANSMIT")
                            isReceivingVoice -> com.safecomm.app.ui.i18n.I18n.t(context, "ПРИЁМ", "ПРИЙОМ", "RECEIVING")
                            else -> com.safecomm.app.ui.i18n.I18n.t(context, "РАЦИЯ", "РАЦІЯ", "RADIO")
                        },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = when {
                            isRecording -> com.safecomm.app.ui.i18n.I18n.t(context, "Говорите...", "Говоріть...", "Speaking...")
                            isReceivingVoice -> com.safecomm.app.ui.i18n.I18n.t(context, "Слушайте", "Слухайте", "Listening")
                            else -> com.safecomm.app.ui.i18n.I18n.t(context, "Удерживайте", "Утримуйте", "Hold to talk")
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
