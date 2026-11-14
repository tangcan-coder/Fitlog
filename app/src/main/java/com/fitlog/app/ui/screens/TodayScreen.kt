package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.AppData
import com.fitlog.app.data.Cats
import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Meal
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.domain.totalsOf
import com.fitlog.app.ui.components.CalorieRing
import com.fitlog.app.ui.components.CircleIconButton
import com.fitlog.app.ui.components.FitCard
import com.fitlog.app.ui.components.FitIcon
import com.fitlog.app.ui.components.GhostButton
import com.fitlog.app.ui.components.IconKind
import com.fitlog.app.ui.components.MacroCard
import com.fitlog.app.ui.theme.FitTheme

@Composable
fun TodayScreen(
    data: AppData,
    date: String,
    onShiftDate: (Long) -> Unit,
    onBackToToday: () -> Unit,
    onAddToMeal: (Meal) -> Unit,
    onEntryClick: (LogEntry) -> Unit,
    onCopyYesterday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    val targets = targetsOf(data.profile)
    val todayEntries = data.logs[date].orEmpty()
    val totals = totalsOf(todayEntries)
    val isToday = date == Dates.today()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 118.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isToday) "今天" else Dates.human(date),
                        color = c.text,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = date.replace("-", " / ") + "  " + Dates.weekdayLabel(Dates.parse(date)),
                        color = c.text3,
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(IconKind.ChevronLeft, size = 34.dp) { onShiftDate(-1) }
                    Spacer(Modifier.width(6.dp))
                    CircleIconButton(IconKind.ChevronRight, size = 34.dp) { onShiftDate(1) }
                }
            }
        }

        item {
            FitCard(padding = 8.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CalorieRing(consumed = totals.kcal, target = targets.kcal)
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                MacroCard("蛋白", totals.protein, targets.protein, c.protein, Modifier.weight(1f))
                MacroCard("碳水", totals.carbs, targets.carbs, c.carbs, Modifier.weight(1f))
                MacroCard("脂肪", totals.fat, targets.fat, c.fat, Modifier.weight(1f))
            }
        }

        items(Meal.all.size) { index ->
            val meal = Meal.all[index]
            val mealEntries = todayEntries.filter { it.meal == meal.id }
            MealCard(
                meal = meal,
                entries = mealEntries,
                onAdd = { onAddToMeal(meal) },
                onEntryClick = onEntryClick,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                GhostButton(
                    text = "复制昨天",
                    icon = IconKind.Copy,
                    modifier = Modifier.weight(1f)
                ) { onCopyYesterday() }
                if (!isToday) {
                    GhostButton(
                        text = "回到今天",
                        icon = IconKind.Calendar,
                        modifier = Modifier.weight(1f)
                    ) { onBackToToday() }
                }
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    entries: List<LogEntry>,
    onAdd: () -> Unit,
    onEntryClick: (LogEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    val mealKcal = entries.sumOf { it.kcal }.toInt()

    FitCard(modifier = modifier, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(c.tint),
                contentAlignment = Alignment.Center
            ) {
                Text(Cats.icon(mealCategory(meal)), fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (mealKcal > 0) {
                    Text("$mealKcal kcal", color = c.text3, fontSize = 11.5.sp)
                }
            }
            CircleIconButton(
                icon = IconKind.Plus,
                size = 32.dp,
                background = if (entries.isEmpty()) c.accent else c.tint,
                tint = if (entries.isEmpty()) c.accentInk else c.text2
            ) { onAdd() }
        }

        if (entries.isEmpty()) {
            Text(
                text = "还没有记录",
                color = c.text3,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(start = 44.dp, top = 10.dp)
            )
        } else {
            Spacer(Modifier.height(8.dp))
            entries.forEach { entry ->
                EntryRow(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun EntryRow(entry: LogEntry, onClick: () -> Unit) {
    val c = FitTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(start = 44.dp, top = 9.dp, bottom = 9.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.name,
            color = c.text,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${formatQty(entry.qty)}${entry.unit} · ${entry.kcal.toInt()} kcal",
            color = c.text3,
            fontSize = 11.5.sp
        )
        Spacer(Modifier.width(6.dp))
        FitIcon(IconKind.ChevronRight, c.text3, size = 13.dp, strokeWidth = 1.8.dp)
    }
}

private fun formatQty(q: Double): String {
    val r = Math.round(q * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}

private fun mealCategory(meal: Meal): String = when (meal) {
    Meal.BREAKFAST -> "饮品"
    Meal.LUNCH, Meal.DINNER -> "家常菜"
    Meal.SNACK -> "零食"
}
