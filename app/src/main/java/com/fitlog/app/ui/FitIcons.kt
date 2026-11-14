package com.fitlog.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver

/**
 * 底栏的两个 Tab 图标。
 *
 * Glasmio 的底栏只接受 `SfSymbol` 枚举；应用没有（也不应该带上）苹果的符号字体，
 * 因此这里用 [GlasmioIconResolver] 按语义名替换成自己的矢量图标。其余图标
 * （加号、垃圾桶、日历等）返回 null，交给 Glasmio 内置的 Material 回退图标。
 */
object FitIconResolver : GlasmioIconResolver {
    override fun resolve(systemName: String): ImageVector? = when (systemName) {
        "square.3.layers.3d" -> FitVectors.Food
        "waveform" -> FitVectors.Stats
        else -> null
    }
}

object FitVectors {

    /** 食物库：一副刀叉。 */
    val Food: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitVectors.Food",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // 叉子的三个齿
                moveTo(5.1f, 2.2f)
                lineTo(6.5f, 2.2f)
                lineTo(6.5f, 7.0f)
                lineTo(5.1f, 7.0f)
                close()
                moveTo(7.8f, 2.2f)
                lineTo(9.2f, 2.2f)
                lineTo(9.2f, 7.0f)
                lineTo(7.8f, 7.0f)
                close()
                moveTo(10.5f, 2.2f)
                lineTo(11.9f, 2.2f)
                lineTo(11.9f, 7.0f)
                lineTo(10.5f, 7.0f)
                close()
                // 叉子的颈与手柄
                moveTo(5.1f, 6.6f)
                lineTo(11.9f, 6.6f)
                lineTo(11.9f, 8.1f)
                curveTo(11.9f, 9.7f, 10.7f, 10.8f, 9.2f, 11.1f)
                lineTo(9.2f, 21.8f)
                lineTo(7.8f, 21.8f)
                lineTo(7.8f, 11.1f)
                curveTo(6.3f, 10.8f, 5.1f, 9.7f, 5.1f, 8.1f)
                close()
                // 餐刀
                moveTo(16.4f, 2.2f)
                curveTo(18.4f, 3.6f, 19.5f, 6.0f, 19.5f, 8.8f)
                lineTo(19.5f, 12.2f)
                lineTo(17.0f, 12.2f)
                lineTo(17.0f, 21.8f)
                lineTo(15.6f, 21.8f)
                lineTo(15.6f, 12.2f)
                lineTo(13.1f, 12.2f)
                lineTo(13.1f, 8.8f)
                curveTo(13.1f, 6.0f, 14.2f, 3.6f, 16.2f, 2.2f)
                close()
            }
        }.build()
    }

    /** 统计：三根递增的柱子。 */
    val Stats: ImageVector by lazy {
        ImageVector.Builder(
            name = "FitVectors.Stats",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3.2f, 13.4f)
                lineTo(7.2f, 13.4f)
                lineTo(7.2f, 21.6f)
                lineTo(3.2f, 21.6f)
                close()
                moveTo(10.0f, 8.4f)
                lineTo(14.0f, 8.4f)
                lineTo(14.0f, 21.6f)
                lineTo(10.0f, 21.6f)
                close()
                moveTo(16.8f, 2.4f)
                lineTo(20.8f, 2.4f)
                lineTo(20.8f, 21.6f)
                lineTo(16.8f, 21.6f)
                close()
            }
        }.build()
    }
}
