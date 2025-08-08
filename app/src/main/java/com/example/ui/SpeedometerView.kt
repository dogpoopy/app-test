package com.example.speedtest.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.speedtest.R
import kotlin.math.*

class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 5f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var currentSpeed = 0f
    private val maxSpeed = 500f

    private fun speedToAngle(speed: Float): Float {
        val speedPoints = listOf(0f, 5f, 10f, 25f, 50f, 100f, 200f, 300f, 500f)
        val anglePoints = listOf(0f, 20f, 40f, 65f, 90f, 120f, 145f, 162f, 180f)
        
        for (i in 0 until speedPoints.size - 1) {
            if (speed <= speedPoints[i + 1]) {
                val speedRange = speedPoints[i + 1] - speedPoints[i]
                val angleRange = anglePoints[i + 1] - anglePoints[i]
                val progress = (speed - speedPoints[i]) / speedRange
                return anglePoints[i] + (progress * angleRange)
            }
        }
        return 180f
    }

    fun setSpeed(speed: Float) {
        val newSpeed = speed.coerceIn(0f, maxSpeed)
        val animator = ValueAnimator.ofFloat(currentSpeed, newSpeed)
        animator.duration = 500
        animator.addUpdateListener {
            currentSpeed = it.animatedValue as Float
            invalidate()
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = 60f
        val size = min(width, height).toFloat() - padding * 2

        val centerX = width / 2f
        val centerY = height.toFloat() - padding

        val radius = size / 2

        val arcRect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        arcPaint.color = ContextCompat.getColor(context, R.color.purple_700)
        canvas.drawArc(arcRect, 180f, 180f, false, arcPaint)

        arcPaint.color = ContextCompat.getColor(context, R.color.teal_200)
        val sweepAngle = speedToAngle(currentSpeed)
        canvas.drawArc(arcRect, 180f, sweepAngle, false, arcPaint)

        val needleAngleRad = Math.toRadians(180.0 - sweepAngle.toDouble())
        val needleLength = radius - 40f
        val needleX = (centerX + cos(needleAngleRad) * needleLength).toFloat()
        val needleY = (centerY - sin(needleAngleRad) * needleLength).toFloat()
        canvas.drawLine(centerX, centerY, needleX, needleY, needlePaint)

        val tickValues = listOf(0, 5, 10, 25, 50, 100, 200, 300, 500)
        for (value in tickValues) {
            val tickAngle = speedToAngle(value.toFloat())
            val angle = Math.toRadians(180.0 - tickAngle.toDouble())
            val tickStart = radius + 10f
            val tickEnd = radius + 30f
            val labelDist = radius + 60f

            val startX = (centerX + cos(angle) * tickStart).toFloat()
            val startY = (centerY - sin(angle) * tickStart).toFloat()
            val endX = (centerX + cos(angle) * tickEnd).toFloat()
            val endY = (centerY - sin(angle) * tickEnd).toFloat()

            canvas.drawLine(startX, startY, endX, endY, tickPaint)

            val labelX = (centerX + cos(angle) * labelDist).toFloat()
            val labelY = (centerY - sin(angle) * labelDist).toFloat() + 12f
            canvas.drawText(value.toString(), labelX, labelY, textPaint)
        }
    }
}
