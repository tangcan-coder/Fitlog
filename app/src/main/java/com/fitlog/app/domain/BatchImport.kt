package com.fitlog.app.domain

import com.fitlog.app.data.Food

data class BatchResult(
    val foods: List<Food>,
    val skipped: Int
)

/**
 * 批量导入：一行一条，空格 / 逗号 / 制表符分隔。
 * 名称 能量 蛋白 碳水 脂肪 —— 后面三个可省略。
 * 能量默认按 kcal；写成 `485kj` 就按 kJ 换算（营养表上抄下来的数字可以直接带单位）。
 * 以 # 开头的行当注释跳过，方便粘贴时保留原标题。
 */
object BatchImport {

    fun parse(text: String): BatchResult {
        val foods = mutableListOf<Food>()
        var skipped = 0
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            val parts = line.split(Regex("[\\s,，\\t]+")).filter { it.isNotEmpty() }
            if (parts.size < 2) {
                skipped++
                return@forEach
            }

            val name = parts[0]
            val kcal = parseKcal(parts[1])
            if (kcal == null || kcal <= 0) {
                skipped++
                return@forEach
            }

            foods += Food(
                id = Ids.next(),
                name = name,
                cat = "主食",
                unit = "份",
                kcal = kcal,
                p = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                c = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0,
                fat = parts.getOrNull(4)?.toDoubleOrNull() ?: 0.0,
                alias = Pinyin.initials(name),
                createdAt = System.currentTimeMillis()
            )
        }
        return BatchResult(foods, skipped)
    }

    /**
     * 能量列：裸数字按 kcal（老习惯，不能改，否则旧文本会被当成 kJ 读小四倍）；
     * 带 `kj` 后缀的按 kJ 换算。返回值统一成 kcal。
     */
    private fun parseKcal(raw: String): Double? {
        val text = raw.trim()
        val isKj = text.endsWith("kj", ignoreCase = true)
        val number = (if (isKj) text.dropLast(2) else text).toDoubleOrNull() ?: return null
        if (number <= 0) return null
        return if (isKj) kcalFromKj(number) else number
    }
}

object Ids {
    fun next(): String {
        val time = System.currentTimeMillis().toString(36)
        val rand = (Math.random() * 46_656).toInt().toString(36).padStart(3, '0')
        return time + rand
    }
}
