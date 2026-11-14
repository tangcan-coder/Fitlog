package com.fitlog.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class IconKind {
    Today, Library, Stats, Me,
    Plus, Minus, Close, ChevronRight, ChevronLeft,
    Search, Check, Trash, Copy, Calendar
}

/**
 * 不引第三方图标库，四个标签页图标和几个操作图标都自己画。
 * 好处是风格统一、体积极小，也不会因为图标库版本变动而编译失败。
 */
@Composable
fun FitIcon(
    kind: IconKind,
    tint: Color,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = strokeWidth.toPx()
        val stroke = Stroke(width = w, cap = StrokeCap.Round)

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = w) {
            drawLine(
                color = tint,
                start = Offset(x1 * s, y1 * s),
                end = Offset(x2 * s, y2 * s),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }

        when (kind) {
            IconKind.Today -> {
                drawCircle(tint, radius = s * 0.29f, style = stroke)
                drawCircle(tint, radius = s * 0.085f)
            }

            IconKind.Library -> {
                listOf(0.3f, 0.5f, 0.7f).forEach { y ->
                    line(0.26f, y, 0.78f, y)
                }
            }

            IconKind.Stats -> {
                line(0.26f, 0.78f, 0.26f, 0.46f, s * 0.14f)
                line(0.5f, 0.78f, 0.5f, 0.26f, s * 0.14f)
                line(0.74f, 0.78f, 0.74f, 0.58f, s * 0.14f)
            }

            IconKind.Me -> {
                drawCircle(tint, radius = s * 0.155f, center = Offset(0.5f * s, 0.32f * s), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(0.22f * s, 0.56f * s),
                    size = Size(0.56f * s, 0.42f * s),
                    style = stroke
                )
            }

            IconKind.Plus -> {
                line(0.5f, 0.24f, 0.5f, 0.76f)
                line(0.24f, 0.5f, 0.76f, 0.5f)
            }

            IconKind.Minus -> line(0.24f, 0.5f, 0.76f, 0.5f)

            IconKind.Close -> {
                line(0.28f, 0.28f, 0.72f, 0.72f)
                line(0.72f, 0.28f, 0.28f, 0.72f)
            }

            IconKind.ChevronRight -> {
                val p = Path().apply {
                    moveTo(0.4f * s, 0.24f * s)
                    lineTo(0.64f * s, 0.5f * s)
                    lineTo(0.4f * s, 0.76f * s)
                }
                drawPath(p, tint, style = stroke)
            }

            IconKind.ChevronLeft -> {
                val p = Path().apply {
                    moveTo(0.6f * s, 0.24f * s)
                    lineTo(0.36f * s, 0.5f * s)
                    lineTo(0.6f * s, 0.76f * s)
                }
                drawPath(p, tint, style = stroke)
            }

            IconKind.Search -> {
                drawCircle(tint, radius = s * 0.24f, center = Offset(0.44f * s, 0.44f * s), style = stroke)
                line(0.62f, 0.62f, 0.79f, 0.79f)
            }

            IconKind.Check -> {
                val p = Path().apply {
                    moveTo(0.26f * s, 0.52f * s)
                    lineTo(0.44f * s, 0.69f * s)
                    lineTo(0.75f * s, 0.32f * s)
                }
                drawPath(p, tint, style = stroke)
            }

            IconKind.Trash -> {
                line(0.22f, 0.3f, 0.78f, 0.3f)
                line(0.4f, 0.3f, 0.4f, 0.2f, w * 0.9f)
                line(0.6f, 0.3f, 0.6f, 0.2f, w * 0.9f)
                val p = Path().apply {
                    moveTo(0.3f * s, 0.3f * s)
                    lineTo(0.34f * s, 0.8f * s)
                    lineTo(0.66f * s, 0.8f * s)
                    lineTo(0.7f * s, 0.3f * s)
                }
                drawPath(p, tint, style = stroke)
            }

            IconKind.Copy -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.32f * s, 0.18f * s),
                    size = Size(0.5f * s, 0.5f * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.18f * s, 0.32f * s),
                    size = Size(0.5f * s, 0.5f * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f),
                    style = stroke
                )
            }

            IconKind.Calendar -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.18f * s, 0.26f * s),
                    size = Size(0.64f * s, 0.56f * s),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f),
                    style = stroke
                )
                line(0.18f, 0.44f, 0.82f, 0.44f, w * 0.85f)
                line(0.36f, 0.16f, 0.36f, 0.3f)
                line(0.64f, 0.16f, 0.64f, 0.3f)
            }
        }
    }
}
