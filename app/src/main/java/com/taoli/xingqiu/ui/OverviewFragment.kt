package com.taoli.xingqiu.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taoli.xingqiu.MainActivity
import com.taoli.xingqiu.R
import com.taoli.xingqiu.view.PieChartView

class OverviewFragment : Fragment() {

    private var currentPeriod = "day"
    private lateinit var totalLabel: TextView
    private lateinit var totalAmount: TextView
    private lateinit var catList: RecyclerView
    private lateinit var pieChart: PieChartView
    private lateinit var tabDay: TextView
    private lateinit var tabMonth: TextView
    private lateinit var tabYear: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_overview, container, false)

        totalLabel = view.findViewById(R.id.total_label)
        totalAmount = view.findViewById(R.id.total_amount)
        catList = view.findViewById(R.id.cat_breakdown_list)
        pieChart = view.findViewById(R.id.pie_chart)
        tabDay = view.findViewById(R.id.tab_day)
        tabMonth = view.findViewById(R.id.tab_month)
        tabYear = view.findViewById(R.id.tab_year)

        catList.layoutManager = LinearLayoutManager(context)

        setupPeriodTabs()
        refresh()

        return view
    }

    private fun setupPeriodTabs() {
        val tabs = listOf(tabDay to "day", tabMonth to "month", tabYear to "year")
        for ((tab, period) in tabs) {
            tab.setOnClickListener {
                currentPeriod = period
                updateTabStyles()
                refresh()
            }
        }
        updateTabStyles()
    }

    private fun updateTabStyles() {
        val allTabs = listOf(tabDay, tabMonth, tabYear)
        val periods = listOf("day", "month", "year")
        for (i in allTabs.indices) {
            if (periods[i] == currentPeriod) {
                allTabs[i].setBackgroundColor(Color.parseColor("#FF6B35"))
                allTabs[i].setTextColor(Color.WHITE)
            } else {
                allTabs[i].setBackgroundColor(Color.TRANSPARENT)
                allTabs[i].setTextColor(Color.parseColor("#999999"))
            }
        }
    }

    fun refresh() {
        val activity = activity as? MainActivity ?: return
        val dbHelper = activity.dbHelper

        val records = when (currentPeriod) {
            "day" -> dbHelper.getTodayRecords()
            "month" -> {
                val cal = java.util.Calendar.getInstance()
                dbHelper.getRecordsByMonth(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
            }
            "year" -> dbHelper.getYearRecords()
            else -> dbHelper.getTodayRecords()
        }

        val labels = mapOf("day" to "今日总消费", "month" to "本月总消费", "year" to "本年总消费")
        totalLabel.text = labels[currentPeriod] ?: "总消费"

        val total = records.sumOf { it.amount }
        totalAmount.text = String.format("%.2f", total)

        val catSums = dbHelper.getCategorySums(records)
        catList.adapter = CategoryBreakdownAdapter(catSums, total)
        pieChart.setData(catSums)
    }

    // ---- Adapter ----
    inner class CategoryBreakdownAdapter(
        private val catSums: Map<String, Double>,
        private val total: Double
    ) : RecyclerView.Adapter<CategoryBreakdownAdapter.VH>() {

        private val entries = catSums.entries.toList().sortedByDescending { it.value }
        private val colors = listOf(
            Color.parseColor("#FF6B35"), Color.parseColor("#FF8C5A"),
            Color.parseColor("#FF4757"), Color.parseColor("#FFA502"),
            Color.parseColor("#2ED573"), Color.parseColor("#1E90FF"),
            Color.parseColor("#A855F7"), Color.parseColor("#06D6A0")
        )

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val dot: View = itemView.findViewById(R.id.cat_dot)
            val name: TextView = itemView.findViewById(R.id.cat_name)
            val amount: TextView = itemView.findViewById(R.id.cat_amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflater = LayoutInflater.from(parent.context)
            // Inflate a simple layout programmatically
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 24, 0, 24)
            }
            val dot = View(parent.context).apply {
                id = R.id.cat_dot
                val size = 20
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }
            }
            val name = TextView(parent.context).apply {
                id = R.id.cat_name
                textSize = 15f
                setTextColor(Color.parseColor("#2D2D2D"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 20
                }
            }
            val amount = TextView(parent.context).apply {
                id = R.id.cat_amount
                textSize = 16f
                setTextColor(Color.parseColor("#FF6B35"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            row.addView(dot)
            row.addView(name)
            row.addView(amount)
            return VH(row)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (category, value) = entries[position]
            val color = colors[position % colors.size]
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(color)
                shape = android.graphics.drawable.GradientDrawable.OVAL
            }
            holder.dot.background = bg
            holder.name.text = category
            val percent = if (total > 0) (value / total * 100) else 0.0
            holder.amount.text = String.format("%.2f (%.1f%%)", value, percent)
        }

        override fun getItemCount() = entries.size
    }
}
