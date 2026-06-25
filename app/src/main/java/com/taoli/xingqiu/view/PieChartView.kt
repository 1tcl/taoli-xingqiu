package com.taoli.xingqiu.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

data class PieSlice(
    val label: String,
    val value: Float,
    val color: Int
)

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#2D2D2D")
    }
    private val slices = mutableListOf<PieSlice>()
    private val colors = listOf(
        Color.parseColor("#FF6B35"),
        Color.parseColor("#FF8C5A"),
        Color.parseColor("#FF4757"),
        Color.parseColor("#FFA502"),
        Color.parseColor("#2ED573"),
        Color.parseColor("#1E90FF"),
        Color.parseColor("#A855F7"),
        Color.parseColor("#06D6A0")
    )

    fun setData(data: Map<String, Double>) {
        slices.clear()
        var i = 0
        for ((label, value) in data) {
            slices.add(PieSlice(label, value.toFloat(), colors[i % colors.size]))
            i++
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - 10f
        val innerRadius = radius * 0.45f

        if (slices.isEmpty() || slices.sumOf { it.value.toDouble() } == 0.0) {
            // Empty state
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawCircle(cx, cy, radius, paint)

            paint.color = Color.parseColor("#FAFAFA")
            canvas.drawCircle(cx, cy, innerRadius, paint)

            textPaint.textSize = 28f
            textPaint.color = Color.parseColor("#999999")
            canvas.drawText("暂无", cx, cy - 4, textPaint)
            textPaint.textSize = 24f
            canvas.drawText("数据", cx, cy + 22, textPaint)
            return
        }

        val total = slices.sumOf { it.value.toDouble() }.toFloat()

        var startAngle = -90f
        for (slice in slices) {
            val sweepAngle = (slice.value / total) * 360f

            // Draw slice
            paint.style = Paint.Style.FILL
            paint.color = slice.color
            val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)

            // Draw percentage text on slice
            val percent = (slice.value / total * 100).toInt()
            if (percent >= 8) {
                val midAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val textR = radius * 0.7f
                val tx = cx + textR * Math.cos(midAngle).toFloat()
                val ty = cy + textR * Math.sin(midAngle).toFloat()
                textPaint.textSize = 22f
                textPaint.color = Color.WHITE
                canvas.drawText("$percent%", tx, ty + 8, textPaint)
            }

            startAngle += sweepAngle
        }

        // Draw center circle (donut hole)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, innerRadius, paint)

        // Draw center text
        textPaint.textSize = 26f
        textPaint.color = Color.parseColor("#2D2D2D")
        canvas.drawText("总消费", cx, cy - 6, textPaint)
        textPaint.textSize = 28f
        textPaint.isFakeBoldText = true
        canvas.drawText(String.format("%.0f", total), cx, cy + 24, textPaint)
        textPaint.isFakeBoldText = false
    }
}
