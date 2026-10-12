package com.safecomm.app.ui.components

import androidx.compose.animation.core.*
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

    // 3D Convex Dome Gradients (light source at top-left ~35% offset)
    val domeBrush = when {
        isRecording -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF6E6E),
                Color(0xFFE53935),
                Color(0xFFB71C1C),
                Color(0xFF7F0000)
            ),
            center = Offset(130f, 130f),
            radius = 240f
        )
        isReceivingVoice -> Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE082),
                Color(0xFFFFA726),
                Color(0xFFF57C00),
                Color(0xFFBF360C)
            ),
            center = Offset(130f, 130f),
            radius = 240f
        )
        else -> Brush.radialGradient(
            colors = listOf(
                Color(0xFF64B5F6), // Top-left specular highlight
                Color(0xFF2196F3),
                Color(0xFF1976D2),
                Color(0xFF0D47A1)  // Bottom-right shadow rim
            ),
            center = Offset(130f, 130f),
            radius = 240f
        )
    }

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

        // --- LAYER 1: 3D Outer Tactical Bezel Ring (162dp) ---
        Box(
            modifier = Modifier
                .size(162.dp)
                .shadow(
                    elevation = if (isRecording) 6.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = if (isRecording) Color(0xFFE53935) else Color(0xFF0D47A1)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF37474F), // Light bezel top
                            Color(0xFF263238),
                            Color(0xFF10171B)  // Dark bezel bottom
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88FFFFFF), // Top highlight specular edge
                            Color(0x22FFFFFF),
                            Color(0x99000000)  // Bottom shadow edge
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // --- LAYER 2: Inner Convex Dome Button (138dp) ---
            Box(
                modifier = Modifier
                    .size(if (isRecording) 134.dp else 138.dp) // Slight mechanical recession when pressed
                    .shadow(
                        elevation = if (isRecording) 2.dp else 10.dp,
                        shape = CircleShape,
                        spotColor = Color.Black
                    )
                    .clip(CircleShape)
                    .background(domeBrush)
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
                // --- LAYER 3: Specular Glass Reflection Arc (Top dome highlight) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(34.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x55FFFFFF),
                                    Color(0x00FFFFFF)
                                )
                            )
                        )
                )

                // Button Labels & Tactical Icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Микрофон рации",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isRecording -> "В ЭФИРЕ"
                            isReceivingVoice -> "ПРИЁМ"
                            else -> "РАЦИЯ"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = when {
                            isRecording -> "Говорите..."
                            isReceivingVoice -> "Слушайте"
                            else -> "Удерживайте"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
