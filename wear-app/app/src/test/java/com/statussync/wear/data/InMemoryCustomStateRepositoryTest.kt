package com.statussync.wear.data

import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCustomStateRepositoryTest {
    private fun custom(id: String) = CustomState(id, "S$id", AvailabilityState.Call, 0xFFEC4899, listOf(15))

    private class RecordingRepository : InMemoryCustomStateRepository() {
        val saved = mutableListOf<List<CustomState>>()
        override fun onChanged(states: List<CustomState>) { saved += states }
    }

    @Test
    fun addAndRemoveKeepTheOrder() {
        val repository = InMemoryCustomStateRepository()
        repository.add(custom("1"))
        repository.add(custom("2"))
        repository.add(custom("3"))
        repository.remove("2")
        assertEquals(listOf("1", "3"), repository.states.value.map { it.id })
    }

    @Test
    fun rejectsAdditionsOverTheLimit() {
        val repository = InMemoryCustomStateRepository()
        repeat(CustomState.MAX_COUNT) { assertTrue(repository.add(custom("$it"))) }
        assertFalse(repository.add(custom("extra")))
        assertEquals(CustomState.MAX_COUNT, repository.states.value.size)
    }

    @Test
    fun persistsOnlyWhenSomethingChanges() {
        val repository = RecordingRepository()
        repository.add(custom("1"))
        repeat(CustomState.MAX_COUNT) { repository.add(custom("x$it")) } // el último se rechaza
        val savesBeforeRejected = repository.saved.size
        repository.add(custom("rejected"))
        assertEquals(savesBeforeRejected, repository.saved.size)
        repository.remove("1")
        assertEquals(savesBeforeRejected + 1, repository.saved.size)
    }
}
