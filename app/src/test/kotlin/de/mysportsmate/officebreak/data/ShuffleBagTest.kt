package de.mysportsmate.officebreak.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleBagTest {

    private fun exercise(name: String) = Exercise(name = name, isEnabled = true)

    @Test
    fun `empty list returns null`() {
        val bag = ShuffleBag()
        assertNull(bag.pick(emptyList()))
    }

    @Test
    fun `single exercise always returned`() {
        val bag = ShuffleBag()
        val one = listOf(exercise("A"))
        repeat(5) {
            assertEquals("A", bag.pick(one)?.name)
        }
    }

    @Test
    fun `all enabled exercises are used before any repeat`() {
        val bag = ShuffleBag()
        val list = listOf(exercise("A"), exercise("B"), exercise("C"))
        val picks = (1..3).mapNotNull { bag.pick(list)?.name }
        assertEquals(setOf("A", "B", "C"), picks.toSet())
    }

    @Test
    fun `after refill last pick is not immediately repeated`() {
        val bag = ShuffleBag()
        val list = listOf(exercise("A"), exercise("B"), exercise("C"))
        val picked = (1..4).mapNotNull { bag.pick(list)?.name }
        // At least 3 distinct names in 3 picks; 4th pick (refill) must not equal 3rd.
        assertNotEquals(picked[2], picked[3])
    }

    @Test
    fun `deterministic picker respects available list ordering`() {
        val bag = ShuffleBag(randomPicker = { it.first() })
        val list = listOf(exercise("A"), exercise("B"), exercise("C"))
        assertEquals("A", bag.pick(list)?.name)
        assertEquals("B", bag.pick(list)?.name)
        assertEquals("C", bag.pick(list)?.name)
    }

    @Test
    fun `reset clears state`() {
        val bag = ShuffleBag(randomPicker = { it.first() })
        bag.pick(listOf(exercise("A"), exercise("B")))
        assertTrue(bag.usedNames.isNotEmpty())
        bag.reset()
        assertTrue(bag.usedNames.isEmpty())
        assertNull(bag.lastPickedName)
    }

    @Test
    fun `restore state from saved used and last`() {
        val bag = ShuffleBag(
            initialUsed = setOf("A"),
            initialLast = "A",
            randomPicker = { it.first() },
        )
        val list = listOf(exercise("A"), exercise("B"), exercise("C"))
        // With A used, picker picks first available => B
        assertEquals("B", bag.pick(list)?.name)
    }
}
