package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.Cats
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.Sheet
import com.fitlog.app.ui.components.CircleIconButton
import com.fitlog.app.ui.components.FitTextField
import com.fitlog.app.ui.components.GhostButton
import com.fitlog.app.ui.components.IconKind
import com.fitlog.app.ui.theme.FitTheme

@Composable
fun FoodFormSheet(form: Sheet.FoodForm, vm: AppViewModel, onDismiss: () -> Unit) {
    val c = FitTheme.colors

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        SheetScrim(onDismiss)
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomSheetCard {
                SheetHeader(
                    title = if (form.isNew) "新建食物" else "编辑食物",
                    onCancel = onDismiss,
                    confirmLabel = "保存",
                    onConfirm = { vm.saveFoodForm() }
                )
                SheetBody {
                    FieldLabel("名称")
                    FitTextField(
                        value = form.name,
                        onValueChange = { v -> vm.updateFoodForm { it.copy(name = v) } },
                        placeholder = "例如：香菇滑鸡"
                    )

                    VSpace(16)
                    FieldLabel("分类")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Cats.list) { cat ->
                            val active = cat == form.cat
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(if (active) c.accent else c.surface2)
                                    .clickable { vm.updateFoodForm { it.copy(cat = cat) } }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
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

                    VSpace(16)
                    FieldLabel("这一份叫什么 · 大概多少克（克数可空）")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FitTextField(
                            value = form.unit,
                            onValueChange = { v -> vm.updateFoodForm { it.copy(unit = v) } },
                            placeholder = "碗 / 个 / 勺",
                            modifier = Modifier.weight(1f)
                        )
                        FitTextField(
                            value = form.gram,
                            onValueChange = { v -> vm.updateFoodForm { it.copy(gram = v) } },
                            placeholder = "克数",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    VSpace(6)
                    Text(
                        "按“份”记录比按克省事，克数只是备注。",
                        color = c.text3,
                        fontSize = 11.sp
                    )

                    VSpace(16)
                    FieldLabel("营养（都按上面这 1 份算）")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FitTextField(
                            value = form.kcal,
                            onValueChange = { v -> vm.updateFoodForm { it.copy(kcal = v) } },
                            placeholder = "热量 kcal",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FitTextField(
                            value = form.p,
                            onValueChange = { v -> vm.updateFoodForm { it.copy(p = v) } },
                            placeholder = "蛋白 g",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    VSpace(9)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FitTextField(
                            value = form.c,
                            onValueChange = { v -> vm.updateFoodForm { it.copy(c = v) } },
                            placeholder = "碳水 g",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FitTextField(
                            value = form.fat,
                            onValueChange = { v -> vm.updateFoodForm { it.copy(fat = v) } },
                            placeholder = "脂肪 g",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    VSpace(6)
                    Text("只填热量也能保存，另外三项以后随时补。", color = c.text3, fontSize = 11.sp)

                    if (!form.isNew) {
                        VSpace(20)
                        GhostButton(
                            "删除这个食物",
                            Modifier.fillMaxWidth(),
                            color = c.danger,
                            icon = IconKind.Trash
                        ) { form.foodId?.let { vm.deleteFood(it) } }
                        VSpace(6)
                        Text(
                            "已经记下的历史不会受影响。",
                            color = c.text3,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntrySheet(sheet: Sheet.Entry, vm: AppViewModel, onDismiss: () -> Unit) {
    val c = FitTheme.colors
    val kcal = (sheet.per.kcal * sheet.qty)

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        SheetScrim(onDismiss)
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomSheetCard {
                SheetHeader(
                    title = sheet.name,
                    onCancel = onDismiss,
                    confirmLabel = "完成",
                    onConfirm = onDismiss,
                    cancelLabel = "关闭"
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleIconButton(
                            IconKind.Minus,
                            size = 48.dp,
                            background = c.surface2,
                            tint = c.text
                        ) { vm.changeEntryQty(-0.5) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 30.dp)
                        ) {
                            Text(
                                formatQty(sheet.qty),
                                color = c.text,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("份", color = c.text3, fontSize = 11.sp)
                        }
                        CircleIconButton(
                            IconKind.Plus,
                            size = 48.dp,
                            background = c.accent,
                            tint = c.accentInk
                        ) { vm.changeEntryQty(0.5) }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "约 ${kcal.toInt()} kcal · 蛋白 ${trim(sheet.per.p * sheet.qty)}g · " +
                            "碳水 ${trim(sheet.per.c * sheet.qty)}g · 脂肪 ${trim(sheet.per.fat * sheet.qty)}g",
                        color = c.text2,
                        fontSize = 12.5.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))
                    GhostButton(
                        "删除这条记录",
                        Modifier.fillMaxWidth(),
                        color = c.danger,
                        icon = IconKind.Trash
                    ) { vm.deleteEntry(sheet.date, sheet.entryId) }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun BatchSheet(vm: AppViewModel, onDismiss: () -> Unit) {
    val c = FitTheme.colors
    var text by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        SheetScrim(onDismiss)
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomSheetCard {
                SheetHeader(
                    title = "批量导入",
                    onCancel = onDismiss,
                    confirmLabel = "导入",
                    onConfirm = { vm.commitBatch(text) }
                )
                SheetBody {
                    Text(
                        "一行一条，空格或逗号分隔：名称 热量 蛋白 碳水 脂肪\n蛋白/碳水/脂肪可以不填。",
                        color = c.text3,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp
                    )
                    VSpace(12)
                    FitTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = "白米饭 230 4.3 48 0.3\n水煮蛋 78 6.3 0.6 5.3\n无糖豆浆 60 5.2 1.8 2.5\n苹果 95 0.5 25 0.3",
                        multiline = true,
                        minHeight = 190.dp
                    )
                    VSpace(8)
                    Text("导入后分类都默认放在「主食」，可以逐个改。", color = c.text3, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatQty(q: Double): String {
    val r = Math.round(q * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}

private fun trim(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}
