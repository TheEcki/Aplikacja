package pl.vitaltrack.core.model

/** Estymaty z publicznych wzorów (AHA/WHO, Uth-Sørensen, Keytel) — nie diagnozy. */
object Estimates {
    fun maxHr(user: Int, age: Int): Int =
        if (user > 0) user else Math.round(205.8 - 0.685 * age.coerceAtLeast(1)).toInt()

    fun vo2max(maxHr: Int, resting: Int): Double {
        val r = resting.coerceAtLeast(35)
        return Math.round(10.0 * 15.3 * maxHr / r) / 10.0
    }

    fun vo2class(v: Double): String = when {
        v < 28 -> "niski"; v < 36 -> "przeciętny"; v < 46 -> "dobry"; else -> "bardzo dobry"
    }

    fun cardioMinute(bpm: Int, maxHr: Int): Int {
        if (bpm <= 0 || maxHr <= 0) return 0
        val f = bpm.toDouble() / maxHr
        return when { f >= 0.70 -> 2; f >= 0.50 -> 1; else -> 0 }
    }

    fun kcalPerMinute(bpm: Int, age: Int, weight: Int, male: Boolean): Double {
        if (bpm <= 0 || age <= 0 || weight <= 0) return 0.0
        val base = if (male) (age * 0.2017) + (weight * 0.199) + (bpm * 0.6309) - 55.0969
        else (age * 0.074) + (weight * 0.126) + (bpm * 0.4472) - 20.4022
        return base / 4.184
    }

    data class Zone(val name: String, val colorHex: String, val lo: Double, val hi: Double)
    val ZONES = listOf(
        Zone("Spoczynek", "#94A1B8", 0.00, 0.50),
        Zone("Rozgrzewka", "#60A5FA", 0.50, 0.60),
        Zone("Spalanie", "#34D399", 0.60, 0.70),
        Zone("Kardio", "#FBBF24", 0.70, 0.85),
        Zone("Szczyt", "#F87171", 0.85, 1.01),
    )
    fun zoneOf(bpm: Int, maxHr: Int): Zone {
        val f = if (maxHr > 0) bpm.toDouble() / maxHr else 0.0
        return ZONES.firstOrNull { f >= it.lo && f < it.hi } ?: ZONES.first()
    }
}
