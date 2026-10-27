package com.occamzchainsaw.straightscorer.viewmodels

import com.occamzchainsaw.straightscorer.data.rules.RulesRepository
import com.occamzchainsaw.straightscorer.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RulesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val markdown = """
        # 1 Fouls

        A foul costs a point. Another foul costs another point.

        # 2 Breaking

        See [Fouls](#fouls).
    """.trimIndent()

    private fun viewModel(repository: RulesRepository) = RulesViewModel(repository, mainDispatcherRule.dispatcher)

    private val RulesViewModel.loaded get() = uiState.value as RulesUiState.Loaded

    @Test
    fun loadsAndParsesTheRules() {
        val vm = viewModel(FakeRules(markdown))

        assertEquals(2, vm.loaded.document.tableOfContents.size)
        assertEquals(0, vm.loaded.document.resolveAnchor("#fouls"))
    }

    @Test
    fun failedLoad_showsError() {
        val vm = viewModel(object : RulesRepository {
            override suspend fun loadRules(): String = error("missing asset")
        })

        assertEquals(RulesUiState.Error, vm.uiState.value)
    }

    @Test
    fun search_stepsThroughMatchesAndWraps() {
        val vm = viewModel(FakeRules(markdown))
        vm.openSearch()
        vm.setQuery("foul")

        val matches = vm.loaded.search.matches
        assertEquals(4, matches.size)
        assertEquals(0, vm.loaded.search.currentIndex)

        vm.previousMatch()
        assertEquals(3, vm.loaded.search.currentIndex)
        vm.nextMatch()
        assertEquals(0, vm.loaded.search.currentIndex)
    }

    @Test
    fun closingSearch_resetsIt() {
        val vm = viewModel(FakeRules(markdown))
        vm.openSearch()
        vm.setQuery("foul")
        vm.closeSearch()

        assertFalse(vm.loaded.search.isActive)
        assertTrue(vm.loaded.search.matches.isEmpty())
    }

    private class FakeRules(private val text: String) : RulesRepository {
        override suspend fun loadRules() = text
    }
}
