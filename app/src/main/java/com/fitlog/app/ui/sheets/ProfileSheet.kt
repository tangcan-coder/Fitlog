package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.fitlog.app.data.ActivityLevels
import com.fitlog.app.data.AppData
import com.fitlog.app.data.Profile
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.components.FitIcon
import com.fitlog.app.ui.components.FitTextField
import com.fitlog.app.ui.components.IconKind
import com.fitlog.app.ui.theme.FitTheme

@Composable
fun ProfileSheet(data: AppData, vm: AppViewModel, onDismiss: () -> Unit) {
    val c = FitTheme.colors
    val p = data.profile

    var sex by remember(p) { mutableStateOf(p.sex) }
    var age by remember(p) { mutableStateOf(p.age.toString()) }
    var height by remember(p) { mutableStateOf(trimNum(p.height)) }
    var weight by remember(p) { mutableStateOf(trimNum(p.weight)) }
    var activity by remember(p) { mutableStateOf(p.activity) }
    var goal by remember(p) { mutableStateOf(p.goal) }
    var rate by remember(p) { mutableStateOf(p.rate) }

    val preview = targetsOf(
        Profile(
            sex = sex,
            age = age.toIntOrNull() ?: p.age,
            height = height.toDoubleOrNull() ?: p.height,
            weight = weight.toDoubleOrNull() ?: p.weight,
            activity = activity,
            goal = goal,
            rate = rate
        )
    )

    Box(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        SheetScrim(onDismiss)
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomSheetCard {
                SheetHeader(
                    title = "资料与目标",
                    onCancel = onDismiss,
                    confirmLabel = "保存",
                    onConfirm = {
                        val a = age.toIntOrNull()
                        val h = height.toDoubleOrNull()
                        val w = weight.toDoubleOrNull()
                        when {
                            a == null || a < 10 || a > 100 -> vm.toast("年龄填 10–100")
                            h == null || h < 100 || h > 230 -> vm.toast("身高填 100–230")
                            w == null || w < 25 || w > 300 -> vm.toast("体重填 25–300")
                            else -> vm.saveProfile(
                                Profile(
                                    sex = sex,
                                    age = a,
                                    height = h,
                                    weight = w,
                                    activity = activity,
                                    goal = goal,
                                    rate = rate
                                )
                            )
                        }
                    }
                )

                SheetBody {
                    FieldLabel("性别")
                    SegmentedRow(
                        options = listOf("male" to "男", "female" to "女"),
                        selected = sex,
                        onSelect = { sex = it }
                    )

                    VSpace(16)
                    FieldLabel("年龄 · 身高(cm) · 体重(kg)")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FitTextField(
                            age,
                            { age = it },
                            "岁",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        FitTextField(
                            height,
                            { height = it },
                            "厘米",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        FitTextField(
                            weight,
                            { weight = it },
                            "公斤",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    VSpace(16)
                    FieldLabel("日常活动量")
                    ActivityLevels.list.forEach { level ->
                        val active = level.value == activity
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 7.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) c.tint else c.surface2)
                                .border(
                                    width = 1.dp,
                                    color = if (active) c.accent else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { activity = level.value }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                level.label,
                                color = if (active) c.text else c.text2,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (active) {
                                FitIcon(IconKind.Check, c.accentText, size = 15.dp, strokeWidth = 2.dp)
                            }
                        }
                    }

                    VSpace(9)
                    FieldLabel("目标")
                    SegmentedRow(
                        options = listOf("cut" to "减脂", "maintain" to "维持", "bulk" to "增肌"),
                        selected = goal,
                        onSelect = { goal = it }
                    )

                    VSpace(16)
                    FieldLabel("每周变化速度")
                    SegmentedRow(
                        options = listOf(
                            "0.25" to "0.25 kg",
                            "0.5" to "0.5 kg",
                            "0.75" to "0.75 kg"
                        ),
                        selected = trimNum(rate),
                        onSelect = { rate = it.toDoubleOrNull() ?: 0.5 }
                    )
                    VSpace(6)
                    Text(
                        "减脂建议 0.5，增肌建议 0.25。太激进容易掉肌肉。",
                        color = c.text3,
                        fontSize = 11.sp
                    )

                    VSpace(18)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surface2)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(c.accent)
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(
                                "每天目标 ${preview.kcal} kcal · " +
                                    "蛋白 ${preview.protein}g / 碳水 ${preview.carbs}g / 脂肪 ${preview.fat}g",
                                color = c.text2,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun trimNum(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}
