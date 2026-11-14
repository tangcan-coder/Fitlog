package com.fitlog.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.Cats
import com.fitlog.app.data.Food
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.ui.components.CircleIconButton
import com.fitlog.app.ui.components.EmptyState
import com.fitlog.app.ui.components.FitIcon
import com.fitlog.app.ui.components.FitTextField
import com.fitlog.app.ui.components.IconKind
import com.fitlog.app.ui.components.PrimaryButton
import com.fitlog.app.ui.theme.FitTheme

@Composable
fun LibraryScreen(
    foods: List<Food>,
    onEdit: (String) -> Unit,
    onNew: () -> Unit,
    onBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Cats.ALL) }

    val visible = foods
        .filter { cat == Cats.ALL || it.cat == cat }
        .filter { Pinyin.matches(it.name, it.alias, query) }
        .sortedWith(compareByDescending<Food> { it.uses }.thenByDescending { it.lastUsed }.thenBy { it.name })

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("食物库", color = c.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("${foods.size} 种 · 全部由你自己维护", color = c.text3, fontSize = 12.sp)
            }
            Text(
                "批量导入",
                color = c.accentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBatch)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Spacer(Modifier.width(4.dp))
            CircleIconButton(
                IconKind.Plus,
                size = 36.dp,
                background = c.accent,
                tint = c.accentInk
            ) { onNew() }
        }

        FitTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "搜索 · 支持拼音首字母",
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Cats.filters) { item ->
                val active = item == cat
                Box(
                    Modifier
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (active) c.accent else c.tint)
                        .clickable { cat = item }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        item,
                        color = if (active) c.accentInk else c.text2,
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 118.dp)
        ) {
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        text = if (foods.isEmpty()) "食物库还是空的\n把常吃的先录进来，以后点两下就能记" else "没有匹配的食物",
                        actionLabel = if (foods.isEmpty()) "新建食物" else null,
                        onAction = if (foods.isEmpty()) onNew else null
                    )
                }
            }
            items(visible, key = { it.id }) { food ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onEdit(food.id) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
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
                            "1${food.unit} · ${food.kcal.toInt()} kcal" +
                                if (food.uses > 0) " · 用过 ${food.uses} 次" else "",
                            color = c.text3,
                            fontSize = 11.5.sp
                        )
                    }
                    FitIcon(IconKind.ChevronRight, c.text3, size = 14.dp, strokeWidth = 1.8.dp)
                }
            }
        }
    }
}
