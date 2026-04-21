package de.mysportsmate.officebreak.data

/**
 * Exercise shuffle-bag. Picks a random exercise from the enabled set, tracking
 * used names so the same exercise isn't picked twice in a row (unless there's
 * only one enabled exercise). When all names have been used, the bag refills —
 * keeping the most recent pick out of the refill to avoid back-to-back repeats.
 */
class ShuffleBag(
    initialUsed: Set<String> = emptySet(),
    initialLast: String? = null,
    private val randomPicker: (List<Exercise>) -> Exercise = { it.random() },
) {
    private val used: MutableSet<String> = initialUsed.toMutableSet()
    var lastPickedName: String? = initialLast
        private set

    val usedNames: Set<String> get() = used.toSet()

    /** Returns null if no exercises are enabled. Mutates used + lastPickedName. */
    fun pick(enabled: List<Exercise>): Exercise? {
        if (enabled.isEmpty()) return null

        val last = lastPickedName
        if (last != null && last !in used) used.add(last)

        var available = enabled.filter { it.name !in used }
        if (available.isEmpty()) {
            used.clear()
            if (last != null && enabled.size > 1) used.add(last)
            available = enabled.filter { it.name !in used }
        }
        val picked = randomPicker(available)
        used.add(picked.name)
        lastPickedName = picked.name
        return picked
    }

    fun reset() {
        used.clear()
        lastPickedName = null
    }
}
