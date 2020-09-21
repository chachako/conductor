@file:Suppress("LeakingThis", "SpellCheckingInspection")

package com.mars.conductor

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/*
 * author: 凛
 * date: 2020/8/5 4:02 PM
 * github: https://github.com/oh-Rin
 * description: A Conductor-Controller with Jetpack architecture components, like CompoentActivity
 */
abstract class ComponentController : Controller(),
  LifecycleOwner,
  ViewModelStoreOwner,
  SavedStateRegistryOwner,
  CoroutineScope {

  private lateinit var lifecycleOwner: LifecycleOwner
  private val savedStateRegistryController = SavedStateRegistryController.create(this)
  private val viewModelStore = ViewModelStore()

  override val coroutineContext: CoroutineContext
    get() = lifecycleScope.coroutineContext

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

  fun <T> LiveData<T>.observe(onChanged: (T) -> Unit): Observer<T> =
    observe(owner = this@ComponentController, onChanged = onChanged)
}