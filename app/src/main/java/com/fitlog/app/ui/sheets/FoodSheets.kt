package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.Cats
import com.fitlog.app.data.Food
import com.fitlog.app.data.Meal
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.Nutrients
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.domain.kcalFromKj
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.servingFromPer100g
import com.fitlog.app.ui.SelectState
import com.fitlog.app.ui.Sheet
import com.fitlog.app.ui.components.FitListRow
import com.fitlog.app.ui.components.CategoryChips
import com.fitlog.app.ui.components.EmptyHint
import com.fitlog.app.ui.components.FieldRow
import com.fitlog.app.ui.components.FitSearchField
import com.fitlog.app.ui.components.FoodEmojiBox
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.PrimaryCapsule
import com.fitlog.app.ui.components.SegmentedRow
import com.fitlog.app.ui.components.groupedRowShape
import com.fitlog.app.ui.components.StepperPill
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioSheetButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.GlasmioTextField
import io.github.neoruaa.glasmio.ui.glasmioGroupedListBackground
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import kotlin.math.floor
import kotlin.math.roundToInt

/* ============================ 选择食物 ============================ */

@Composable
fun SelectSheet(
    state: SelectState,
    foods: List<Food>,
    visible: Boolean,
    onDismiss: () -> Unit,
    onQuery: (String) -> Unit,
    onCat: (String) -> Unit,
    onQty: (String, Int) -> Unit,
    onCommit: () -> Unit,
    onNewFood: () -> Unit,
    pageMode: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)
    val mealLabel = Meal.of(state.meal).label

    val visibleFoods = remember(state.query, state.cat, foods) {
        foods
            .filter { state.cat == Cats.ALL || it.cat == state.cat }
            .filter { Pinyin.matches(it.name, it.alias, state.query) }
            .sortedWith(
                compareByDescending<Food> { it.uses }
                    .thenByDescending { it.lastUsed }
                    .thenBy { it.name },
            )
    }
    val chosen = state.qty.filterValues { it > 0 }
    val totalKcal = chosen.entries.sumOf { (id, n) ->
        (foods.firstOrNull { it.id == id }?.kcal ?: 0.0) * n
    }.roundToInt()

    // 弹层和二级页面共用同一份内容，区别只在外面这层壳
    val body: @Composable ColumnScope.() -> Unit = {
        val pad = LocalGlasmioSheetContentPadding.current
        Column(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = pad.calculateTopPadding() + 6.dp,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item(key = "filters") {
                    Column(
                        Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FitSearchField(
                            value = state.query,
                            onValueChange = onQuery,
                            placeholder = "搜索食物 · 试试拼音首字母 mf",
                        )
                        CategoryChips(
                            items = Cats.filters,
                            selected = state.cat,
                            onSelect = onCat,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (visibleFoods.isEmpty()) {
                    item(key = "empty") {
                        Column(Modifier.padding(top = 14.dp)) {
                            EmptyHint("没有匹配的食物，换个关键词或新建一个")
                        }
                    }
                } else {
                    // 行拆成独立 item，食物很多时也只组合屏幕上的几行。
                    itemsIndexed(visibleFoods, key = { _, food -> food.id }) { index, food ->
                        val count = state.qty[food.id] ?: 0
                        FitListRow(
                            title = food.name,
                            subtitle = "1${food.unit} · ${food.kcal.roundToInt()} kcal · " +
                                "蛋白 ${trimNumber(food.p)}g",
                            showTopSeparator = index > 0,
                            leading = { FoodEmojiBox(food.cat) },
                            modifier = Modifier.glasmioGroupedListBackground(
                                groupedRowShape(
                                    isFirst = index == 0,
                                    isLast = index == visibleFoods.lastIndex,
                                ),
                            ),
                            trailing = {
                                StepperPill(
                                    count = count,
                                    onMinus = { onQty(food.id, -1) },
                                    onPlus = { onQty(food.id, 1) },
                                )
                            },
                        )
                    }
                }

                item(key = "new-food") {
                    Column(Modifier.padding(top = 14.dp)) {
                        GlasmioGroupedList {
                            FitListRow(
                                "没有？新建一个食物",
                                systemName = "plus",
                                titleColor = colors.accent,
                                showTopSeparator = false,
                                onClick = onNewFood,
                            )
                        }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = pad.calculateBottomPadding() + 16.dp),
            ) {
                if (chosen.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "已选 ${state.totalCount} 份",
                            style = IosTypography.subheadline,
                            color = colors.content,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "约 $totalKcal kcal",
                            style = IosTypography.subheadline,
                            color = colors.secondaryContent,
                        )
                    }
                }
                PrimaryCapsule(
                    text = "加入$mealLabel",
                    onClick = onCommit,
                    enabled = chosen.isNotEmpty(),
                )
            }
        }
    }

    if (pageMode) {
        GlasmioPinnedPage(
            title = "$mealLabel · ${Dates.human(state.date)}",
            onNavigateBack = onDismiss,
            actions = {
                GlasmioButton(
                    onClick = onCommit,
                    size = GlasmioButtonSize.Small,
                    enabled = chosen.isNotEmpty(),
                ) {
                    Text("记录")
                }
            },
            topBarBlurProgress = edge,
            modifier = Modifier.imePadding(),
        ) { padding ->
            pageBody(padding = padding, body = body)
        }
    } else {
        GlasmioBottomSheet(
            visible = visible,
            onDismissRequest = onDismiss,
            skipPartiallyExpanded = true,
            title = "选择食物",
            topBarBlurProgress = edge,
            topBar = {
                GlasmioSheetTopToolbar(
                    title = "$mealLabel · ${Dates.human(state.date)}",
                    navigation = {
                        GlasmioSheetButton(onClick = onDismiss) { Text("取消") }
                    },
                    actions = {
                        GlasmioSheetButton(onClick = onCommit, enabled = chosen.isNotEmpty()) {
                            Text("记录")
                        }
                    },
                )
            },
        ) {
            body()
        }
    }
}

/* ============================ 新建 / 编辑食物 ============================ */

@Composable
fun FoodFormSheet(
    form: Sheet.FoodForm,
    visible: Boolean,
    onDismiss: () -> Unit,
    onUpdate: ((Sheet.FoodForm) -> Sheet.FoodForm) -> Unit,
    onBasis: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    pageMode: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)
    var confirmDelete by remember { mutableStateOf(false) }
    // 弹层是独立窗口，Toast 会被整个盖住（用户点保存看起来毫无反应），
    // 所以校验结果必须落在表单里：哪一栏缺东西就把提示写在哪一栏下面。
    var nameError by remember { mutableStateOf(false) }
    var gramError by remember { mutableStateOf(false) }
    var numberError by remember { mutableStateOf(false) }
    val attemptSave = {
        nameError = form.name.isBlank()
        gramError = form.per100 && (form.gram.toDoubleOrNull() ?: 0.0) <= 0.0
        // 营养值填错（比如 12.5.3）以前会被 toDoubleOrNull 兜成 0 静默存下去
        numberError = listOf(form.kj, form.p, form.c, form.fat).any { it.isBadNumber() }
        if (!nameError && !gramError && !numberError) onSave()
    }
    val title = if (form.isNew) "新建食物" else "编辑食物"
    val unitLabel = form.unit.ifBlank { "份" }
    // 实时折算：边改克数或营养表，这里立刻给出「一份」的结果，存之前就能对一遍。
    // 能量输入是 kJ（营养表的写法），折算成一份之后再换成 kcal 显示。
    val gram = form.gram.toDoubleOrNull()?.takeIf { it > 0 }
    val serving = servingFromPer100g(
        Nutrients(
            kcal = kcalFromKj(form.kj.toDoubleOrNull() ?: 0.0),
            p = form.p.toDoubleOrNull() ?: 0.0,
            c = form.c.toDoubleOrNull() ?: 0.0,
            fat = form.fat.toDoubleOrNull() ?: 0.0,
        ),
        gram,
    )

    // 弹层和二级页面共用同一份表单内容，区别只在外面这层壳
    val body: @Composable ColumnScope.() -> Unit = {
        val pad = LocalGlasmioSheetContentPadding.current
        Column(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = pad.calculateTopPadding() + 6.dp,
                    bottom = pad.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "name") {
                    Column {
                        GlasmioGroupedList {
                            GlasmioTextField(
                                value = form.name,
                                onValueChange = { v ->
                                    nameError = false
                                    onUpdate { it.copy(name = v) }
                                },
                                placeholder = "名称，例如：香菇滑鸡",
                            )
                        }
                        if (nameError) {
                            IosGroupFooter("先把名字填上", color = colors.destructive)
                        }
                    }
                }

                item(key = "category") {
                    Column {
                        IosSectionHeader("分类")
                        CategoryChips(
                            items = Cats.list,
                            selected = form.cat,
                            onSelect = { v -> onUpdate { it.copy(cat = v) } },
                        )
                    }
                }

                item(key = "serving") {
                    Column {
                        IosSectionHeader("一份是多少")
                        GlasmioGroupedList {
                            FieldRow(
                                label = "单位",
                                value = form.unit,
                                onValueChange = { v -> onUpdate { it.copy(unit = v) } },
                                placeholder = "碗 / 个 / 勺",
                                showTopSeparator = false,
                            )
                            FieldRow(
                                label = "克数",
                                value = form.gram,
                                onValueChange = { v ->
                                    gramError = false
                                    onUpdate { it.copy(gram = v) }
                                },
                                placeholder = if (form.per100) "例如 150" else "可空",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                        IosGroupFooter(
                            when {
                                gramError -> "这一份多少克要填上，营养表就是按它折算的。"
                                form.per100 -> "营养表上的「每 100 克」乘上这里的克数再除以 100，就是一份的营养。"
                                else -> "按「份」记录比按克省事，克数只是备注。"
                            },
                            color = if (gramError) colors.destructive else Color.Unspecified,
                        )
                    }
                }

                item(key = "nutrition") {
                    Column {
                        IosSectionHeader("营养")
                        // 包装上的营养表一律按每 100 g 标，先按表填、填克数自动折算，比心算快也不容易错。
                        // 早年按「一份」记下的食物没有克数，切到「直接填一份」就能保持原样。
                        SegmentedRow(
                            options = listOf("100g" to "营养表（每 100 g）", "serving" to "直接填一份"),
                            selected = if (form.per100) "100g" else "serving",
                            onSelect = { v ->
                                gramError = false
                                onBasis(v == "100g")
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        GlasmioGroupedList {
                            FieldRow(
                                label = "能量",
                                value = form.kj,
                                onValueChange = { v ->
                                    numberError = false
                                    onUpdate { it.copy(kj = v) }
                                },
                                placeholder = "0",
                                suffix = "kJ",
                                keyboardType = KeyboardType.Decimal,
                                showTopSeparator = false,
                            )
                            FieldRow(
                                label = "蛋白",
                                value = form.p,
                                onValueChange = { v ->
                                    numberError = false
                                    onUpdate { it.copy(p = v) }
                                },
                                placeholder = "0",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                            FieldRow(
                                label = "碳水",
                                value = form.c,
                                onValueChange = { v ->
                                    numberError = false
                                    onUpdate { it.copy(c = v) }
                                },
                                placeholder = "0",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                            FieldRow(
                                label = "脂肪",
                                value = form.fat,
                                onValueChange = { v ->
                                    numberError = false
                                    onUpdate { it.copy(fat = v) }
                                },
                                placeholder = "0",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                        if (numberError) {
                            IosGroupFooter(
                                "营养值只能填数字，暂时不知道就先留空",
                                color = colors.destructive,
                            )
                        }
                        if (form.per100) {
                            Spacer(Modifier.height(10.dp))
                            NutritionPreviewCard(form = form, serving = serving, gram = gram)
                            IosGroupFooter("能量照营养表填 kJ（1 kcal ≈ 4.184 kJ），只能填能量也能保存。")
                        } else {
                            IosGroupFooter("按 1$unitLabel 算，能量单位和营养表一致，只填能量也能保存。")
                        }
                    }
                }

                if (!form.isNew) {
                    item(key = "delete") {
                        Column {
                            GlasmioGroupedList {
                                FitListRow(
                                    "删除这个食物",
                                    titleColor = colors.destructive,
                                    showTopSeparator = false,
                                    showDisclosure = false,
                                    onClick = { confirmDelete = true },
                                )
                            }
                            IosGroupFooter("已经记下的历史不会受影响。")
                        }
                    }
                }
            }
        }
    }

    if (pageMode) {
        // 二级页面：玻璃导航栏 + 返回键 + 操作按钮，内容滚动时顶栏做渐进模糊
        GlasmioPinnedPage(
            title = title,
            onNavigateBack = onDismiss,
            actions = {
                GlasmioButton(onClick = attemptSave, size = GlasmioButtonSize.Small) {
                    Text("保存")
                }
            },
            topBarBlurProgress = edge,
            modifier = Modifier.imePadding(),
        ) { padding ->
            pageBody(padding = padding, body = body)
        }
    } else {
        GlasmioBottomSheet(
            visible = visible,
            onDismissRequest = onDismiss,
            skipPartiallyExpanded = true,
            title = title,
            topBarBlurProgress = edge,
            topBar = {
                GlasmioSheetTopToolbar(
                    title = title,
                    navigation = {
                        GlasmioSheetButton(onClick = onDismiss) { Text("取消") }
                    },
                    actions = {
                        GlasmioSheetButton(onClick = attemptSave) { Text("保存") }
                    },
                )
            },
        ) {
            body()
        }
    }

    GlasmioAlertDialog(
        visible = confirmDelete,
        onDismissRequest = { confirmDelete = false },
        title = "删除「${form.name}」？",
        message = "这个食物会从食物库里移除，已经记下的历史记录不受影响。",
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = "取消",
                role = GlasmioAlertButtonRole.Cancel,
                onClick = { confirmDelete = false },
            ),
            GlasmioAlertButtonSpec(
                label = "删除",
                role = GlasmioAlertButtonRole.Destructive,
                onClick = {
                    confirmDelete = false
                    onDelete()
                },
            ),
        ),
    )
}

/**
 * 「一份」换算结果的实时预览。
 *
 * 折算公式是隐式的，如果只在保存后才生效，用户没法确认自己填的是 100 g 还是一份。
 * 这里把结果摊开（一份多少千卡、三项宏量各多少克），和列表里显示的口径完全一致。
 */
@Composable
private fun NutritionPreviewCard(
    form: Sheet.FoodForm,
    serving: Nutrients?,
    gram: Double?,
) {
    val colors = GlasmioTheme.colors
    val unit = form.unit.ifBlank { "份" }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(20.dp))
            .background(colors.elevatedBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                if (serving != null) {
                    "1$unit ≈ ${serving.kcal.pretty()} kcal"
                } else {
                    "填上克数，这里算出这一份的营养"
                },
                style = IosTypography.subheadline,
                color = colors.content,
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            if (serving != null && gram != null) {
                "${gram.pretty()} g · 蛋白 ${serving.p.pretty()} g · " +
                    "碳水 ${serving.c.pretty()} g · 脂肪 ${serving.fat.pretty()} g"
            } else {
                "每 100 g 的数值 × 克数 ÷ 100"
            },
            style = IosTypography.footnote,
            color = colors.secondaryContent,
        )
    }
}

/* ============================ 修改某条记录 ============================ */

@Composable
fun EntrySheet(
    sheet: Sheet.Entry,
    visible: Boolean,
    onDismiss: () -> Unit,
    onDelta: (Double) -> Unit,
    onDelete: () -> Unit,
    pageMode: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    val kcal = sheet.per.kcal * sheet.qty
    val minQty = sheet.qty <= 0.5

    val body: @Composable ColumnScope.() -> Unit = {
        val pad = LocalGlasmioSheetContentPadding.current
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 8.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityButton(
                    systemName = "minus",
                    enabled = !minQty,
                    onClick = { onDelta(-0.5) },
                )
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        trimNumber(sheet.qty),
                        style = IosTypography.largeTitle,
                        fontWeight = FontWeight(700),
                        color = colors.content,
                    )
                    Text("份", style = IosTypography.footnote, color = colors.tertiaryContent)
                }
                QuantityButton(
                    systemName = "plus",
                    enabled = true,
                    onClick = { onDelta(0.5) },
                )
            }

            Text(
                "约 ${kcal.roundToInt()} kcal · 蛋白 ${trimNumber(sheet.per.p * sheet.qty)}g · " +
                    "碳水 ${trimNumber(sheet.per.c * sheet.qty)}g · " +
                    "脂肪 ${trimNumber(sheet.per.fat * sheet.qty)}g",
                style = IosTypography.subheadline,
                color = colors.secondaryContent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
            )

            GlasmioGroupedList {
                FitListRow(
                    "删除这条记录",
                    titleColor = colors.destructive,
                    showTopSeparator = false,
                    onClick = onDelete,
                )
            }
        }
    }

    if (pageMode) {
        GlasmioPinnedPage(
            title = sheet.name,
            onNavigateBack = onDismiss,
        ) { padding ->
            pageBody(padding = padding, body = body)
        }
    } else {
        GlasmioBottomSheet(
            visible = visible,
            onDismissRequest = onDismiss,
            fitContent = true,
            title = sheet.name,
            topBar = {
                GlasmioSheetTopToolbar(
                    title = sheet.name,
                    actions = {
                        GlasmioSheetButton(onClick = onDismiss) { Text("完成") }
                    },
                )
            },
        ) {
            body()
        }
    }
}

@Composable
private fun QuantityButton(
    systemName: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    Box(
        Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(colors.segmentedControlBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon(
            systemName,
            if (systemName == "plus") "增加份量" else "减少份量",
            size = 20.dp,
            tint = if (enabled) colors.content else colors.tertiaryContent,
        )
    }
}

/* ============================ 批量导入 ============================ */

@Composable
fun BatchSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCommit: (String) -> Unit,
    pageMode: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    var text by rememberSaveable { mutableStateOf("") }

    val body: @Composable ColumnScope.() -> Unit = {
        val pad = LocalGlasmioSheetContentPadding.current
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        ) {
            Text(
                "一行一条，空格或逗号分隔：名称 能量 蛋白 碳水 脂肪。" +
                    "能量按 kcal 写，营养表上抄来的就写成 326kj；后面三项可以不填。",
                style = IosTypography.footnote,
                color = colors.secondaryContent,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .clip(ContinuousRoundedRectangle(22.dp))
                    // 页面底色就是 groupedBackground，输入区必须比它亮一档才看得出边界
                    .background(colors.elevatedBackground)
                    .padding(14.dp),
            ) {
                // 占位示例放进 decorationBox，避免输入第一个字时输入框被重建。
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = IosTypography.body.copy(color = colors.content),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 172.dp),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                "白米饭 230 4.3 48 0.3\n水煮蛋 326kj 6.3 0.6 5.3\n无糖豆浆 60 5.2 1.8 2.5\n苹果 95 0.5 25 0.3",
                                style = IosTypography.body,
                                color = colors.tertiaryContent,
                            )
                        }
                        innerTextField()
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            IosGroupFooter("导入后分类都默认放在「主食」，可以逐个改。")
        }
    }

    if (pageMode) {
        GlasmioPinnedPage(
            title = "批量导入",
            onNavigateBack = onDismiss,
            actions = {
                GlasmioButton(
                    onClick = { onCommit(text) },
                    size = GlasmioButtonSize.Small,
                    enabled = text.isNotBlank(),
                ) {
                    Text("导入")
                }
            },
            modifier = Modifier.imePadding(),
        ) { padding ->
            pageBody(padding = padding, body = body)
        }
    } else {
        GlasmioBottomSheet(
            visible = visible,
            onDismissRequest = onDismiss,
            fitContent = true,
            title = "批量导入",
            topBar = {
                GlasmioSheetTopToolbar(
                    title = "批量导入",
                    navigation = {
                        GlasmioSheetButton(onClick = onDismiss) { Text("取消") }
                    },
                    actions = {
                        GlasmioSheetButton(
                            onClick = { onCommit(text) },
                            enabled = text.isNotBlank(),
                        ) { Text("导入") }
                    },
                )
            },
        ) {
            body()
        }
    }
}

private fun trimNumber(value: Double): String {
    val r = (value * 10).roundToInt() / 10.0
    return if (r == floor(r)) r.toInt().toString() else r.toString()
}

/**
 * 营养值的输入校验：留空表示「以后补」，填了就必须是非负数字。
 *
 * 以前 `"12.5.3".toDoubleOrNull()` 得到 null，再被 `?: 0.0` 兜成 0 静默保存，
 * 用户以为存进去了，实际是一条 0 千卡的食物。输入法允许小数点重复，这条很容易踩。
 */
private fun String.isBadNumber(): Boolean =
    isNotBlank() && (toDoubleOrNull()?.let { it >= 0.0 } != true)

/**
 * 二级页面的内容包装：把 [GlasmioPinnedPage] 给的 PaddingValues 转成页面内容读的
 * `LocalGlasmioSheetContentPadding`，横向留给内容自己的 16dp。
 */
@Composable
private fun pageBody(
    padding: PaddingValues,
    body: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalGlasmioSheetContentPadding provides PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding(),
        ),
    ) {
        Column(Modifier.fillMaxSize()) { body() }
    }
}