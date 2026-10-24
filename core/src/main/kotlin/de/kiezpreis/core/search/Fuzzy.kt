package de.kiezpreis.core.search

import kotlin.math.abs
import kotlin.math.min

object Fuzzy {

    /** Allowed typos depend on word length, so short words such as "zero" vs "hero" are not confused. */
    fun allowedEdits(length: Int): Int = when {
        length <= 4 -> 0
        length <= 7 -> 1
        else -> 2
    }

    /** Optimal string alignment distance (Damerau-Levenshtein with adjacent swaps), capped at [max] + 1. */
    fun distance(a: String, b: String, max: Int): Int {
        if (abs(a.length - b.length) > max) return max + 1
        if (a == b) return 0
        val rows = a.length + 1
        val cols = b.length + 1
        val d = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) d[i][0] = i
        for (j in 0 until cols) d[0][j] = j
        for (i in 1 until rows) {
            var rowMin = Int.MAX_VALUE
            for (j in 1 until cols) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = min(v, d[i - 2][j - 2] + 1)
                }
                d[i][j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
        }
        return d[a.length][b.length]
    }

    fun isTypo(query: String, candidate: String): Boolean {
        val max = allowedEdits(query.length)
        return max > 0 && distance(query, candidate, max) <= max
    }
}
