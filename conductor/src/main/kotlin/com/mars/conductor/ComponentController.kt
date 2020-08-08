@file:Suppress("LeakingThis", "SpellCheckingInspection")

package com.mars.conductor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/*
 * author: 凛
 * date: 2020/8/5 4:02 PM
 * github: https://github.com/oh-Rin
 * description: A Conductor-Controller with Jetpack architecture components, like CompoentActivity
 */
abstract class ComponentController : Controller(),
  LifecycleOwner,
  ViewModelStoreOwner,
  SavedStateRegistryOwner {

  private lateinit var lifecycleOwner: LifecycleOwner
  private val viewModelStore = ViewModelStore()
  private val savedStateRegistryController = SavedStateRegistryController.create(this)

  override fun onInitialized(controller: Controller, savedViewState: Bundle?) {
    // State.INITIALIZED
    lifecycleOwner = ControllerLifecycleOwner(this)
    savedStateRegistryController.performRestore(savedViewState)
    initViewTreeOwners()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    savedStateRegistryController.performSave(outState)
  }

  private fun initViewTreeOwners() {
    // Set the view tree owners before setting the content view so that the inflation process
    // and attach listeners will see them already present
    ViewTreeLifecycleOwner.set(window.decorView, this)
    ViewTreeViewModelStoreOwner.set(window.decorView, this)
    ViewTreeSavedStateRegistryOwner.set(window.decorView, this)
  }

  override fun getLifecycle(): Lifecycle =
    lifecycleOwner.lifecycle

  override fun getViewModelStore(): ViewModelStore =
    viewModelStore

  override fun getSavedStateRegistry(): SavedStateRegistry =
    savedStateRegistryController.savedStateRegistry

  // ViewModel

  fun <T> LiveData<T>.observe(observer: (T) -> Unit) =
    observe(this@ComponentController) { observer.invoke(it) }

  // Coroutines

  /** @see CoroutineScope.launch */
  fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
  ) = lifecycleScope.launch(context, start, block)

  /** @see CoroutineScope.async */
  fun <T> async(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
  ) = lifecycleScope.async(context, start, block)

  /** @see LifecycleCoroutineScope.launchWhenCreated */
  fun launchWhenCreated(block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launchWhenCreated(block)

  /** @see LifecycleCoroutineScope.launchWhenStarted */
  fun launchWhenStarted(block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launchWhenStarted(block)

  /** @see LifecycleCoroutineScope.launchWhenResumed */
  fun launchWhenResumed(block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launchWhenResumed(block)
}