package com.meetspot.app.ui.screens

import com.meetspot.app.MainDispatcherRule
import com.meetspot.app.data.FakeLocationHelper
import com.meetspot.app.data.remote.dto.PlaceRecommendation
import com.meetspot.app.data.remote.dto.RecommendResponse
import com.meetspot.app.data.repository.ApiException
import com.meetspot.app.data.repository.FakeRecommendRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class FindViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit does nothing when a required field is blank`() = runTest {
        val fake = FakeRecommendRepository()
        val viewModel = FindViewModel(fake, FakeLocationHelper())
        viewModel.onPersonAChange("UCL")
        // personB and query left blank
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(0, fake.pairCalls.size)
        assertNull(viewModel.uiState.value.results)
    }

    @Test
    fun `successful submit stores the results`() = runTest {
        val fake = FakeRecommendRepository().apply {
            recommendResult = Result.success(
                RecommendResponse(recommendations = listOf(place("Blue Bottle")))
            )
        }
        val viewModel = FindViewModel(fake, FakeLocationHelper())
        viewModel.onPersonAChange("UCL")
        viewModel.onPersonBChange("London Bridge")
        viewModel.onQueryChange("coffee")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, fake.pairCalls.size)
        assertEquals(Triple("UCL", "London Bridge", "coffee"), fake.pairCalls.first())
        assertEquals("Blue Bottle", viewModel.uiState.value.results?.recommendations?.first()?.name)
        assertEquals(false, viewModel.uiState.value.submitting)
    }

    @Test
    fun `failed submit surfaces the error message`() = runTest {
        val fake = FakeRecommendRepository().apply {
            recommendResult = Result.failure(ApiException("The search failed."))
        }
        val viewModel = FindViewModel(fake, FakeLocationHelper())
        viewModel.onPersonAChange("UCL")
        viewModel.onPersonBChange("London Bridge")
        viewModel.onQueryChange("coffee")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("The search failed.", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.results)
    }

    @Test
    fun `useCurrentLocation fills the requested field`() = runTest {
        val locationHelper = FakeLocationHelper().apply { result = Result.success("51.5,-0.13") }
        val viewModel = FindViewModel(FakeRecommendRepository(), locationHelper)

        viewModel.useCurrentLocation(LocationField.PERSON_B)
        advanceUntilIdle()

        assertEquals("51.5,-0.13", viewModel.uiState.value.personB)
        assertEquals("", viewModel.uiState.value.personA)
        assertNull(viewModel.uiState.value.locatingField)
    }

    private fun place(name: String) = PlaceRecommendation(
        id = "p1", name = name, journeys = emptyList(), longest = 10, difference = 2, score = 5.0, explanation = "ok",
    )
}
