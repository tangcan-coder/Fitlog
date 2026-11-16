package com.fitlog.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.ActivityLevels
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.FitListRow
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.ValueRow
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import java.time.Instant
import java.time.ZoneId

/**
 * 「我的」：目标汇总、本地数据、备份与清空。
 * 破坏性操作一律走 iOS 风格确认弹窗。
 */
@Composable
fun MeScreen(
    data: AppData,
    listState: LazyListState,
    bottomPadding: Dp,
    buildExport: () -> String,
    onExportDone: () -> Unit,
    onImport: (String) -> Unit,
    onEditProfile: () -> Unit,
    onClearAll: () -> Unit,
    onToast: (String) -> Unit,
) {
    val colors = GlasmioTheme.colors
    val context = LocalContext.current
    val targets = targetsOf(data.profile)
    val p = data.profile

    var pendingImport by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                val text = buildExport()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
            if (ok) onExportDone() else onToast("导出失败，换个位置再试")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text != null) pendingImport = text else onToast("这个文件读不出来")
        }
    }

    val goalName = when (p.goal) {
        "cut" -> "减脂"
        "bulk" -> "增肌"
        else -> "维持"
    }
    val lastBackup = data.meta.lastBackup?.let { millis ->
        Dates.short(
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
        )
    } ?: "从未"

    GlasmioPinnedListPage(
        title = "我的",
        subtitle = "数据只保存在这台手机上",
        listState = listState,
        bottomPadding = bottomPadding,
    ) {
        item(key = "profile") {
            Column {
                IosSectionHeader("目标")
                GlasmioGroupedList {
                    ValueRow("每日热量", "${targets.kcal} kcal")
                    ValueRow(
                        "蛋白 / 碳水 / 脂肪",
                        "${targets.protein} / ${targets.carbs} / ${targets.fat} g",
                    )
                    ValueRow("基础代谢 / 总消耗", "${targets.bmr} / ${targets.tdee} kcal")
                    ValueRow("身高体重", "${p.height.pretty()} cm · ${p.weight.pretty()} kg")
                    ValueRow("活动量", ActivityLevels.labelOf(p.activity))
                    ValueRow("目标", goalName)
                    FitListRow(
                        "修改资料与目标",
                        titleColor = colors.accent,
                        onClick = onEditProfile,
                    )
                }
            }
        }

        item(key = "data") {
            Column {
                IosSectionHeader("数据")
                GlasmioGroupedList {
                    ValueRow("食物库", "${data.foods.size} 种")
                    ValueRow("记录天数", "${data.logs.count { it.value.isNotEmpty() }} 天")
                    ValueRow("上次备份", lastBackup)
                    FitListRow(
                        "导出备份",
                        systemName = "doc.on.doc",
                        onClick = {
                            exportLauncher.launch("fitlog-${Dates.today()}.json")
                        },
                    )
                    FitListRow(
                        "导入恢复",
                        systemName = "arrow.down.circle",
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                    )
                }
            }
        }

        item(key = "note") {
            Text(
                "数据存在本机的应用私有目录里，卸载应用会一起删掉。建议每周导出一次备份，" +
                    "文件会存到你选择的位置（比如网盘目录），换手机时再导入回来。",
                style = IosTypography.footnote,
                color = colors.secondaryContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ContinuousRoundedRectangle(22.dp))
                    .background(colors.elevatedBackground)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }

        item(key = "danger") {
            GlasmioGroupedList {
                FitListRow(
                    "清空全部数据",
                    titleColor = colors.destructive,
                    showTopSeparator = false,
                    onClick = { confirmClear = true },
                )
            }
        }

        item(key = "footer") {
            IosGroupFooter("FitLog · 本地版，数据不上传。")
            Spacer(Modifier.height(4.dp))
        }
    }

    GlasmioAlertDialog(
        visible = confirmClear,
        onDismissRequest = { confirmClear = false },
        title = "清空全部数据？",
        message = "食物库、所有记录和体重都会删除，无法恢复。建议先导出备份。",
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = "取消",
                role = GlasmioAlertButtonRole.Cancel,
                onClick = { confirmClear = false },
            ),
            GlasmioAlertButtonSpec(
                label = "清空",
                role = GlasmioAlertButtonRole.Destructive,
                onClick = {
                    confirmClear = false
                    onClearAll()
                },
            ),
        ),
    )

    GlasmioAlertDialog(
        visible = pendingImport != null,
        onDismissRequest = { pendingImport = null },
        title = "导入备份？",
        message = "导入会覆盖当前全部数据，确定继续吗？",
        buttons = listOf(
            GlasmioAlertButtonSpec(
                label = "取消",
                role = GlasmioAlertButtonRole.Cancel,
                onClick = { pendingImport = null },
            ),
            GlasmioAlertButtonSpec(
                label = "导入",
                onClick = {
                    val text = pendingImport
                    pendingImport = null
                    if (text != null) onImport(text)
                },
            ),
        ),
    )
}
