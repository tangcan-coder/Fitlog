package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.Cats
import com.fitlog.app.data.Food
import com.fitlog.app.data.Meal
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.ui.SelectState
import com.fitlog.app.ui.Sheet
import com.fitlog.app.ui.components.CategoryChips
import com.fitlog.app.ui.components.EmptyHint
import com.fitlog.app.ui.components.FieldRow
import com.fitlog.app.ui.components.FitSearchField
import com.fitlog.app.ui.components.FoodEmojiBox
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.PrimaryCapsule
import com.fitlog.app.ui.components.groupedRowShape
import com.fitlog.app.ui.components.StepperPill
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
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
    }.toInt()

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
                        GlasmioListRow(
                            title = food.name,
                            subtitle = "1${food.unit} · ${food.kcal.toInt()} kcal · " +
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
                            GlasmioListRow(
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
}

/* ============================ 新建 / 编辑食物 ============================ */

@Composable
fun FoodFormSheet(
    form: Sheet.FoodForm,
    visible: Boolean,
    onDismiss: () -> Unit,
    onUpdate: ((Sheet.FoodForm) -> Sheet.FoodForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)
    var confirmDelete by remember { mutableStateOf(false) }
    val title = if (form.isNew) "新建食物" else "编辑食物"
    val unitLabel = form.unit.ifBlank { "份" }

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
                    GlasmioSheetButton(onClick = onSave) { Text("保存") }
                },
            )
        },
    ) {
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
                    GlasmioGroupedList {
                        GlasmioTextField(
                            value = form.name,
                            onValueChange = { v -> onUpdate { it.copy(name = v) } },
                            placeholder = "名称，例如：香菇滑鸡",
                        )
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
                                onValueChange = { v -> onUpdate { it.copy(gram = v) } },
                                placeholder = "可空",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                        IosGroupFooter("按「份」记录比按克省事，克数只是备注。")
                    }
                }

                item(key = "nutrition") {
                    Column {
                        IosSectionHeader("营养（按 1$unitLabel 算）")
                        GlasmioGroupedList {
                            FieldRow(
                                label = "热量",
                                value = form.kcal,
                                onValueChange = { v -> onUpdate { it.copy(kcal = v) } },
                                placeholder = "0",
                                suffix = "kcal",
                                keyboardType = KeyboardType.Decimal,
                                showTopSeparator = false,
                            )
                            FieldRow(
                                label = "蛋白",
                                value = form.p,
                                onValueChange = { v -> onUpdate { it.copy(p = v) } },
                                placeholder = "0",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                            FieldRow(
                                label = "碳水",
                                value = form.c,
                                onValueChange = { v -> onUpdate { it.copy(c = v) } },
                                placeholder = "0",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                            FieldRow(
                                label = "脂肪",
                                value = form.fat,
                                onValueChange = { v -> onUpdate { it.copy(fat = v) } },
                                placeholder = "0",
                                suffix = "g",
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                        IosGroupFooter("只填热量也能保存，另外三项以后随时补。")
                    }
                }

                if (!form.isNew) {
                    item(key = "delete") {
                        Column {
                            GlasmioGroupedList {
                                GlasmioListRow(
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

/* ============================ 修改某条记录 ============================ */

@Composable
fun EntrySheet(
    sheet: Sheet.Entry,
    visible: Boolean,
    onDismiss: () -> Unit,
    onDelta: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val kcal = sheet.per.kcal * sheet.qty
    val minQty = sheet.qty <= 0.5

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
                "约 ${kcal.toInt()} kcal · 蛋白 ${trimNumber(sheet.per.p * sheet.qty)}g · " +
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
                GlasmioListRow(
                    "删除这条记录",
                    titleColor = colors.destructive,
                    showTopSeparator = false,
                    onClick = onDelete,
                )
            }
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
) {
    val colors = GlasmioTheme.colors
    var text by rememberSaveable { mutableStateOf("") }

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
        val pad = LocalGlasmioSheetContentPadding.current
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        ) {
            Text(
                "一行一条，空格或逗号分隔：名称 热量 蛋白 碳水 脂肪。蛋白 / 碳水 / 脂肪可以不填。",
                style = IosTypography.footnote,
                color = colors.secondaryContent,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .clip(ContinuousRoundedRectangle(22.dp))
                    .background(colors.groupedBackground)
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
                                "白米饭 230 4.3 48 0.3\n水煮蛋 78 6.3 0.6 5.3\n无糖豆浆 60 5.2 1.8 2.5\n苹果 95 0.5 25 0.3",
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
}

private fun trimNumber(value: Double): String {
    val r = (value * 10).roundToInt() / 10.0
    return if (r == floor(r)) r.toInt().toString() else r.toString()
}
