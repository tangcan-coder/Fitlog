package com.fitlog.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * 一份数据 ↔ 一段 JSON。抽出来是为了让「导入旧备份」这种容错逻辑能被单元测试覆盖，
 * 不必为了测它去造一个 Android Context。
 */
object AppCodec {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val prettyJson = Json(json) { prettyPrint = true }

    fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)

    fun encodePretty(data: AppData): String = prettyJson.encodeToString(AppData.serializer(), data)

    /**
     * 网页版用 `""` 表示「没填克数」，这里是 null。直接搬过来会让整个文件解析失败，
     * 所以在解析前先把这种空串归一化成 null。
     */
    fun decode(text: String): AppData? {
        val normalized = text.replace("\"gram\":\"\"", "\"gram\":null")
        return runCatching { json.decodeFromString(AppData.serializer(), normalized) }
            .getOrNull()
            ?.let(::migrateCategories)
    }

    /**
     * 分类改名：老存档里的「家常菜」统一迁到「肉类」。
     * 只改食物自己的分类，不动历史记录（记录里存的是当时算好的营养值）。
     */
    private fun migrateCategories(data: AppData): AppData {
        if (data.foods.none { it.cat == LEGACY_MEAT }) return data
        return data.copy(
            foods = data.foods.map { if (it.cat == LEGACY_MEAT) it.copy(cat = "肉类") else it },
        )
    }

    /** 解析失败时退回空数据，避免一条脏数据让应用打不开。 */
    fun decodeOrEmpty(text: String): AppData = decode(text) ?: AppData()

    /**
     * 备份里至少要有一个我们认识的键，才算「像一份 FitLog 备份」。
     * 不能只看「能不能解析」：所有字段都有默认值，`{}` 也能解成一份空数据，
     * 直接拿去 replace 就等于把用户所有记录清空了，还提示「已恢复备份」。
     */
    private val BACKUP_KEYS = setOf("foods", "logs", "weights", "profile", "meta")

    /** 分类改名前的旧值：老存档里的「家常菜」自动迁到「肉类」 */
    private const val LEGACY_MEAT = "家常菜"

    /** 导入专用：既解析得动，也确认它确实是一份备份。 */
    fun decodeBackup(text: String): AppData? {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject
            ?: return null
        if (root.keys.none { it in BACKUP_KEYS }) return null
        return decode(text)
    }
}
