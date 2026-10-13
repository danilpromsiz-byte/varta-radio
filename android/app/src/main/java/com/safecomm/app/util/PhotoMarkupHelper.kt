package com.safecomm.app.util

import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class NormalizedArrow(
    val startX: Float, // 0.0f .. 1.0f
    val startY: Float,
    val endX: Float,   // 0.0f .. 1.0f
    val endY: Float
)

object PhotoMarkupHelper {

    fun bakeArrows(
        sourceFile: File,
        arrows: List<NormalizedArrow>,
        outputFile: File = sourceFile
    ): File {
        if (arrows.isEmpty() || !sourceFile.exists()) return sourceFile
        val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return sourceFile
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        originalBitmap.recycle()

        val canvas = Canvas(mutableBitmap)
        val w = mutableBitmap.width.toFloat()
        val h = mutableBitmap.height.toFloat()

        val baseStroke = (w * 0.012f).coerceAtLeast(6f)
        val arrowHeadLength = (w * 0.055f).coerceAtLeast(30f)

        // Dark outline paint for maximum contrast on light/dark backgrounds
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 0, 0, 0)
            strokeWidth = baseStroke * 1.8f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // Tactical Neon Yellow / Gold Paint
        val neonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD600")
            strokeWidth = baseStroke
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val headFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD600")
            style = Paint.Style.FILL
        }

        val headOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 0, 0, 0)
            strokeWidth = baseStroke * 0.8f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }

        for (arrow in arrows) {
            val sx = arrow.startX * w
            val sy = arrow.startY * h
            val ex = arrow.endX * w
            val ey = arrow.endY * h

            // 1. Draw shaft outline & neon line
            canvas.drawLine(sx, sy, ex, ey, outlinePaint)
            canvas.drawLine(sx, sy, ex, ey, neonPaint)

            // 2. Compute arrowhead angle
            val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
            val arrowAngle = Math.toRadians(28.0)

            val x1 = (ex - arrowHeadLength * cos(angle - arrowAngle)).toFloat()
            val y1 = (ey - arrowHeadLength * sin(angle - arrowAngle)).toFloat()
            val x2 = (ex - arrowHeadLength * cos(angle + arrowAngle)).toFloat()
            val y2 = (ey - arrowHeadLength * sin(angle + arrowAngle)).toFloat()

            val headPath = Path().apply {
                moveTo(ex, ey)
                lineTo(x1, y1)
                lineTo(x2, y2)
                close()
            }

            canvas.drawPath(headPath, headOutlinePaint)
            canvas.drawPath(headPath, headFillPaint)
        }

        try {
            FileOutputStream(outputFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }
        } catch (_: Exception) {}
        mutableBitmap.recycle()
        return outputFile
    }
}
