package com.example.bmicalculator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal data class TrendPoint(val date: Date, val bmi: Double)

internal fun historyDateFormat(style: Int, locale: Locale): DateFormat =
    DateFormat.getDateInstance(style, locale).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

class TrendView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val ink = context.getColor(R.color.teal)
    private val labels = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.muted)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.muted)
        alpha = 44
        strokeWidth = dp(1)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        style = Paint.Style.STROKE
        strokeWidth = dp(2)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        alpha = 23
    }
    private val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
    private val path = Path()
    private val area = Path()
    private val numberFormat = NumberFormat.getNumberInstance(resources.configuration.locales[0]).apply {
        isGroupingUsed = false
        maximumFractionDigits = 1
    }
    private val dateFormat = historyDateFormat(DateFormat.SHORT, resources.configuration.locales[0])
    private var points = emptyList<TrendPoint>()
    private var ticks = emptyList<Pair<Double, String>>()
    private var startLabel = ""
    private var endLabel = ""

    internal fun plot(data: List<TrendPoint>) {
        points = data
        val min = points.minOf { it.bmi }
        val max = points.maxOf { it.bmi }
        val step = tickStep(((max - min) / 4).coerceAtLeast(0.5))
        val first = floor((min - step / 2) / step).toInt().coerceAtLeast(0)
        val last = ceil((max + step / 2) / step).toInt()
        ticks = (first..last).map { index ->
            val value = index * step
            value to numberFormat.format(value)
        }
        startLabel = dateFormat.format(data.first().date)
        endLabel = dateFormat.format(data.last().date)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.isEmpty()) return

        val labelHeight = labels.descent() - labels.ascent()
        val left = ticks.maxOf { labels.measureText(it.second) } + dp(12)
        val right = width - dp(5)
        val top = labelHeight / 2
        val bottom = height - labelHeight - dp(12)
        if (right <= left || bottom <= top) return

        // 横坐标按真实日期间隔计算，缺少记录的日期之间会留出相应距离。
        val earliest = points.first().date.time
        val duration = points.last().date.time - earliest
        val lower = ticks.first().first
        val upper = ticks.last().first
        fun x(point: TrendPoint) = if (duration == 0L) (left + right) / 2
            else left + (right - left) * ((point.date.time - earliest).toDouble() / duration).toFloat()
        fun y(bmi: Double) = bottom - (bottom - top) * ((bmi - lower) / (upper - lower)).toFloat()

        labels.textAlign = Paint.Align.RIGHT
        val center = (labels.ascent() + labels.descent()) / 2
        for ((value, label) in ticks) {
            val row = y(value)
            canvas.drawLine(left, row, right, row, grid)
            canvas.drawText(label, left - dp(8), row - center, labels)
        }

        if (points.size > 1) {
            path.rewind()
            val first = points.first()
            path.moveTo(x(first), y(first.bmi))
            for (index in 1 until points.size) {
                val point = points[index]
                path.lineTo(x(point), y(point.bmi))
            }
            area.set(path)
            area.lineTo(right, bottom)
            area.lineTo(left, bottom)
            area.close()
            canvas.drawPath(area, fill)
            canvas.drawPath(path, line)
        }
        if (points.size <= 24) {
            for (point in points) canvas.drawCircle(x(point), y(point.bmi), dp(3), marker)
        }
        val latest = points.last()
        canvas.drawCircle(x(latest), y(latest.bmi), dp(4), marker)

        val baseline = bottom + dp(8) - labels.ascent()
        if (points.size == 1) {
            labels.textAlign = Paint.Align.CENTER
            canvas.drawText(startLabel, x(latest), baseline, labels)
        } else {
            val labelsFit = labels.measureText(startLabel) + labels.measureText(endLabel) + dp(8) <= right - left
            if (labelsFit) {
                labels.textAlign = Paint.Align.LEFT
                canvas.drawText(startLabel, left, baseline, labels)
            }
            labels.textAlign = Paint.Align.RIGHT
            canvas.drawText(endLabel, right, baseline, labels)
        }
    }

    private fun tickStep(raw: Double): Double {
        val magnitude = 10.0.pow(floor(log10(raw)))
        return listOf(1.0, 2.0, 5.0, 10.0).first { it * magnitude >= raw } * magnitude
    }

    private fun dp(value: Int): Float = value * resources.displayMetrics.density
}
