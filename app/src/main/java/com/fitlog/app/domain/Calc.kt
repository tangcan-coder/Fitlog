package com.fitlog.app.domain

import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Profile

data class Targets(
    val bmr: Int,
    val tdee: Int,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

data class Totals(
    val kcal: Int = 0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

/**
 * Mifflin-St Jeor 公式算基础代谢，乘活动系数得到每日总消耗，
 * 再按“每周想变化多少公斤”折算成每天的热量缺口（1 kg 脂肪 ≈ 7700 kcal）。
 */
fun targetsOf(p: Profile): Targets {
    val bmr = 10 * p.weight + 6.25 * p.height - 5 * p.age + if (p.sex == "male") 5 else -161
    val tdee = bmr * p.activity
    val delta = when (p.goal) {
        "cut" -> -Math.round(7700 * p.rate / 7.0)
        "bulk" -> Math.round(7700 * p.rate / 7.0)
        else -> 0L
    }
    var kcal = Math.round((tdee + delta) / 10.0) * 10
    if (kcal < 1000) kcal = 1000

    val proteinPerKg = when (p.goal) {
        "cut" -> 2.0
        "bulk" -> 1.8
        else -> 1.6
    }
    val protein = Math.round(p.weight * proteinPerKg)
    val fat = Math.round(maxOf(p.weight * 0.8, kcal * 0.25 / 9))
    val carbs = maxOf(0L, Math.round((kcal - protein * 4 - fat * 9) / 4.0))

    return Targets(
        bmr = Math.round(bmr).toInt(),
        tdee = Math.round(tdee).toInt(),
        kcal = kcal.toInt(),
        protein = protein.toInt(),
        carbs = carbs.toInt(),
        fat = fat.toInt()
    )
}

fun totalsOf(entries: List<LogEntry>): Totals {
    var kcal = 0.0
    var p = 0.0
    var c = 0.0
    var f = 0.0
    for (e in entries) {
        kcal += e.kcal
        p += e.p
        c += e.c
        f += e.fat
    }
    return Totals(round0(kcal), round1(p), round1(c), round1(f))
}

fun round1(v: Double): Double = Math.round(v * 10) / 10.0
fun round0(v: Double): Int = Math.round(v).toInt()

/** 供界面直接显示的整数字符串，避免出现 “230.0” 这种 */
fun Double.pretty(): String {
    val r = round1(this)
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}
