package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.ActivityLevels
import com.fitlog.app.data.AppData
import com.fitlog.app.data.Profile
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.FieldRow
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.SegmentedRow
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioBottomSheet
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSheetButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * 「资料与目标」：首次打开时会自动弹出，之后从「我的」进入。
 * 表单用 iOS 分组列表 + 分段控件，底部实时预览算出来的每日目标。
 */
@Composable
fun ProfileSheet(
    data: AppData,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit,
    onInvalid: (String) -> Unit,
    pageMode: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    val p = data.profile

    var sex by remember(p) { mutableStateOf(p.sex) }
    var age by remember(p) { mutableStateOf(p.age.toString()) }
    var height by remember(p) { mutableStateOf(trimNumber(p.height)) }
    var weight by remember(p) { mutableStateOf(trimNumber(p.weight)) }
    var activity by remember(p) { mutableStateOf(p.activity) }
    var goal by remember(p) { mutableStateOf(p.goal) }
    var rate by remember(p) { mutableStateOf(p.rate) }

    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)

    val preview = targetsOf(
        Profile(
            sex = sex,
            age = age.toIntOrNull() ?: p.age,
            height = height.toDoubleOrNull() ?: p.height,
            weight = weight.toDoubleOrNull() ?: p.weight,
            activity = activity,
            goal = goal,
            rate = rate,
        ),
    )

    val save = {
        val a = age.toIntOrNull()
        val h = height.toDoubleOrNull()
        val w = weight.toDoubleOrNull()
        when {
            a == null || a < 10 || a > 100 -> onInvalid("年龄填 10–100")
            h == null || h < 100 || h > 230 -> onInvalid("身高填 100–230")
            w == null || w < 25 || w > 300 -> onInvalid("体重填 25–300")
            else -> onSave(Profile(sex, a, h, w, activity, goal, rate))
        }
    }

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
                    bottom = pad.calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "sex") {
                    Column {
                        IosSectionHeader("性别")
                        SegmentedRow(
                            options = listOf("male" to "男", "female" to "女"),
                            selected = sex,
                            onSelect = { sex = it },
                        )
                    }
                }

                item(key = "body") {
                    Column {
                        IosSectionHeader("身体数据")
                        GlasmioGroupedList {
                            FieldRow(
                                label = "年龄",
                                value = age,
                                onValueChange = { age = it },
                                placeholder = "岁",
                                suffix = "岁",
                                keyboardType = KeyboardType.Number,
                                showTopSeparator = false,
                            )
                            FieldRow(
                                label = "身高",
                                value = height,
                                onValueChange = { height = it },
                                placeholder = "厘米",
                                suffix = "cm",
                                keyboardType = KeyboardType.Decimal,
                            )
                            FieldRow(
                                label = "体重",
                                value = weight,
                                onValueChange = { weight = it },
                                placeholder = "公斤",
                                suffix = "kg",
                                keyboardType = KeyboardType.Decimal,
                            )
                        }
                    }
                }

                item(key = "activity") {
                    Column {
                        IosSectionHeader("日常活动量")
                        GlasmioGroupedList {
                            ActivityLevels.list.forEachIndexed { index, level ->
                                val active = level.value == activity
                                GlasmioListRow(
                                    title = level.label,
                                    showTopSeparator = index > 0,
                                    showDisclosure = false,
                                    onClick = { activity = level.value },
                                    trailing = if (active) {
                                        { SfIcon("checkmark", null, size = 17.dp, tint = colors.accent) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }

                item(key = "goal") {
                    Column {
                        IosSectionHeader("目标")
                        SegmentedRow(
                            options = listOf("cut" to "减脂", "maintain" to "维持", "bulk" to "增肌"),
                            selected = goal,
                            onSelect = { goal = it },
                        )
                    }
                }

                item(key = "rate") {
                    Column {
                        IosSectionHeader("每周变化速度")
                        SegmentedRow(
                            options = listOf(
                                "0.25" to "0.25 kg",
                                "0.5" to "0.5 kg",
                                "0.75" to "0.75 kg",
                            ),
                            selected = rateText(rate),
                            onSelect = { v -> rate = v.toDoubleOrNull() ?: 0.5 },
                        )
                            }
                }

                item(key = "preview") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(ContinuousRoundedRectangle(22.dp))
                            .background(colors.elevatedBackground)
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "每天目标 ${preview.kcal} kcal",
                                style = IosTypography.subheadline,
                                color = colors.content,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "蛋白 ${preview.protein}g / 碳水 ${preview.carbs}g / 脂肪 ${preview.fat}g",
                            style = IosTypography.footnote,
                            color = colors.secondaryContent,
                        )
                    }
                }
            }
        }
    }

    if (pageMode) {
        // 二级页面：玻璃导航栏 + 返回键 + 保存按钮，滚动时顶栏渐进模糊
        GlasmioPinnedPage(
            navigateBackContentDescription = "返回",
            title = "资料与目标",
            onNavigateBack = onDismiss,
            actions = {
                GlasmioButton(onClick = save, size = GlasmioButtonSize.Small) {
                    Text("保存")
                }
            },
            topBarBlurProgress = edge,
            modifier = Modifier.imePadding(),
        ) { padding ->
            CompositionLocalProvider(
                LocalGlasmioSheetContentPadding provides PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
            ) {
                Column(Modifier.fillMaxSize()) { body() }
            }
        }
    } else {
        GlasmioBottomSheet(
            visible = visible,
            onDismissRequest = onDismiss,
            skipPartiallyExpanded = true,
            title = "资料与目标",
            topBarBlurProgress = edge,
            topBar = {
                GlasmioSheetTopToolbar(
                    title = "资料与目标",
                    navigation = {
                        GlasmioSheetButton(onClick = onDismiss) { Text("取消") }
                    },
                    actions = {
                        GlasmioSheetButton(onClick = save) { Text("保存") }
                    },
                )
            },
        ) {
            body()
        }
    }
}

private fun rateText(value: Double): String =
    if (value == floor(value)) value.toInt().toString() else value.toString()

private fun trimNumber(value: Double): String {
    val r = (value * 10).roundToInt() / 10.0
    return if (r == floor(r)) r.toInt().toString() else r.toString()
}