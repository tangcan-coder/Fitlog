package com.fitlog.app.data

import kotlinx.serialization.json.Json

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
        return runCatching { json.decodeFromString(AppData.serializer(), normalized) }.getOrNull()
    }

    /** 解析失败时退回空数据，避免一条脏数据让应用打不开。 */
    fun decodeOrEmpty(text: String): AppData = decode(text) ?: AppData()
}
