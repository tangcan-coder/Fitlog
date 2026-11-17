package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.DaySummaryCard
import com.fitlog.app.ui.components.EmptyHint
import com.fitlog.app.ui.components.HistoryCalendarCard
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.PrimaryCapsule
import com.fitlog.app.ui.components.WeightLineChart
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlassColors
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import java.time.LocalDate
import java.time.YearMonth

/**
 * 「统计」：月历 + 选中日期的营养详情 + 体重趋势。
 *
 * 原来这里只能看最近 7 天，现在改成可以一直往回翻的月历，
 * 点哪天就看那天吃了多少、三大宏量怎么样。
 */
@Composable
fun StatsScreen(
    data: AppData,
    listState: LazyListState,
    bottomPadding: Dp,
    onAddWeight: (Double) -> Unit,
    onInvalidWeight: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    val targets = targetsOf(data.profile)

    val today = LocalDate.parse(Dates.today())
    val currentMonth = YearMonth.from(today)
    var monthKey by rememberSaveable { mutableStateOf(currentMonth.toString()) }
    var selectedKey by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthKey)
    val selected = LocalDate.parse(selectedKey)

    val weights = data.weights.sortedBy { it.date }.takeLast(30)
    var weightInput by rememberSaveable { mutableStateOf("") }

    GlasmioPinnedListPage(
        title = "",
        listState = listState,
        bottomPadding = bottomPadding,
        showsLargeTitle = false,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "calendar") {
            HistoryCalendarCard(
                month = month,
                selected = selected,
                logs = data.logs,
                targetKcal = targets.kcal,
                onSelect = { selectedKey = it.toString() },
                onMonthChange = { next ->
                    monthKey = next.toString()
                    // 翻到别的月份时，选中日跟着落在这个月内，避免详情和日历对不上。
                    if (YearMonth.from(selected) != next) {
                        selectedKey = if (next == currentMonth) today.toString() else next.atDay(1).toString()
                    }
                },
                onToday = {
                    monthKey = currentMonth.toString()
                    selectedKey = today.toString()
                },
            )
        }

        item(key = "day-summary") {
            DaySummaryCard(
                date = selected,
                entries = data.logs[selected.toString()].orEmpty(),
                targets = targets,
            )
        }

        item(key = "weight-trend") {
            Column {
                IosSectionHeader(
                    title = "体重趋势",
                    trailing = {
                        if (weights.isNotEmpty()) {
                            Text(
                                "近 ${weights.size} 次",
                                style = IosTypography.footnote,
                                color = colors.tertiaryContent,
                            )
                        }
                    },
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(ContinuousRoundedRectangle(26.dp))
                        .background(colors.elevatedBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (weights.isEmpty()) {
                        EmptyHint(text = "还没有体重记录\n记录一次就能看到趋势", symbol = null)
                    } else {
                        WeightLineChart(points = weights.map { it.kg }, chartHeight = 100.dp)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "起 ${weights.first().kg.pretty()} kg · ${Dates.short(weights.first().date)}",
                                style = IosTypography.caption2,
                                color = colors.tertiaryContent,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "最新 ${weights.last().kg.pretty()} kg · ${Dates.short(weights.last().date)}",
                                style = IosTypography.caption2,
                                color = colors.secondaryContent,
                            )
                        }
                    }
                }
            }
        }

        item(key = "weight-input") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(26.dp))
                    .background(colors.elevatedBackground)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("今天的体重", style = IosTypography.body, color = colors.content)
                    Spacer(Modifier.weight(1f))
                    WeightInput(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        placeholder = "68.2",
                        colors = colors,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("kg", style = IosTypography.body, color = colors.tertiaryContent)
                }
                Spacer(Modifier.height(12.dp))
                PrimaryCapsule(
                    text = "记录今天体重",
                    height = 44.dp,
                    onClick = {
                        val v = weightInput.toDoubleOrNull()
                        if (v != null && v >= 20 && v <= 300) {
                            onAddWeight(v)
                            weightInput = ""
                        } else {
                            onInvalidWeight()
                        }
                    },
                )
            }
        }

    }
}

@Composable
private fun WeightInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    colors: GlassColors,
) {
    // 占位符放在 decorationBox 里，输入时输入框不会被重建、光标不会跳回开头。
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = IosTypography.body.copy(color = colors.content, textAlign = TextAlign.End),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.width(92.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = IosTypography.body,
                    color = colors.tertiaryContent,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            innerTextField()
        },
    )
}
