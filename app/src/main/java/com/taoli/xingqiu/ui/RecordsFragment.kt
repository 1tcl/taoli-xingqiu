package com.taoli.xingqiu.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taoli.xingqiu.MainActivity
import com.taoli.xingqiu.R
import com.taoli.xingqiu.model.Record
import java.text.SimpleDateFormat
import java.util.*

class RecordsFragment : Fragment() {

    private var recordYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var recordMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private lateinit var mfLabel: TextView
    private lateinit var recordsList: RecyclerView
    private lateinit var mfPrev: ImageButton
    private lateinit var mfNext: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_records, container, false)

        mfLabel = view.findViewById(R.id.mf_label)
        recordsList = view.findViewById(R.id.records_list)
        mfPrev = view.findViewById(R.id.mf_prev)
        mfNext = view.findViewById(R.id.mf_next)

        recordsList.layoutManager = LinearLayoutManager(context)

        mfPrev.setOnClickListener {
            if (recordMonth == 1) {
                recordMonth = 12; recordYear--
            } else {
                recordMonth--
            }
            refresh()
        }

        mfNext.setOnClickListener {
            val now = Calendar.getInstance()
            if (recordYear == now.get(Calendar.YEAR) && recordMonth == now.get(Calendar.MONTH) + 1) return@setOnClickListener
            if (recordMonth == 12) {
                recordMonth = 1; recordYear++
            } else {
                recordMonth++
            }
            refresh()
        }

        refresh()
        return view
    }

    fun refresh() {
        val activity = activity as? MainActivity ?: return
        val dbHelper = activity.dbHelper

        mfLabel.text = "${recordYear}年${recordMonth}月"
        val records = dbHelper.getRecordsByMonth(recordYear, recordMonth)
        recordsList.adapter = RecordsAdapter(records, dbHelper, this)
    }

    // ---- Adapter ----
    inner class RecordsAdapter(
        private val records: List<Record>,
        private val dbHelper: com.taoli.xingqiu.data.DatabaseHelper,
        private val fragment: RecordsFragment
    ) : RecyclerView.Adapter<RecordsAdapter.VH>() {

        private val catIcons = mapOf(
            "饭菜钱" to "🍚", "路程篇" to "🚗", "购物消费" to "🛒",
            "娱乐消费" to "🎮", "日常用品" to "📦", "其他" to "📌"
        )
        private val catColors = mapOf(
            "饭菜钱" to "#FFF3E0", "路程篇" to "#E3F2FD", "购物消费" to "#FCE4EC",
            "娱乐消费" to "#F3E5F5", "日常用品" to "#E8F5E9", "其他" to "#F5F5F5"
        )
        private val dateFormat = SimpleDateFormat("M/d HH:mm", Locale.getDefault())

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: TextView = itemView.findViewById(R.id.record_icon)
            val category: TextView = itemView.findViewById(R.id.record_category)
            val note: TextView = itemView.findViewById(R.id.record_note)
            val time: TextView = itemView.findViewById(R.id.record_time)
            val amount: TextView = itemView.findViewById(R.id.record_amount)
            val delete: ImageButton = itemView.findViewById(R.id.record_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_record, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val record = records[position]
            holder.icon.text = catIcons[record.category] ?: "📌"
            val bgStr = catColors[record.category] ?: "#F5F5F5"
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(bgStr))
                cornerRadius = 24f
            }
            holder.icon.background = bg
            holder.category.text = record.category
            if (record.note.isNotEmpty()) {
                holder.note.visibility = View.VISIBLE
                holder.note.text = record.note
            } else {
                holder.note.visibility = View.GONE
            }
            holder.time.text = dateFormat.format(Date(record.time))
            holder.amount.text = "-¥${String.format("%.2f", record.amount)}"

            holder.delete.setOnClickListener {
                AlertDialog.Builder(parent.context)
                    .setTitle("删除记录")
                    .setMessage("确定要删除这条消费记录吗？")
                    .setPositiveButton("删除") { _, _ ->
                        dbHelper.deleteRecord(record.id)
                        fragment.refresh()
                        // Also refresh overview
                        (activity as? MainActivity)?.refreshAllFragments()
                        Toast.makeText(parent.context, "记录已删除", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        override fun getItemCount() = records.size

        private val parent = fragment
    }
}
