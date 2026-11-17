package com.fitlog.app.ui.sheets

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.ActivityLevels
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.PrimaryCapsule
import com.fitlog.app.ui.components.ValueRow
import com.fitlog.app.ui.theme.FitPalette
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedPage
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioHeaderScrollEdge
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import java.time.Instant
import java.time.ZoneId

/* ============================ 目标（只读） ============================ */

/**
 * 「目标」二级页面：把当前目标完整摊开。
 *
 * 蛋白 / 碳水 / 脂肪 原来是挤在同一行里的「140 / 239 / 56 g」，
 * 现在拆成三行、各带一个宏量色点，扫一眼就知道每一项是多少。
 */
@Composable
fun TargetPage(data: AppData, onDismiss: () -> Unit) {
    val targets = targetsOf(data.profile)
    val p = data.profile
    val goalName = when (p.goal) {
        "cut" -> "减脂"
        "bulk" -> "增肌"
        else -> "维持"
    }
    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)

    GlasmioPinnedPage(
        navigateBackContentDescription = "返回",
        title = "目标",
        onNavigateBack = onDismiss,
        topBarBlurProgress = edge,
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "daily") {
                Column {
                    IosSectionHeader("每日目标")
                    GlasmioGroupedList {
                        GlasmioListRow(
                            title = "热量",
                            detail = "${targets.kcal} kcal",
                            showTopSeparator = false,
                        )
                        GlasmioListRow(
                            title = "蛋白",
                            detail = "${targets.protein} g",
                            leading = { MacroDot(FitPalette.protein) },
                        )
                        GlasmioListRow(
                            title = "碳水",
                            detail = "${targets.carbs} g",
                            leading = { MacroDot(FitPalette.carbs) },
                        )
                        GlasmioListRow(
                            title = "脂肪",
                            detail = "${targets.fat} g",
                            leading = { MacroDot(FitPalette.fat) },
                        )
                    }
                }
            }

            item(key = "body") {
                Column {
                    IosSectionHeader("身体与消耗")
                    GlasmioGroupedList {
                        ValueRow("基础代谢", "${targets.bmr} kcal")
                        ValueRow("总消耗", "${targets.tdee} kcal")
                        ValueRow("身高体重", "${p.height.pretty()} cm · ${p.weight.pretty()} kg")
                        ValueRow("活动量", ActivityLevels.labelOf(p.activity))
                        ValueRow("目标", goalName)
                    }
                }
            }

            item(key = "footer") {
                IosGroupFooter("要改这些数值，去「设置 → 修改资料与目标」。")
            }
        }
    }
}

@Composable
private fun MacroDot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/* ============================ 设置 ============================ */

/**
 * 「设置」二级页面：资料修改、备份地址、导出/导入、上次备份时间。
 *
 * 没有设置备份地址时，「导出备份」仍然走系统文件选择器；
 * 设置之后直接写进那个目录，不再每次都选位置。
 */
@Composable
fun SettingsPage(
    data: AppData,
    backupDir: String?,
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onBackupDir: () -> Unit,
    onExportToDir: () -> Unit,
    buildExport: () -> String,
    onExportDone: () -> Unit,
    onImport: (String) -> Unit,
    onToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)
    var pendingImport by remember { mutableStateOf<String?>(null) }

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

    val lastBackup = data.meta.lastBackup?.let { millis ->
        Dates.short(
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
        )
    } ?: "从未"
    val dirLabel = remember(backupDir) { backupDirLabel(context.contentResolver, backupDir) }

    GlasmioPinnedPage(
        navigateBackContentDescription = "返回",
        title = "设置",
        onNavigateBack = onDismiss,
        topBarBlurProgress = edge,
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "profile") {
                Column {
                    IosSectionHeader("个人")
                    GlasmioGroupedList {
                        GlasmioListRow(
                            title = "修改资料与目标",
                            systemName = "person",
                            showTopSeparator = false,
                            onClick = onEditProfile,
                        )
                    }
                }
            }

            item(key = "backup") {
                Column {
                    IosSectionHeader("备份")
                    GlasmioGroupedList {
                        GlasmioListRow(
                            title = "备份地址设置",
                            subtitle = dirLabel ?: "未设置，导出时会让你选位置",
                            systemName = "folder",
                            showTopSeparator = false,
                            onClick = onBackupDir,
                        )
                        GlasmioListRow(
                            title = "导出备份",
                            systemName = "arrow.up.doc",
                            onClick = {
                                if (backupDir == null) {
                                    exportLauncher.launch("fitlog-${Dates.today()}.json")
                                } else {
                                    onExportToDir()
                                }
                            },
                        )
                        GlasmioListRow(
                            title = "导入恢复",
                            systemName = "arrow.down.circle",
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                        )
                        ValueRow("上次备份", lastBackup)
                    }
                }
            }

            item(key = "note") {
                IosGroupFooter(
                    "数据存在本机的应用私有目录里，卸载应用会一起删掉。" +
                        "设好备份地址后，每周点一下「导出备份」即可，文件直接写到那个目录。",
                )
            }
        }
    }

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

/* ============================ 备份地址（三级） ============================ */

/**
 * 「备份地址设置」三级页面。
 *
 * 选目录走 SAF（ACTION_OPEN_DOCUMENT_TREE），并把读写权限持久化——
 * 这样以后导出备份不用每次都重新选位置。
 */
@Composable
fun BackupDirPage(
    current: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onToast: (String) -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val edge = rememberGlasmioHeaderScrollEdge(listState)
    val label = remember(current) { backupDirLabel(context.contentResolver, current) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // 先尝试读写权限；有的提供方只给读，再退一步只拿读权限
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.isSuccess || runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.isSuccess
            if (persisted) onPick(uri.toString()) else onToast("这个目录没有持久权限，换一个试试")
        }
    }

    GlasmioPinnedPage(
        navigateBackContentDescription = "返回",
        title = "备份地址",
        onNavigateBack = onDismiss,
        topBarBlurProgress = edge,
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "current") {
                Column {
                    IosSectionHeader("当前地址")
                    GlasmioGroupedList {
                        GlasmioListRow(
                            title = label ?: "未设置",
                            subtitle = if (label == null) {
                                "导出备份时会让你手动选位置"
                            } else {
                                "导出备份会直接写到这里"
                            },
                            systemName = "folder",
                            showTopSeparator = false,
                        )
                    }
                }
            }

            item(key = "pick") {
                PrimaryCapsule(
                    text = if (current == null) "选择目录" else "重新选择目录",
                    onClick = { launcher.launch(null) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (current != null) {
                item(key = "clear") {
                    GlasmioGroupedList(Modifier.padding(horizontal = 16.dp)) {
                        GlasmioListRow(
                            title = "清除备份地址",
                            titleColor = GlasmioTheme.colors.destructive,
                            showTopSeparator = false,
                            onClick = onClear,
                        )
                    }
                }
            }

            item(key = "note") {
                IosGroupFooter(
                    "设好之后，去「设置 → 导出备份」会直接写进这个目录，不用每次选位置。" +
                        "目录被删掉、或者换手机之后，需要重新选一次。",
                )
            }
        }
    }
}

/**
 * 把 SAF 的 tree URI 变成用户看得懂的目录名。
 * 拿不到时退回 URI 最后一段，至少不是一片空白。
 */
private fun backupDirLabel(resolver: ContentResolver, uri: String?): String? {
    if (uri.isNullOrBlank()) return null
    return runCatching {
        val tree = Uri.parse(uri)
        val doc = DocumentsContract.buildDocumentUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        resolver.query(doc, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: runCatching { Uri.parse(uri).lastPathSegment }.getOrNull()
}
