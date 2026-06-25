package com.taoli.xingqiu.ui

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taoli.xingqiu.MainActivity
import com.taoli.xingqiu.R

class CategoryFragment : Fragment() {

    private lateinit var catList: RecyclerView
    private lateinit var newCatInput: EditText
    private lateinit var btnAddCat: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_categories, container, false)

        catList = view.findViewById(R.id.cat_mgmt_list)
        newCatInput = view.findViewById(R.id.new_cat_input)
        btnAddCat = view.findViewById(R.id.btn_add_cat)

        catList.layoutManager = LinearLayoutManager(context)

        btnAddCat.setOnClickListener { addCategory() }
        newCatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCategory()
                true
            } else false
        }

        refresh()
        return view
    }

    fun refresh() {
        val activity = activity as? MainActivity ?: return
        val categories = activity.getCategories()
        catList.adapter = CategoryAdapter(categories, activity, this)
    }

    private fun addCategory() {
        val activity = activity as? MainActivity ?: return
        val name = newCatInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(context, "请输入类型名称", Toast.LENGTH_SHORT).show()
            return
        }
        val categories = activity.getCategories().toMutableList()
        if (name in categories) {
            Toast.makeText(context, "该类型已存在", Toast.LENGTH_SHORT).show()
            return
        }
        if (categories.size >= 20) {
            Toast.makeText(context, "最多支持20个类型", Toast.LENGTH_SHORT).show()
            return
        }
        categories.add(name)
        activity.saveCategories(categories)
        newCatInput.text.clear()
        refresh()
        Toast.makeText(context, "类型「$name」已添加", Toast.LENGTH_SHORT).show()
    }

    // ---- Adapter ----
    inner class CategoryAdapter(
        private val categories: List<String>,
        private val mainActivity: MainActivity,
        private val fragment: CategoryFragment
    ) : RecyclerView.Adapter<CategoryAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.cm_name)
            val delete: ImageButton = itemView.findViewById(R.id.cm_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val cat = categories[position]
            holder.name.text = cat

            if (categories.size <= 1) {
                holder.delete.isEnabled = false
                holder.delete.alpha = 0.3f
            } else {
                holder.delete.isEnabled = true
                holder.delete.alpha = 1.0f
            }

            holder.delete.setOnClickListener {
                if (categories.size <= 1) {
                    Toast.makeText(parent.context, "至少保留一个类型", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(parent.context)
                    .setTitle("删除类型")
                    .setMessage("确定删除类型「$cat」吗？\n已有的该类型记录不会被删除。")
                    .setPositiveButton("删除") { _, _ ->
                        val updated = categories.toMutableList()
                        updated.remove(cat)
                        mainActivity.saveCategories(updated)
                        fragment.refresh()
                        Toast.makeText(parent.context, "类型「$cat」已删除", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        override fun getItemCount() = categories.size

        private val parent = fragment
    }
}
