package com.example.bmicalculator

import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class HistoryActivity : LocalizedActivity() {
    private lateinit var list: ListView
    private lateinit var chart: TrendView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history)
        setupChrome(report = true)
        actionBar?.setTitle(R.string.history_title)

        list = findViewById(R.id.historyList)
        val header = layoutInflater.inflate(R.layout.trend, list, false)
        chart = header.findViewById(R.id.trendChart)
        list.addHeaderView(header, null, false)

        findViewById<View>(R.id.historyRetry).setOnClickListener { loadHistory() }
        loadHistory()
    }

    private fun loadHistory() {
        showStatus(R.string.history_loading)
        lifecycleScope.launch {
            // 打开数据库、查询和换算日期都在后台完成，页面只渲染结果。
            val points = try {
                withContext(Dispatchers.IO) {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = false
                    }
                    BmiDb(applicationContext).use { db ->
                        db.history().map { day -> TrendPoint(requireNotNull(parser.parse(day.date)), day.bmi) }
                    }
                }
            } catch (_: SQLiteException) {
                showStatus(R.string.history_error)
                return@launch
            } catch (_: ParseException) {
                showStatus(R.string.history_error)
                return@launch
            }
            if (points.isEmpty()) showStatus(R.string.history_empty) else showHistory(points)
        }
    }

    private fun showStatus(message: Int) {
        list.isVisible = false
        findViewById<View>(R.id.historyStatus).isVisible = true
        findViewById<TextView>(R.id.historyMessage).setText(message)
        findViewById<View>(R.id.historyLoading).isVisible = message == R.string.history_loading
        findViewById<View>(R.id.historyHint).isVisible = message == R.string.history_empty
        findViewById<View>(R.id.historyRetry).isVisible = message == R.string.history_error
    }

    private fun showHistory(points: List<TrendPoint>) {
        val dates = historyDateFormat(DateFormat.MEDIUM, resources.configuration.locales[0])
        chart.plot(points)
        val first = dates.format(points.first().date)
        chart.contentDescription = if (points.size == 1) getString(R.string.history_chart_one, first)
            else getString(R.string.history_chart_desc, first, dates.format(points.last().date))
        // 图表从旧到新排列，列表将最新数字放在最上面。
        list.adapter = DayAdapter(layoutInflater, points.asReversed(), dates)
        findViewById<View>(R.id.historyStatus).isVisible = false
        list.isVisible = true
    }
}

private class DayAdapter(
    private val inflater: LayoutInflater,
    private val points: List<TrendPoint>,
    private val dates: DateFormat
) : BaseAdapter() {
    override fun getCount() = points.size
    override fun getItem(position: Int) = points[position]
    override fun getItemId(position: Int) = getItem(position).date.time
    override fun areAllItemsEnabled() = false
    override fun isEnabled(position: Int) = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.day, parent, false)
        val point = getItem(position)
        view.findViewById<TextView>(R.id.historyRowDate).text = dates.format(point.date)
        view.findViewById<TextView>(R.id.historyRowBmi).text =
            inflater.context.getString(R.string.bmi_number, point.bmi)
        return view
    }
}
