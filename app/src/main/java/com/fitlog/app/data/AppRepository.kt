package com.fitlog.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
     */
    suspend fun <R> mutate(block: (AppData) -> Pair<AppData, R>): R {
        var result: R? = null
        appContext.fitlogStore.edit { prefs ->
            val current = prefs[KEY]?.let { AppCodec.decodeOrEmpty(it) } ?: AppData()
            val (next, r) = block(current)
            result = r
            prefs[KEY] = AppCodec.encode(next)
        }
        @Suppress("UNCHECKED_CAST")
        return result as R
    }

    suspend fun replace(data: AppData) {
        appContext.fitlogStore.edit { prefs ->
            prefs[KEY] = AppCodec.encode(data)
        }
    }

    fun exportText(data: AppData): String = AppCodec.encodePretty(data)

    fun parseImport(text: String): AppData? = AppCodec.decode(text)

    private companion object {
        val KEY = stringPreferencesKey("data_json")
    }
}
