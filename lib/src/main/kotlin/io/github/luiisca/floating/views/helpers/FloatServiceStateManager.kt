package io.github.luiisca.floating.views.helpers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the global state of the floating service.
 *
 * This object provides a way to track and update the running state of the service
 * across the entire application.
 */
object FloatServiceStateManager {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

    /**
     * Updates the running state of the service.
     *
     * @param isRunning `true` if the service is starting, `false` if it's stopping
     *
     * This function should be called in the service's lifecycle methods:
     * - Set to `true` in `onCreate()` when the service starts
     * - Set to `false` in `onDestroy()` when the service stops
     */
    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }
}