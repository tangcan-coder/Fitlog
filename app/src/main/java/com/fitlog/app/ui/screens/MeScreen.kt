package com.fitlog.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.ActivityLevels
import com.fitlog.app.data.AppData

import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.FitCard
import com.fitlog.app.ui.components.GhostButton
import com.fitlog.app.ui.components.IconKind
import com.fitlog.app.ui.components.SectionTitle
import com.fitlog.app.ui.theme.FitTheme

@Composable
fun MeScreen(
    data: AppData,
    buildExport: () -> String,
    onExportDone: () -> Unit,
    onImport: (String) -> Unit,
    onEditProfile: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    val context = LocalContext.current
    val targets = targetsOf(data.profile)
    val p = data.profile

    var pendingImport by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val text = buildExport()
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                }
            }
            onExportDone()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text != null) pendingImport = text
        }
    }

    val goalName = when (p.goal) {
        "cut" -> "减脂"
        "bulk" -> "增肌"
        else -> "维持"
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 118.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 16.dp)) {
                Text("我的", color = c.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("数据只保存在这台手机上", color = c.text3, fontSize = 12.sp)
            }
        }

        item {
            FitCard {
                KvRow("目标", "$goalName · 每天 ${targets.kcal} kcal")
                KvRow("蛋白 / 碳水 / 脂肪", "${targets.protein} / ${targets.carbs} / ${targets.fat} g")
                KvRow("基础代谢 / 总消耗", "${targets.bmr} / ${targets.tdee} kcal")
                KvRow("身高体重", "${p.height.pretty()} cm · ${p.weight.pretty()} kg")
                KvRow("活动量", ActivityLevels.labelOf(p.activity))
                Spacer(Modifier.height(12.dp))
                GhostButton("修改资料与目标", Modifier.fillMaxWidth()) { onEditProfile() }
            }
        }

        item { SectionTitle("数据") }

        item {
            FitCard {
                KvRow("食物库", "${data.foods.size} 种")
                KvRow(
                    "记录天数",
                    "${data.logs.count { it.value.isNotEmpty() }} 天"
                )
                KvRow(
                    "上次备份",
                    data.meta.lastBackup?.let { Dates.short(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()) }
                        ?: "从未"
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    GhostButton(
                        "导出备份",
                        Modifier.weight(1f),
                        icon = IconKind.Copy
                    ) {
                        val name = "fitlog-${Dates.today()}.json"
                        exportLauncher.launch(name)
                    }
                    GhostButton(
                        "导入恢复",
                        Modifier.weight(1f),
                        icon = IconKind.Check
                    ) {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                }
            }
        }

        item {
            FitCard(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    "数据存在本机的应用私有目录里，卸载应用会一起删掉。建议每周导出一次备份，" +
                        "文件会存到你选择的位置（比如网盘目录），换手机时再导入回来。",
                    color = c.text2,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            GhostButton(
                "清空全部数据",
                Modifier.fillMaxWidth(),
                color = c.danger,
                icon = IconKind.Trash
            ) { confirmClear = true }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text(
                "FitLog · 本地版，数据不上传",
                color = c.text3,
                fontSize = 11.sp
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部数据？") },
            text = { Text("食物库、所有记录和体重都会删除，无法恢复。建议先导出备份。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("确定清空", color = c.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消", color = c.text2) }
            },
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.text2
        )
    }

    val text = pendingImport
    if (text != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("导入备份？") },
            text = { Text("导入会覆盖当前全部数据。确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    onImport(text)
                }) { Text("导入", color = c.accentText) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("取消", color = c.text2) }
            },
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.text2
        )
    }
}

@Composable
private fun KvRow(label: String, value: String) {
    val c = FitTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = c.text3, fontSize = 12.5.sp)
        Spacer(Modifier.weight(1f).width(10.dp))
        Text(value, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
