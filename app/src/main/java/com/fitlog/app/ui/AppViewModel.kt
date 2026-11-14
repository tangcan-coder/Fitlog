package com.fitlog.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fitlog.app.data.AppData
import com.fitlog.app.data.AppRepository
import com.fitlog.app.data.Cats
import com.fitlog.app.data.Food
import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Meal
import com.fitlog.app.data.Meta
import com.fitlog.app.data.Profile
import com.fitlog.app.data.WeightLog
import com.fitlog.app.domain.BatchImport
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.Ids
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.domain.pretty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Tab(val label: String) {
    TODAY("今日"),
    LIBRARY("食物库"),
    STATS("统计"),
    ME("我的")
}

data class SelectState(
    val meal: String = Meal.BREAKFAST.id,
    val date: String = Dates.today(),
    val query: String = "",
    val cat: String = Cats.ALL,
    val qty: Map<String, Int> = emptyMap()
) {
    val totalCount: Int get() = qty.values.sum()
}

data class PerServing(
    val kcal: Double,
    val p: Double,
    val c: Double,
    val fat: Double
)

sealed interface Sheet {
    data class Select(val state: SelectState) : Sheet

    data class FoodForm(
        val foodId: String? = null,
        val name: String = "",
        val cat: String = "主食",
        val unit: String = "份",
        val gram: String = "",
        val kcal: String = "",
        val p: String = "",
        val c: String = "",
        val fat: String = "",
        /** 从「选择食物」页新建时带上原状态，保存后回到那一页并直接加进这一餐 */
        val returnTo: SelectState? = null
    ) : Sheet {
        val isNew: Boolean get() = foodId == null
    }

    data class Entry(
        val date: String,
        val entryId: String,
        val name: String,
        val per: PerServing,
        val qty: Double
    ) : Sheet

    data object Batch : Sheet
    data object Profile : Sheet
}

data class ToastMsg(val id: Long, val text: String)

data class UiState(
    val tab: Tab = Tab.TODAY,
    val date: String = Dates.today(),
    val sheet: Sheet? = null,
    val message: ToastMsg? = null
)

data class AppState(
    /** 首次读取本地数据完成前为 false，避免用默认值误判「没填过资料」 */
    val loaded: Boolean = false,
    val data: AppData = AppData()
)

class AppViewModel(private val repo: AppRepository) : ViewModel() {

    val state: StateFlow<AppState> = repo.data
        .map { AppState(loaded = true, data = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /* ---------------- 导航 ---------------- */

    fun selectTab(tab: Tab) {
        _ui.value = _ui.value.copy(tab = tab)
    }

    fun setDate(date: String) {
        _ui.value = _ui.value.copy(date = date)
    }

    fun shiftDate(days: Long) {
        setDate(Dates.shift(_ui.value.date, days))
    }

    fun toast(text: String) {
        _ui.value = _ui.value.copy(message = ToastMsg(System.currentTimeMillis(), text))
    }

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    fun closeSheet() {
        _ui.value = _ui.value.copy(sheet = null)
    }

    /* ---------------- 选择食物 ---------------- */

    fun openSelect(meal: String, date: String = _ui.value.date) {
        _ui.value = _ui.value.copy(sheet = Sheet.Select(SelectState(meal = meal, date = date)))
    }

    fun setSelectQuery(q: String) = updateSelect { it.copy(query = q) }

    fun setSelectCat(cat: String) = updateSelect { it.copy(cat = cat) }

    fun changeQty(foodId: String, delta: Int) = updateSelect { st ->
        val next = (st.qty[foodId] ?: 0) + delta
        val map = st.qty.toMutableMap()
        if (next <= 0) map.remove(foodId) else map[foodId] = next
        st.copy(qty = map)
    }

    private fun updateSelect(block: (SelectState) -> SelectState) {
        val cur = _ui.value
        val sheet = cur.sheet
        if (sheet is Sheet.Select) {
            _ui.value = cur.copy(sheet = Sheet.Select(block(sheet.state)))
        }
    }

    fun commitSelection() {
        val sheet = _ui.value.sheet as? Sheet.Select ?: return
        val st = sheet.state
        val chosen = st.qty.filterValues { it > 0 }
        if (chosen.isEmpty()) {
            toast("还没选食物")
            return
        }
        val foods = state.value.data.foods.associateBy { it.id }
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            val count = repo.mutate { d ->
                val list = (d.logs[st.date] ?: emptyList()).toMutableList()
                var added = 0
                chosen.forEach { (foodId, n) ->
                    val f = foods[foodId] ?: return@forEach
                    list += LogEntry(
                        id = Ids.next(),
                        foodId = f.id,
                        name = f.name,
                        unit = f.unit,
                        qty = n.toDouble(),
                        meal = st.meal,
                        kcal = f.kcal * n,
                        p = f.p * n,
                        c = f.c * n,
                        fat = f.fat * n,
                        ts = now
                    )
                    added++
                }
                val newFoods = d.foods.map { f ->
                    if (chosen.containsKey(f.id)) {
                        f.copy(uses = f.uses + 1, lastUsed = now)
                    } else {
                        f
                    }
                }
                d.copy(logs = d.logs + (st.date to list), foods = newFoods) to added
            }
            closeSheet()
            toast("已记录 $count 项")
        }
    }

    /* ---------------- 食物库 ---------------- */

    fun openNewFood(returnTo: SelectState? = null) {
        _ui.value = _ui.value.copy(sheet = Sheet.FoodForm(returnTo = returnTo))
    }

    fun openEditFood(id: String) {
        val f = state.value.data.foods.firstOrNull { it.id == id } ?: return
        _ui.value = _ui.value.copy(
            sheet = Sheet.FoodForm(
                foodId = f.id,
                name = f.name,
                cat = f.cat,
                unit = f.unit,
                gram = f.gram?.pretty()?.takeIf { it != "0" } ?: "",
                kcal = f.kcal.pretty(),
                p = f.p.pretty(),
                c = f.c.pretty(),
                fat = f.fat.pretty()
            )
        )
    }

    fun updateFoodForm(block: (Sheet.FoodForm) -> Sheet.FoodForm) {
        val cur = _ui.value
        val form = cur.sheet as? Sheet.FoodForm ?: return
        _ui.value = cur.copy(sheet = block(form))
    }

    fun saveFoodForm() {
        val form = _ui.value.sheet as? Sheet.FoodForm ?: return
        val name = form.name.trim()
        if (name.isEmpty()) {
            toast("先给食物起个名字")
            return
        }
        val kcal = form.kcal.toDoubleOrNull() ?: 0.0
        val gram = form.gram.toDoubleOrNull()
        val alias = Pinyin.initials(name)

        viewModelScope.launch {
            var newId: String? = null
            repo.mutate { d ->
                if (form.foodId == null) {
                    val food = Food(
                        id = Ids.next(),
                        name = name,
                        cat = form.cat,
                        unit = form.unit.ifBlank { "份" },
                        gram = gram,
                        kcal = kcal,
                        p = form.p.toDoubleOrNull() ?: 0.0,
                        c = form.c.toDoubleOrNull() ?: 0.0,
                        fat = form.fat.toDoubleOrNull() ?: 0.0,
                        alias = alias,
                        createdAt = System.currentTimeMillis()
                    )
                    newId = food.id
                    d.copy(foods = d.foods + food) to Unit
                } else {
                    val updated = d.foods.map { f ->
                        if (f.id == form.foodId) {
                            f.copy(
                                name = name,
                                cat = form.cat,
                                unit = form.unit.ifBlank { "份" },
                                gram = gram,
                                kcal = kcal,
                                p = form.p.toDoubleOrNull() ?: 0.0,
                                c = form.c.toDoubleOrNull() ?: 0.0,
                                fat = form.fat.toDoubleOrNull() ?: 0.0,
                                alias = alias
                            )
                        } else {
                            f
                        }
                    }
                    d.copy(foods = updated) to Unit
                }
            }

            val back = form.returnTo
            if (back != null && newId != null) {
                // 新建完直接回到选择页，并把这份食物算作已选 1 份
                _ui.value = _ui.value.copy(
                    sheet = Sheet.Select(
                        back.copy(
                            query = "",
                            qty = back.qty + (newId!! to (back.qty[newId!!] ?: 0) + 1)
                        )
                    )
                )
                toast("已加入这一餐")
            } else {
                closeSheet()
                toast("已保存")
            }
        }
    }

    fun deleteFood(id: String) {
        viewModelScope.launch {
            repo.mutate { d -> d.copy(foods = d.foods.filterNot { it.id == id }) to Unit }
            closeSheet()
            toast("已删除")
        }
    }

    fun openBatch() {
        _ui.value = _ui.value.copy(sheet = Sheet.Batch)
    }

    fun commitBatch(text: String) {
        val result = BatchImport.parse(text)
        if (result.foods.isEmpty()) {
            toast("没读到有效内容，一行一条试试")
            return
        }
        viewModelScope.launch {
            repo.mutate { d -> d.copy(foods = d.foods + result.foods) to Unit }
            _ui.value = _ui.value.copy(tab = Tab.LIBRARY, sheet = null)
            toast(
                "导入 ${result.foods.size} 条" +
                    if (result.skipped > 0) "，跳过 ${result.skipped} 行" else ""
            )
        }
    }

    /* ---------------- 记录条目 ---------------- */

    fun openEntry(date: String, entry: LogEntry) {
        val qty = if (entry.qty > 0) entry.qty else 1.0
        _ui.value = _ui.value.copy(
            sheet = Sheet.Entry(
                date = date,
                entryId = entry.id,
                name = entry.name,
                per = PerServing(
                    kcal = entry.kcal / qty,
                    p = entry.p / qty,
                    c = entry.c / qty,
                    fat = entry.fat / qty
                ),
                qty = qty
            )
        )
    }

    fun changeEntryQty(delta: Double) {
        val sheet = _ui.value.sheet as? Sheet.Entry ?: return
        val next = (sheet.qty + delta).coerceIn(0.5, 20.0)
        _ui.value = _ui.value.copy(sheet = sheet.copy(qty = next))

        viewModelScope.launch {
            repo.mutate { d ->
                val list = d.logs[sheet.date].orEmpty().map { e ->
                    if (e.id != sheet.entryId) {
                        e
                    } else {
                        e.copy(
                            qty = next,
                            kcal = sheet.per.kcal * next,
                            p = sheet.per.p * next,
                            c = sheet.per.c * next,
                            fat = sheet.per.fat * next
                        )
                    }
                }
                d.copy(logs = d.logs + (sheet.date to list)) to Unit
            }
        }
    }

    fun deleteEntry(date: String, entryId: String) {
        viewModelScope.launch {
            repo.mutate { d ->
                val list = d.logs[date].orEmpty().filterNot { it.id == entryId }
                d.copy(logs = d.logs + (date to list)) to Unit
            }
            closeSheet()
            toast("已删除这条记录")
        }
    }

    fun copyYesterday() {
        val date = _ui.value.date
        val prev = Dates.shift(date, -1)
        viewModelScope.launch {
            val count = repo.mutate { d ->
                val src = d.logs[prev].orEmpty()
                if (src.isEmpty()) {
                    d to 0
                } else {
                    val now = System.currentTimeMillis()
                    val copies = src.map { it.copy(id = Ids.next(), ts = now) }
                    val list = d.logs[date].orEmpty() + copies
                    d.copy(logs = d.logs + (date to list)) to copies.size
                }
            }
            toast(if (count == 0) "昨天没有记录" else "已复制昨天的 $count 条")
        }
    }

    fun clearDay(date: String) {
        viewModelScope.launch {
            repo.mutate { d -> d.copy(logs = d.logs - date) to Unit }
            toast("已清空这一天")
        }
    }

    /* ---------------- 体重与资料 ---------------- */

    fun addWeight(kg: Double) {
        if (kg < 20 || kg > 300) {
            toast("输个体重吧")
            return
        }
        val date = Dates.today()
        viewModelScope.launch {
            repo.mutate { d ->
                val exists = d.weights.any { it.date == date }
                val list = if (exists) {
                    d.weights.map { if (it.date == date) it.copy(kg = kg) else it }
                } else {
                    d.weights + WeightLog(date, kg)
                }
                d.copy(weights = list, profile = d.profile.copy(weight = kg)) to Unit
            }
            toast("已记录 ${kg.pretty()} kg")
        }
    }

    fun openProfile() {
        _ui.value = _ui.value.copy(sheet = Sheet.Profile)
    }

    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            repo.mutate { d ->
                d.copy(profile = profile, meta = d.meta.copy(onboarded = true)) to Unit
            }
            closeSheet()
            toast("目标已更新")
        }
    }

    /* ---------------- 备份 ---------------- */

    fun exportText(): String = repo.exportText(state.value.data)

    fun markBackedUp() {
        viewModelScope.launch {
            repo.mutate { d ->
                d.copy(meta = d.meta.copy(lastBackup = System.currentTimeMillis())) to Unit
            }
        }
    }

    fun importText(text: String) {
        val parsed = repo.parseImport(text)
        if (parsed == null) {
            toast("这个文件读不出来")
            return
        }
        viewModelScope.launch {
            repo.replace(
                parsed.copy(meta = Meta(onboarded = true, lastBackup = System.currentTimeMillis()))
            )
            toast("已恢复备份")
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.replace(AppData(meta = Meta(onboarded = true)))
            toast("已清空")
        }
    }

    companion object {
        fun factory(context: Context) = viewModelFactory {
            initializer { AppViewModel(AppRepository(context)) }
        }
    }
}
