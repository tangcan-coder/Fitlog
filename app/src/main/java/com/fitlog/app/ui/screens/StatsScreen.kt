package com.fitlog.app.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.domain.totalsOf
import com.fitlog.app.ui.components.EmptyState
import com.fitlog.app.ui.components.FitCard
import com.fitlog.app.ui.components.FitTextField
import com.fitlog.app.ui.components.PrimaryButton
import com.fitlog.app.ui.components.SectionTitle
import com.fitlog.app.ui.components.WeekBarChart
import com.fitlog.app.ui.components.WeightLineChart
import com.fitlog.app.ui.theme.FitTheme
import kotlin.math.abs

@Composable
fun StatsScreen(
    data: AppData,
    onAddWeight: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    val targets = targetsOf(data.profile)

    val days = (6 downTo 0).map { Dates.shift(Dates.today(), -it.toLong()) }
    val values = days.map { totalsOf(data.logs[it].orEmpty()).kcal }
    val labels = days.map { d ->
        "日一二三四五六"[Dates.parse(d).dayOfWeek.value % 7].toString()
    }

    val recorded = values.count { it > 0 }
    val avg = if (recorded == 0) 0 else values.sum() / recorded
    val hit = values.count { it > 0 && abs(it - targets.kcal) <= targets.kcal * 0.1 }

    val weights = data.weights.sortedBy { it.date }.takeLast(30)
    var weightInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 118.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 16.dp)) {
                Text("统计", color = c.text, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("最近 7 天的摄入与体重", color = c.text3, fontSize = 12.sp)
            }
        }

        item {
            FitCard {
                WeekBarChart(values = values, target = targets.kcal, labels = labels)
                Spacer(Modifier.height(10.dp))
                Text(
                    "虚线是每天的目标 ${targets.kcal} kcal",
                    color = c.text3,
                    fontSize = 11.5.sp
                )
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FitCard(modifier = Modifier.weight(1f), padding = 15.dp) {
                    Text("日均摄入 · $recorded 天", color = c.text3, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            avg.toString(),
                            color = c.text,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(" kcal", color = c.text3, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                FitCard(modifier = Modifier.weight(1f), padding = 15.dp) {
                    Text("达标天数", color = c.text3, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            hit.toString(),
                            color = c.text,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(" / 7 天", color = c.text3, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
            }
        }

        item {
            SectionTitle("体重趋势", trailing = if (weights.isEmpty()) "" else "近 ${weights.size} 次")
        }

        item {
            FitCard {
                if (weights.isEmpty()) {
                    EmptyState(text = "还没有体重记录")
                } else {
                    WeightLineChart(points = weights.map { it.kg })
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "起 ${trimKg(weights.first().kg)} kg · ${Dates.short(weights.first().date)}",
                            color = c.text3,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "最新 ${trimKg(weights.last().kg)} kg · ${Dates.short(weights.last().date)}",
                            color = c.text2,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FitTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        placeholder = "今天的体重 kg",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(9.dp))
                    PrimaryButton("记录") {
                        val v = weightInput.toDoubleOrNull()
                        if (v != null) {
                            onAddWeight(v)
                            weightInput = ""
                        }
                    }
                }
            }
        }

        item {
            Text(
                "建议每天早晨空腹、上完厕所后称，看周平均而不是单日。",
                color = c.text3,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }
    }
}

private fun trimKg(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}
