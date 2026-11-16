package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography

/**
 * 二级页面的外壳：实色顶栏 + 内容区。
 *
 * 弹层改成页面之后，顶栏不再需要玻璃——页面内容就在正下方，玻璃采样自己会形成反馈环路。
 * 所以这里用 iOS 风格的三段式实色栏：左「取消/返回」、中间标题、右「保存/导入」。
 */
@Composable
fun SheetPageScaffold(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backLabel: String = "取消",
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = GlasmioTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                backLabel,
                style = IosTypography.body,
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(Capsule())
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Text(
                title,
                style = IosTypography.headline,
                fontWeight = FontWeight(600),
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 72.dp),
            )
            if (actionLabel != null) {
                Text(
                    actionLabel,
                    style = IosTypography.body,
                    fontWeight = FontWeight(600),
                    color = if (actionEnabled) colors.accent else colors.tertiaryContent,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(Capsule())
                        .clickable(enabled = actionEnabled && onAction != null) {
                            onAction?.invoke()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        content()
    }
}
