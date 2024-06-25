package com.example.cupcake

import android.view.View
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class FloatViewLifecycleOwner() :
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    private var _view: View? = null
    private var recomposer: Recomposer? = null
    private var runRecomposeScope: CoroutineScope? = null
    private var coroutineContext: CoroutineContext? = null

    /**
    Compose uses the Window's decor view to locate the
    Lifecycle/ViewModel/SavedStateRegistry owners.
    Therefore, we need to set this class as the "owner" for the decor view.
     */
    fun attachToDecorView(decorView: View?) {
        if (decorView == null) return
        this._view = decorView

        decorView.setViewTreeLifecycleOwner(this)
        decorView.setViewTreeViewModelStoreOwner(this)
        decorView.setViewTreeSavedStateRegistryOwner(this)
    }

    init {
        coroutineContext = AndroidUiDispatcher.CurrentThread
    }
    // LifecycleOwner methods
    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        runRecomposeScope?.cancel()
        runRecomposeScope = CoroutineScope(coroutineContext!!)
        recomposer = Recomposer(coroutineContext!!)
        this._view?.compositionContext = recomposer

        runRecomposeScope!!.launch {
            recomposer!!.runRecomposeAndApplyChanges()
        }
    }
    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }
    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }
    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}