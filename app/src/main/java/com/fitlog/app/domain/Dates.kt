package com.fitlog.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Dates {

    private val md = DateTimeFormatter.ofPattern("M月d日")

    fun today(): String = LocalDate.now().toString()

    fun parse(ymd: String): LocalDate =
        runCatching { LocalDate.parse(ymd) }.getOrElse { LocalDate.now() }

    fun shift(ymd: String, days: Long): String = parse(ymd).plusDays(days).toString()

    fun weekdayLabel(date: LocalDate): String {
        val names = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names[date.dayOfWeek.value - 1]
    }

    /** 今天 / 昨天 / 11月14日 周六 */
    fun human(ymd: String): String {
        val t = today()
        if (ymd == t) return "今天"
        if (ymd == shift(t, -1)) return "昨天"
        val d = parse(ymd)
        return md.format(d) + " " + weekdayLabel(d)
    }

    fun short(ymd: String): String = md.format(parse(ymd))
}
