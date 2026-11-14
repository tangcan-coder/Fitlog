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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.fitlog.app.data.Cats
import com.fitlog.app.data.Food
import com.fitlog.app.data.Meal
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.ui.SelectState
import com.fitlog.app.ui.components.CircleIconButton
import com.fitlog.app.ui.components.EmptyState
import com.fitlog.app.ui.components.FitIcon
import com.fitlog.app.ui.components.FitTextField
import com.fitlog.app.ui.components.IconKind
import com.fitlog.app.ui.components.PrimaryButton
import com.fitlog.app.ui.theme.FitTheme

@Composable
fun FoodSelectScreen(
    state: SelectState,
    foods: List<Food>,
    onQuery: (String) -> Unit,
    onCat: (String) -> Unit,
    onQty: (String, Int) -> Unit,
    onCommit: () -> Unit,
    onClose: () -> Unit,
    onNewFood: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors

    val visible = foods
        .filter { state.cat == Cats.ALL || it.cat == state.cat }
        .filter { Pinyin.matches(it.name, it.alias, state.query) }
        .sortedWith(compareByDescending<Food> { it.uses }.thenByDescending { it.lastUsed }.thenBy { it.name })

    val chosen = state.qty.filterValues { it > 0 }
    val totalKcal = chosen.entries.sumOf { (id, n) ->
        (foods.firstOrNull { it.id == id }?.kcal ?: 0.0) * n
    }.toInt()

    Column(modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        // 顶部操作条
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "取消",
                color = c.text2,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    Meal.of(state.meal).label,
                    color = c.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(Dates.human(state.date), color = c.text3, fontSize = 11.sp)
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton("记录", enabled = chosen.isNotEmpty(), onClick = onCommit)
        }

        FitTextField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = "搜索食物 · 试试拼音首字母 mf",
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Cats.filters) { cat ->
                val active = cat == state.cat
                Box(
                    Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) c.accent else c.tint)
                        .clickable { onCat(cat) }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        cat,
                        color = if (active) c.accentInk else c.text2,
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        text = if (state.query.isNotEmpty()) "没有匹配的食物" else "食物库还是空的\n先把常吃的录进来",
                        actionLabel = "新建食物",
                        onAction = onNewFood
                    )
                }
            }

            items(visible, key = { it.id }) { food ->
                val n = state.qty[food.id] ?: 0
                FoodPickRow(
                    food = food,
                    qty = n,
                    onPlus = { onQty(food.id, 1) },
                    onMinus = { onQty(food.id, -1) }
                )
            }

            if (visible.isNotEmpty()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onNewFood)
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .border(1.dp, c.line2, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            FitIcon(IconKind.Plus, c.text2, size = 14.dp, strokeWidth = 1.9.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("没有？新建一个食物", color = c.text2, fontSize = 13.5.sp)
                    }
                }
            }
        }

        // 底部汇总条
        if (chosen.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "已选 ${chosen.values.sum()} 份",
                        color = c.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("约 $totalKcal kcal", color = c.text3, fontSize = 11.5.sp)
                }
                PrimaryButton("加入${Meal.of(state.meal).label}", onClick = onCommit)
            }
        }
    }
}

@Composable
private fun FoodPickRow(
    food: Food,
    qty: Int,
    onPlus: () -> Unit,
    onMinus: () -> Unit
) {
    val c = FitTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (qty > 0) c.tint else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(Cats.icon(food.cat), fontSize = 17.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(food.name, color = c.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                "1${food.unit} · ${food.kcal.toInt()} kcal · 蛋白 ${trimNum(food.p)}g",
                color = c.text3,
                fontSize = 11.5.sp
            )
        }
        if (qty > 0) {
            CircleIconButton(IconKind.Minus, size = 30.dp, background = c.surface2, tint = c.text2) { onMinus() }
            Text(
                qty.toString(),
                color = c.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        CircleIconButton(
            IconKind.Plus,
            size = 30.dp,
            background = if (qty > 0) c.accent else c.surface2,
            tint = if (qty > 0) c.accentInk else c.text2
        ) { onPlus() }
    }
}

private fun trimNum(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}
