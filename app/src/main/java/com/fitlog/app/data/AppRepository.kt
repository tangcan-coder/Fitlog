package com.fitlog.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.fitlogStore: DataStore<Preferences> by preferencesDataStore(name = "fitlog")

/**
 * 全部数据存成一份 JSON 放在 DataStore 里。
 * 个人应用的数据量（几百种食物、几年的记录）远小于需要数据库索引的规模，
 * 一份快照换来的是「备份 / 恢复 / 迁移」全都只是读写一个文件。
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext

    val data: Flow<AppData> = appContext.fitlogStore.data.map { prefs ->
        prefs[KEY]?.let { AppCodec.decodeOrEmpty(it) } ?: AppData()
    }

    /**
     * 原子地读改写。block 返回新数据和一个结果值，结果值会作为返回值透出，
     * 这样界面上的提示语（比如「已记录 3 项」）用的是真实写入后的结果。
     *
     * 解码失败时不会直接拿空数据盖掉存档，而是先把原始内容隔离到
     * `files/quarantine/` 下，保证这份唯一的存档还有捞回来的机会。
     */
    suspend fun <R> mutate(block: (AppData) -> Pair<AppData, R>): R {
        var result: R? = null
        var executed = false

        appContext.fitlogStore.edit { prefs ->
            val raw = prefs[KEY]
            val current = when {
                raw.isNullOrBlank() -> AppData()
                else -> AppCodec.decode(raw) ?: AppData().also {
                    // 存档读不出来：先把原文另存成文件，否则这次写入之后就只剩空数据了
                    quarantine(raw)
                }
            }
            val (next, r) = block(current)
            prefs[KEY] = AppCodec.encode(next)
            result = r
            executed = true
        }

        // edit 的 transform 一定会被调用一次；走到这里仍是 false 说明协程在写入前被取消，
        // 此时没有可透出的结果，直接抛异常比返回一个错的值更容易定位。
        check(executed) { "写入未执行（协程可能已被取消），数据未改动" }

        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    suspend fun replace(data: AppData) {
        appContext.fitlogStore.edit { prefs ->
            prefs[KEY] = AppCodec.encode(data)
        }
    }

    fun exportText(data: AppData): String = AppCodec.encodePretty(data)

    fun parseImport(text: String): AppData? = AppCodec.decodeBackup(text)

    /**
     * 把解不开的存档原文落到 `files/quarantine/`，只保留最近 3 份。
     * 取回方式（不用 root）：
     *   adb exec-out run-as com.fitlog.app cat files/quarantine/fitlog-xxx.json
     * 内容本身就是一份完整备份，修好之后可以直接「我的 → 导入恢复」。
     */
    private fun quarantine(raw: String) {
        runCatching {
            val dir = File(appContext.filesDir, "quarantine").apply { mkdirs() }
            File(dir, "fitlog-${System.currentTimeMillis()}.json").writeText(raw)

            dir.listFiles()
                ?.filter { it.name.endsWith(".json") }
                ?.sortedByDescending { it.name }
                ?.drop(MAX_QUARANTINE)
                ?.forEach { it.delete() }
        }
        // 隔离失败（比如存储写满）不能反过来把正常流程打断，
        // 这种情况下宁可照常写入，也不要让界面卡在一个永远存不进去的状态。
    }

    private companion object {
        val KEY = stringPreferencesKey("data_json")
        const val MAX_QUARANTINE = 3
    }
}
