package com.taoli.xingqiu.model

data class Record(
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String = "",
    val time: Long = System.currentTimeMillis()
)
