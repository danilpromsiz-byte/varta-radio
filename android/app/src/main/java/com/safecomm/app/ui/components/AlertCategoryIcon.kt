package com.safecomm.app.ui.components

import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safecomm.app.data.model.AlertType

/**
 * Renders the category icon.
 * For [AlertType.MILITARY] ("Опасная зона"), draws a custom Waze-style
 * turtle with a spotted military camouflage shell ("черепаха в пятнистой защитной расцветке").
 */
@Composable
fun AlertCategoryIcon(
    type: AlertType,
    fontSize: TextUnit = 22.sp,
    modifier: Modifier = Modifier
) {
    if (type == AlertType.MILITARY) {
        val sizeDp: Dp = (fontSize.value * 1.25f).dp
        CamoTurtleIcon(size = sizeDp, modifier = modifier)
    } else {
        Text(
            text = type.icon,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}

@Composable
fun CamoTurtleIcon(
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.48f
        val cy = h * 0.54f
        val s = minOf(w, h)

        val outlineColor = Color(0xFF1A2210)
        val skinDark = Color(0xFF556B2F)
        val skinBright = Color(0xFF789C30)
        val shellBase = Color(0xFF4B5320)
        val camoKhaki = Color(0xFFC3B078)
        val camoForest = Color(0xFF263318)
        val camoBrown = Color(0xFF6E4726)
        val camoOlive = Color(0xFF8FA348)

        // 1. Tail
        val tailPath = Path().apply {
            moveTo(cx - 0.34f * s, cy + 0.10f * s)
            lineTo(cx - 0.45f * s, cy + 0.15f * s)
            lineTo(cx - 0.32f * s, cy + 0.17f * s)
            close()
        }
        drawPath(tailPath, skinDark)

        // 2. Four Legs
        drawOval(
            color = skinDark,
            topLeft = Offset(cx - 0.32f * s, cy + 0.14f * s),
            size = Size(0.15f * s, 0.18f * s)
        )
        drawOval(
            color = skinBright,
            topLeft = Offset(cx - 0.15f * s, cy + 0.16f * s),
            size = Size(0.15f * s, 0.19f * s)
        )
        drawOval(
            color = skinDark,
            topLeft = Offset(cx + 0.02f * s, cy + 0.16f * s),
            size = Size(0.15f * s, 0.19f * s)
        )
        drawOval(
            color = skinBright,
            topLeft = Offset(cx + 0.16f * s, cy + 0.14f * s),
            size = Size(0.15f * s, 0.18f * s)
        )

        // 3. Head peeking out on the right
        drawOval(
            color = skinBright,
            topLeft = Offset(cx + 0.22f * s, cy - 0.12f * s),
            size = Size(0.28f * s, 0.24f * s)
        )
        drawOval(
            color = outlineColor,
            topLeft = Offset(cx + 0.22f * s, cy - 0.12f * s),
            size = Size(0.28f * s, 0.24f * s),
            style = Stroke(width = s * 0.035f)
        )
        // Eye
        drawCircle(
            color = Color.White,
            radius = 0.045f * s,
            center = Offset(cx + 0.39f * s, cy - 0.03f * s)
        )
        drawCircle(
            color = Color.Black,
            radius = 0.024f * s,
            center = Offset(cx + 0.405f * s, cy - 0.03f * s)
        )

        // 4. Dome Shell Path
        val shellPath = Path().apply {
            moveTo(cx - 0.38f * s, cy + 0.16f * s)
            cubicTo(
                cx - 0.40f * s, cy - 0.34f * s,
                cx + 0.30f * s, cy - 0.34f * s,
                cx + 0.28f * s, cy + 0.16f * s
            )
            close()
        }

        // Fill base shell
        drawPath(shellPath, shellBase)

        // Clip camouflage spots inside shell
        clipPath(shellPath) {
            // Khaki spot top-left
            drawOval(
                color = camoKhaki,
                topLeft = Offset(cx - 0.28f * s, cy - 0.18f * s),
                size = Size(0.24f * s, 0.16f * s)
            )
            // Dark forest spot top-right
            drawOval(
                color = camoForest,
                topLeft = Offset(cx - 0.02f * s, cy - 0.22f * s),
                size = Size(0.25f * s, 0.17f * s)
            )
            // Woodland brown spot center
            drawOval(
                color = camoBrown,
                topLeft = Offset(cx - 0.14f * s, cy - 0.04f * s),
                size = Size(0.26f * s, 0.16f * s)
            )
            // Khaki spot bottom-right
            drawOval(
                color = camoKhaki,
                topLeft = Offset(cx + 0.08f * s, cy - 0.02f * s),
                size = Size(0.22f * s, 0.16f * s)
            )
            // Forest spot bottom-left
            drawOval(
                color = camoForest,
                topLeft = Offset(cx - 0.36f * s, cy + 0.01f * s),
                size = Size(0.22f * s, 0.15f * s)
            )
            // Olive spot top
            drawOval(
                color = camoOlive,
                topLeft = Offset(cx - 0.12f * s, cy - 0.28f * s),
                size = Size(0.18f * s, 0.12f * s)
            )
        }

        // Shell Rim & Outline
        drawRoundRect(
            color = camoBrown,
            topLeft = Offset(cx - 0.39f * s, cy + 0.11f * s),
            size = Size(0.68f * s, 0.07f * s),
            cornerRadius = CornerRadius(0.04f * s, 0.04f * s)
        )
        drawPath(
            path = shellPath,
            color = outlineColor,
            style = Stroke(width = s * 0.045f)
        )
    }
}

/**
 * Draws the spotted camouflage turtle directly onto an Android [AndroidCanvas] for OSM map markers.
 */
fun drawCamoTurtleOnAndroidCanvas(canvas: AndroidCanvas, cx: Float, cy: Float, size: Float) {
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    val s = size

    // Subtle drop shadow under the turtle
    paint.style = AndroidPaint.Style.FILL
    paint.color = AndroidColor.argb(90, 0, 0, 0)
    canvas.drawOval(RectF(cx - 0.42f * s, cy + 0.22f * s, cx + 0.42f * s, cy + 0.36f * s), paint)

    // 1. Legs
    paint.color = AndroidColor.parseColor("#556B2F")
    canvas.drawOval(RectF(cx - 0.32f * s, cy + 0.12f * s, cx - 0.17f * s, cy + 0.30f * s), paint)
    canvas.drawOval(RectF(cx + 0.02f * s, cy + 0.14f * s, cx + 0.17f * s, cy + 0.32f * s), paint)

    paint.color = AndroidColor.parseColor("#789C30")
    canvas.drawOval(RectF(cx - 0.15f * s, cy + 0.14f * s, cx + 0.0f * s, cy + 0.32f * s), paint)
    canvas.drawOval(RectF(cx + 0.16f * s, cy + 0.12f * s, cx + 0.31f * s, cy + 0.30f * s), paint)

    // 2. Head
    val headRect = RectF(cx + 0.20f * s, cy - 0.12f * s, cx + 0.48f * s, cy + 0.12f * s)
    paint.style = AndroidPaint.Style.FILL
    paint.color = AndroidColor.parseColor("#789C30")
    canvas.drawOval(headRect, paint)

    paint.style = AndroidPaint.Style.STROKE
    paint.strokeWidth = s * 0.04f
    paint.color = AndroidColor.parseColor("#1A2210")
    canvas.drawOval(headRect, paint)

    // Eye
    paint.style = AndroidPaint.Style.FILL
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx + 0.38f * s, cy - 0.02f * s, 0.045f * s, paint)
    paint.color = AndroidColor.BLACK
    canvas.drawCircle(cx + 0.395f * s, cy - 0.02f * s, 0.024f * s, paint)

    // 3. Dome Shell
    val shellPath = AndroidPath().apply {
        moveTo(cx - 0.38f * s, cy + 0.16f * s)
        cubicTo(
            cx - 0.40f * s, cy - 0.34f * s,
            cx + 0.30f * s, cy - 0.34f * s,
            cx + 0.28f * s, cy + 0.16f * s
        )
        close()
    }

    paint.style = AndroidPaint.Style.FILL
    paint.color = AndroidColor.parseColor("#4B5320")
    canvas.drawPath(shellPath, paint)

    // Clip to shell and draw camo spots
    canvas.save()
    canvas.clipPath(shellPath)

    paint.color = AndroidColor.parseColor("#C3B078") // Khaki
    canvas.drawOval(RectF(cx - 0.28f * s, cy - 0.18f * s, cx - 0.04f * s, cy - 0.02f * s), paint)
    canvas.drawOval(RectF(cx + 0.08f * s, cy - 0.02f * s, cx + 0.30f * s, cy + 0.14f * s), paint)

    paint.color = AndroidColor.parseColor("#263318") // Forest
    canvas.drawOval(RectF(cx - 0.02f * s, cy - 0.22f * s, cx + 0.23f * s, cy - 0.05f * s), paint)
    canvas.drawOval(RectF(cx - 0.36f * s, cy + 0.01f * s, cx - 0.14f * s, cy + 0.16f * s), paint)

    paint.color = AndroidColor.parseColor("#6E4726") // Brown
    canvas.drawOval(RectF(cx - 0.14f * s, cy - 0.04f * s, cx + 0.12f * s, cy + 0.12f * s), paint)

    paint.color = AndroidColor.parseColor("#8FA348") // Light Olive
    canvas.drawOval(RectF(cx - 0.12f * s, cy - 0.28f * s, cx + 0.06f * s, cy - 0.16f * s), paint)

    canvas.restore()

    // Shell rim
    paint.style = AndroidPaint.Style.FILL
    paint.color = AndroidColor.parseColor("#6E4726")
    canvas.drawRoundRect(RectF(cx - 0.39f * s, cy + 0.11f * s, cx + 0.29f * s, cy + 0.18f * s), 0.04f * s, 0.04f * s, paint)

    // Shell outline
    paint.style = AndroidPaint.Style.STROKE
    paint.strokeWidth = s * 0.045f
    paint.color = AndroidColor.parseColor("#1A2210")
    canvas.drawPath(shellPath, paint)
}
