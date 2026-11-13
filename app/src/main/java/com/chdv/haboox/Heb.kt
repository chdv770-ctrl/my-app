package com.chdv.haboox

import android.icu.util.HebrewCalendar
import java.util.Locale

/** Offline Hebrew calendar: dates, numerals and holidays. */
object Heb {
    private val ICAL_YEAR = android.icu.util.Calendar.YEAR
    private val ICAL_MONTH = android.icu.util.Calendar.MONTH
    private val ICAL_DAY = android.icu.util.Calendar.DAY_OF_MONTH
    private val ICAL_DOW = android.icu.util.Calendar.DAY_OF_WEEK
    private val ICAL_JD = android.icu.util.Calendar.JULIAN_DAY

    const val SUN = 1
    const val MON = 2
    const val THU = 5
    const val FRI = 6
    const val SAT = 7

    private val months = arrayOf(
        "תשרי", "חשוון", "כסלו", "טבת", "שבט", "אדר א׳", "אדר",
        "ניסן", "אייר", "סיוון", "תמוז", "אב", "אלול"
    )
    val dayNames = arrayOf("", "ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
    val dayLetters = arrayOf("", "א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")

    class HD(val y: Int, val m: Int, val d: Int, val dow: Int, val jd: Int)

    fun isLeap(y: Int): Boolean = ((7 * y + 1) % 19) < 7

    fun monthName(y: Int, m: Int): String = when {
        m == 6 && isLeap(y) -> "אדר ב׳"
        m == 5 && !isLeap(y) -> "אדר"
        else -> months[m]
    }

    fun of(millis: Long): HD {
        val c = HebrewCalendar()
        c.timeInMillis = millis
        return HD(c.get(ICAL_YEAR), c.get(ICAL_MONTH), c.get(ICAL_DAY), c.get(ICAL_DOW), c.get(ICAL_JD))
    }

    private fun jdOf(y: Int, m: Int, d: Int): Int = HebrewCalendar(y, m, d).get(ICAL_JD)

    private fun dowOfJd(jd: Int): Int = ((jd + 1) % 7) + 1

    /** Hebrew numerals (gematria) with geresh / gershayim. */
    fun gem(num: Int): String {
        var x = num
        val sb = StringBuilder()
        val hundreds = intArrayOf(400, 300, 200, 100)
        val hl = arrayOf("ת", "ש", "ר", "ק")
        while (x >= 100) {
            for (i in hundreds.indices) {
                if (x >= hundreds[i]) { sb.append(hl[i]); x -= hundreds[i]; break }
            }
        }
        if (x == 15) { sb.append("טו"); x = 0 }
        if (x == 16) { sb.append("טז"); x = 0 }
        val tens = arrayOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
        val ones = arrayOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
        sb.append(tens[x / 10]).append(ones[x % 10])
        val s = sb.toString()
        return when {
            s.isEmpty() -> ""
            s.length == 1 -> "$s׳"
            else -> s.substring(0, s.length - 1) + "״" + s.last()
        }
    }

    fun yearStr(y: Int): String = gem(y % 1000)

    fun format(h: HD): String = "${gem(h.d)} ב${monthName(h.y, h.m)} ${yearStr(h.y)}"
    fun formatShort(h: HD): String = "${gem(h.d)} ${monthName(h.y, h.m)}"

    fun omer(h: HD): Int {
        val o = h.jd - jdOf(h.y, 7, 16) + 1
        return if (o in 1..49) o else 0
    }

    fun events(h: HD): List<String> {
        val r = ArrayList<String>()
        val y = h.y
        val m = h.m
        val d = h.d
        val dow = h.dow
        val leap = isLeap(y)
        when (m) {
            0 -> {
                when (d) {
                    1, 2 -> r.add("ראש השנה")
                    9 -> r.add("ערב יום כיפור")
                    10 -> r.add("יום כיפור")
                    14 -> r.add("ערב סוכות")
                    15 -> r.add("סוכות")
                    in 16..20 -> r.add("חול המועד סוכות")
                    21 -> r.add("הושענא רבה")
                    22 -> r.add("שמיני עצרת · שמחת תורה")
                }
                if ((d == 3 && dow != SAT) || (d == 4 && dow == SUN)) r.add("צום גדליה")
            }
            3 -> if (d == 10) r.add("עשרה בטבת")
            4 -> if (d == 15) r.add("ט״ו בשבט")
            5 -> if (leap) {
                if (d == 14) r.add("פורים קטן")
                if (d == 15) r.add("שושן פורים קטן")
            }
            6 -> {
                if ((d == 13 && dow != SAT) || (d == 11 && dow == THU)) r.add("תענית אסתר")
                if (d == 14) r.add("פורים")
                if (d == 15) r.add("שושן פורים")
            }
            7 -> {
                when (d) {
                    14 -> r.add("ערב פסח")
                    15 -> r.add("פסח")
                    in 16..20 -> r.add("חול המועד פסח")
                    21 -> r.add("שביעי של פסח")
                }
                if ((d == 27 && dow != FRI && dow != SUN) || (d == 26 && dow == THU) || (d == 28 && dow == MON))
                    r.add("יום השואה")
            }
            8 -> {
                val dw5 = dowOfJd(jdOf(y, 8, 5))
                val atz = when (dw5) { FRI -> 4; SAT -> 3; MON -> 6; else -> 5 }
                if (d == atz - 1) r.add("יום הזיכרון")
                if (d == atz) r.add("יום העצמאות")
                if (d == 14) r.add("פסח שני")
                if (d == 18) r.add("ל״ג בעומר")
                if (d == 28) r.add("יום ירושלים")
            }
            9 -> {
                if (d == 5) r.add("ערב שבועות")
                if (d == 6) r.add("שבועות")
            }
            10 -> if ((d == 17 && dow != SAT) || (d == 18 && dow == SUN)) r.add("צום י״ז בתמוז")
            11 -> {
                if ((d == 9 && dow != SAT) || (d == 10 && dow == SUN)) r.add("תשעה באב")
                if (d == 15) r.add("ט״ו באב")
            }
            12 -> if (d == 29) r.add("ערב ראש השנה")
        }
        val k = h.jd - jdOf(y, 2, 25)
        if (k in 0..7) r.add("חנוכה · נר ${gem(k + 1)}")
        if (d == 30) {
            var nm = m + 1
            if (nm == 5 && !leap) nm = 6
            if (nm <= 12) r.add("ראש חודש ${monthName(y, nm)}")
        }
        if (d == 1 && m != 0) r.add("ראש חודש ${monthName(y, m)}")
        return r
    }

    fun gregLong(millis: Long): String =
        java.text.SimpleDateFormat("EEEE, d בMMMM yyyy", Locale("he")).format(java.util.Date(millis))
}
