package com.fitlog.app.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 数据模型刻意保持与网页版导出 JSON 的字段一致，
 * 这样以前在浏览器里记录的备份可以直接导入，不用重新录一遍。
 */

@Immutable
@Serializable
data class Food(
    val id: String = "",
    val name: String = "",
    val cat: String = "主食",
    val unit: String = "份",
    val gram: Double? = null,
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val fat: Double = 0.0,
    val alias: String = "",
    val uses: Int = 0,
    val lastUsed: Long = 0L,
    /** 网页版有「隐藏食物」，这里保留字段保证 JSON 往返不丢数据；界面暂时没有入口 */
    val hidden: Boolean = false,
    val createdAt: Long = 0L
)

@Immutable
@Serializable
data class LogEntry(
    val id: String = "",
    val foodId: String = "",
    val name: String = "",
    val unit: String = "份",
    val qty: Double = 1.0,
    val meal: String = Meal.BREAKFAST.id,
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val fat: Double = 0.0,
    val ts: Long = 0L
)

@Immutable
@Serializable
data class WeightLog(
    val date: String,
    val kg: Double
)

@Immutable
@Serializable
data class Profile(
    val sex: String = "male",
    val age: Int = 28,
    val height: Double = 175.0,
    val weight: Double = 70.0,
    val activity: Double = 1.375,
    val goal: String = "cut",
    val rate: Double = 0.5
)

@Immutable
@Serializable
data class Meta(
    val onboarded: Boolean = false,
    val lastBackup: Long? = null,
    /** 留给以后的存档迁移用；现在不变，字段本身是网页版 JSON 的一部分 */
    val version: Int = 1
)

@Immutable
@Serializable
data class AppData(
    val foods: List<Food> = emptyList(),
    val logs: Map<String, List<LogEntry>> = emptyMap(),
    val weights: List<WeightLog> = emptyList(),
    val profile: Profile = Profile(),
    val meta: Meta = Meta()
)

enum class Meal(val id: String, val label: String) {
    BREAKFAST("breakfast", "早餐"),
    LUNCH("lunch", "午餐"),
    DINNER("dinner", "晚餐"),
    SNACK("snack", "加餐");

    companion object {
        val all = entries
        fun of(id: String?): Meal = entries.firstOrNull { it.id == id } ?: BREAKFAST
    }
}

object Cats {
    const val ALL = "全部"
    val list = listOf("主食", "蛋白", "家常菜", "蔬菜", "水果", "饮品", "零食")
    val filters = listOf(ALL) + list

    fun icon(cat: String): String = when (cat) {
        "主食" -> "🍚"
        "蛋白" -> "🥚"
        "家常菜" -> "🍲"
        "蔬菜" -> "🥬"
        "水果" -> "🍎"
        "饮品" -> "☕"
        "零食" -> "🍪"
        else -> "🍽️"
    }
}

data class ActivityLevel(val value: Double, val label: String)

object ActivityLevels {
    val list = listOf(
        ActivityLevel(1.2, "几乎不运动"),
        ActivityLevel(1.375, "每周运动 1–3 次"),
        ActivityLevel(1.55, "每周运动 3–5 次"),
        ActivityLevel(1.725, "每周运动 6–7 次"),
        ActivityLevel(1.9, "体力劳动 / 每天训练")
    )

    fun labelOf(value: Double): String =
        list.firstOrNull { it.value == value }?.label ?: list[1].label
}
