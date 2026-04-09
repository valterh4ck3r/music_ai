package com.valter.music_ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MainViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        // Set the main dispatcher to the test dispatcher for coroutine handling
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher after each test
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should show splash screen`() = runTest {
        // Initialize the ViewModel
        viewModel = MainViewModel()
        
        // At the beginning, the splash screen should be visible
        assertTrue("Splash screen should be visible initially", viewModel.showSplash.value)
    }

    @Test
    fun `splash screen should hide after 1000ms delay`() = runTest {
        // Initialize the ViewModel
        viewModel = MainViewModel()
        
        // Wait for 1000ms (the delay in MainViewModel)
        advanceTimeBy(1001)
        
        // After the delay, the splash screen should be hidden
        assertFalse("Splash screen should be hidden after 1000ms delay", viewModel.showSplash.value)
    }
}
