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
import com.fitlog.app.domain.Nutrients
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.domain.kcalFromKj
import com.fitlog.app.domain.kjFromKcal
import com.fitlog.app.domain.kjText
import com.fitlog.app.domain.per100gFromServing
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.round1
import com.fitlog.app.domain.servingFromPer100g
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        /** 这一份的实际克数；[per100] 为 true 时必填，是折算的基准 */
        val gram: String = "",
        /**
         * 输入口径。true 表示下面几个营养值填的是营养表上的「每 100 g」，
         * 保存时按 [gram] 折算成一份；false 表示直接填一份的数值（老食物的写法）。
         */
        val per100: Boolean = true,
        /** 能量按营养表的单位填 kJ，保存时折算成 kcal（1 kcal = 4.184 kJ） */
        val kj: String = "",
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

@androidx.compose.runtime.Stable
class AppViewModel(private val repo: AppRepository) : ViewModel() {

    val state: StateFlow<AppState> = repo.data
        .map { AppState(loaded = true, data = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    /**
     * 把界面状态拆成几条独立的流。
     *
     * 弹层里每敲一个字都会更新 [_ui]；如果界面直接订阅整份 `ui`，一次输入就会让整页
     * （包括底下那层玻璃采样）重新组合一遍，手感会明显发涩。拆开之后：
     * 打字只重新组合弹层、弹提示只重新组合提示条，页面和底栏不受影响。
     */
    val tab: StateFlow<Tab> = _ui.map { it.tab }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Tab.TODAY)
    val date: StateFlow<String> = _ui.map { it.date }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState().date)
    val sheet: StateFlow<Sheet?> = _ui.map { it.sheet }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val message: StateFlow<ToastMsg?> = _ui.map { it.message }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 待落盘份量改动所对应的弹层状态；停手 [QTY_WRITE_DELAY_MS] 毫秒后由 [flushPendingQty] 写掉 */
    private var pendingQty: Sheet.Entry? = null
    private var qtyJob: Job? = null

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
        // 份量可能有还没落盘的改动，关弹层前先把它写掉，否则最后一次点击会丢
        if (pendingQty != null) {
            qtyJob?.cancel()
            viewModelScope.launch { flushPendingQty() }
        }
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
        val gram = f.gram?.takeIf { it > 0 }
        // 有克数的食物按营养表的「每 100 g」回填，用户改克数就能重新折算；
        // 早年按「一份」直接记下、没有克数的，保持一份口径，不强行要求补克数。
        val per100Values = per100gFromServing(Nutrients(f.kcal, f.p, f.c, f.fat), gram)
        val shown = per100Values ?: Nutrients(f.kcal, f.p, f.c, f.fat)
        _ui.value = _ui.value.copy(
            sheet = Sheet.FoodForm(
                foodId = f.id,
                name = f.name,
                cat = f.cat,
                unit = f.unit,
                gram = f.gram?.pretty()?.takeIf { it != "0" } ?: "",
                per100 = per100Values != null,
                // 能量在表单里显示成 kJ，正好是包装营养表上的原样
                kj = kjFromKcal(shown.kcal).kjText(),
                p = shown.p.pretty(),
                c = shown.c.pretty(),
                fat = shown.fat.pretty()
            )
        )
    }

    fun updateFoodForm(block: (Sheet.FoodForm) -> Sheet.FoodForm) {
        val cur = _ui.value
        val form = cur.sheet as? Sheet.FoodForm ?: return
        _ui.value = cur.copy(sheet = block(form))
    }

    /**
     * 切换「营养表（每 100 g）」和「直接填一份」两种口径。
     *
     * 这只是换个写法，不应该改变这个食物本身，所以能换算时顺手把界面上的数字也换算过去；
     * 否则用户会看到同一串数字从「每 100 g」变成「每份」，食物却悄悄变了。
     * 留空的格子保持留空，不要被换算成 0。
     */
    fun setFoodBasis(per100: Boolean) {
        val cur = _ui.value
        val form = cur.sheet as? Sheet.FoodForm ?: return
        if (form.per100 == per100) return

        val gram = form.gram.toDoubleOrNull()?.takeIf { it > 0 }
        // 能量的已知单位是 kJ，就在 kJ 空间里直接乘除以克数，避免「kJ→kcal→kJ」的二次取整漂移；
        // 三项宏量本来就是克，走 Nutrients 那套换算。
        val factor = gram?.let { if (per100) 100.0 / it else it / 100.0 }
        val typedKj = form.kj.toDoubleOrNull() ?: 0.0
        val typed = Nutrients(
            kcal = kcalFromKj(typedKj),
            p = form.p.toDoubleOrNull() ?: 0.0,
            c = form.c.toDoubleOrNull() ?: 0.0,
            fat = form.fat.toDoubleOrNull() ?: 0.0,
        )
        val converted = when {
            factor == null -> null
            per100 -> per100gFromServing(typed, gram)
            else -> servingFromPer100g(typed, gram)
        }

        fun field(raw: String, value: Double): String =
            if (converted == null || raw.isBlank()) raw else value.pretty()

        val next = form.copy(
            per100 = per100,
            kj = if (factor == null || form.kj.isBlank()) form.kj else (typedKj * factor).kjText(),
            p = field(form.p, converted?.p ?: 0.0),
            c = field(form.c, converted?.c ?: 0.0),
            fat = field(form.fat, converted?.fat ?: 0.0),
        )
        _ui.value = cur.copy(sheet = next)
    }

    fun saveFoodForm() {
        val form = _ui.value.sheet as? Sheet.FoodForm ?: return
        val name = form.name.trim()
        if (name.isEmpty()) {
            toast("先给食物起个名字")
            return
        }
        // 界面上填的是哪一套数字取决于口径：营养表（每 100 g）要先按克数折算成一份，
        // 存档里永远是「一份」的数值，这样今日页、统计页都不用关心录入方式。
        val typed = Nutrients(
            kcal = kcalFromKj(form.kj.toDoubleOrNull() ?: 0.0),
            p = form.p.toDoubleOrNull() ?: 0.0,
            c = form.c.toDoubleOrNull() ?: 0.0,
            fat = form.fat.toDoubleOrNull() ?: 0.0,
        )
        val gram = form.gram.toDoubleOrNull()?.takeIf { it > 0 }
        val serving = if (form.per100) {
            servingFromPer100g(typed, gram) ?: run {
                toast("这一份多少克要填上，才能按每 100 g 折算")
                return
            }
        } else {
            typed.round1()
        }
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
                        kcal = serving.kcal,
                        p = serving.p,
                        c = serving.c,
                        fat = serving.fat,
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
                                kcal = serving.kcal,
                                p = serving.p,
                                c = serving.c,
                                fat = serving.fat,
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
                // 折算是隐式的，保存时把结果说出来，用户才能立刻确认数字对不对
                toast(
                    if (form.per100 && gram != null) {
                        "已保存 · 1${form.unit.ifBlank { "份" }} ≈ ${serving.kcal.pretty()} kcal"
                    } else {
                        "已保存"
                    }
                )
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

    /**
     * 点一下 ± 只改界面上的份量，真正的写入攒到停手后再做一次。
     * 之前每点一下就把整份 AppData 序列化并写盘，连点十下就是十次全量写，
     * 记录攒到几年后会明显卡顿。
     */
    fun changeEntryQty(delta: Double) {
        val sheet = _ui.value.sheet as? Sheet.Entry ?: return
        val next = (sheet.qty + delta).coerceIn(0.5, 20.0)
        // 已经到上下限时界面不会变，没必要再安排一次写入
        if (next == sheet.qty) return

        val updated = sheet.copy(qty = next)
        _ui.value = _ui.value.copy(sheet = updated)
        pendingQty = updated

        qtyJob?.cancel()
        qtyJob = viewModelScope.launch {
            delay(QTY_WRITE_DELAY_MS)
            flushPendingQty()
        }
    }

    /** 把攒下的份量变更落盘。可以重复调用：取走 pendingQty 后即清空，不会重复写。 */
    private suspend fun flushPendingQty() {
        val sheet = pendingQty ?: return
        pendingQty = null

        repo.mutate { d ->
            val list = d.logs[sheet.date].orEmpty().map { e ->
                if (e.id != sheet.entryId) {
                    e
                } else {
                    e.copy(
                        qty = sheet.qty,
                        kcal = sheet.per.kcal * sheet.qty,
                        p = sheet.per.p * sheet.qty,
                        c = sheet.per.c * sheet.qty,
                        fat = sheet.per.fat * sheet.qty
                    )
                }
            }
            d.copy(logs = d.logs + (sheet.date to list)) to Unit
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
        /** 份量改动合并写入的等待时间：短到察觉不出延迟，长到能把连点合成一次写 */
        private const val QTY_WRITE_DELAY_MS = 250L

        fun factory(context: Context) = viewModelFactory {
            initializer { AppViewModel(AppRepository(context)) }
        }
    }
}
