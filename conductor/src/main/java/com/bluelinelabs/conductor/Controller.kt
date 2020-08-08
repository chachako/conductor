@file:Suppress("MemberVisibilityCanBePrivate")

package com.bluelinelabs.conductor

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.IdRes
import com.bluelinelabs.conductor.internal.ClassUtils
import com.bluelinelabs.conductor.internal.RouterRequiringFunc
import com.bluelinelabs.conductor.internal.ViewAttachHandler
import com.bluelinelabs.conductor.internal.ViewAttachHandler.ViewAttachListener
import com.mars.conductor.ContextController
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.util.*

/**
 * A Controller manages portions of the UI. It is similar to an Activity or Fragment in that it manages its
 * own lifecycle and controls interactions between the UI and whatever logic is required. It is, however,
 * a much lighter weight component than either Activities or Fragments. While it offers several lifecycle
 * methods, they are much simpler and more predictable than those of Activities and Fragments.
 */
abstract class Controller protected constructor(args: Bundle? = null): ContextController() {
  /**
   * Returns any arguments that were set in this Controller's constructor
   */
  val args: Bundle = args ?: Bundle(javaClass.classLoader)
  var viewState: Bundle? = null
  private var savedInstanceState: Bundle? = null

  /**
   * Returns whether or not this Controller is currently in the process of being destroyed.
   */
  @JvmField var isBeingDestroyed = false

  /**
   * Returns whether or not this Controller has been destroyed.
   */
  var isDestroyed = false
    private set

  /**
   * Returns whether or not this Controller is currently attached to a host View.
   */
  var isAttached = false
    private set
  private var hasOptionsMenu = false
  private var optionsMenuHidden = false
  var viewIsAttached = false
  var viewWasDetached = false

  /**
   * Returns the [Router] object that can be used for pushing or popping other Controllers
   */
  var routerOrNull: Router? = null
    set(value) {
      if (field !== value) {
        field = value
        performOnRestoreInstanceState()
        for (listener in onRouterSetListeners) {
          listener.execute()
        }
        onRouterSetListeners.clear()
      } else {
        performOnRestoreInstanceState()
      }
    }

  val router: Router
    get() = routerOrNull!!

  /**
   * Return this Controller's View or `null` if it has not yet been created or has been
   * destroyed.
   */
  var view: View? = null

  /**
   * Returns this Controller's parent Controller if it is a child Controller or `null` if
   * it has no parent.
   */
  var parentController: Controller? = null

  /**
   * Returns this Controller's instance ID, which is generated when the instance is created and
   * retained across restarts.
   */
  var instanceId: String
  private var targetInstanceId: String? = null
  var needsAttach = false
  private var attachedToUnownedParent = false
  private var awaitingParentAttach = false
  private var hasSavedViewState = false
  var isDetachFrozen = false
    set(frozen) {
      if (field != frozen) {
        field = frozen
        for (router in childRouters) {
          router.setDetachFrozen(frozen)
        }
        if (!frozen && view != null && viewWasDetached) {
          val aView = view!!
          detach(aView, forceViewRefRemoval = false, blockViewRefRemoval = false)
          if (view == null && aView.parent === router.container) {
            router.container.removeView(aView) // need to remove the view when this controller is a child controller
          }
        }
      }
    }

  /**
   * Returns the [ControllerChangeHandler] that should be used for pushing this Controller, or null
   * if the handler from the [RouterTransaction] should be used instead.
   */
  var overriddenPushHandler: ControllerChangeHandler? = null
    private set

  /**
   * Returns the [ControllerChangeHandler] that should be used for popping this Controller, or null
   * if the handler from the [RouterTransaction] should be used instead.
   */
  var overriddenPopHandler: ControllerChangeHandler? = null
    private set
  private var retainViewMode = RetainViewMode.RELEASE_DETACH
  private var viewAttachHandler: ViewAttachHandler? = null
  private val childRouters: MutableList<ControllerHostedRouter> = ArrayList()
  private val lifecycleListeners: MutableList<LifecycleListener> = ArrayList()
  private val requestedPermissions = ArrayList<String>()
  private val onRouterSetListeners = ArrayList<RouterRequiringFunc>()
  private var destroyedView: WeakReference<View>? = null
  private var isPerformingExitTransition = false
  private var isContextAvailable = false

  protected open fun onInitialized(controller: Controller, savedViewState: Bundle?) {}

  /**
   * Called when the controller is ready to display its view. A valid view must be returned. The standard body
   * for this method will be `return inflater.inflate(R.layout.my_layout, container, false);`, plus
   * any binding code.
   *
   * @param inflater       The LayoutInflater that should be used to inflate views
   * @param container      The parent view that this Controller's view will eventually be attached to.
   * This Controller's view should NOT be added in this method. It is simply passed in
   * so that valid LayoutParams can be used during inflation.
   * @param savedViewState A bundle for the view's state, which would have been created in [.onSaveViewState],
   * or `null` if no saved state exists.
   */
  protected abstract fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View

  /**
   * Retrieves the child [Router] for the given container. If no child router for this container
   * exists yet, it will be created.
   *
   * @param container The ViewGroup that hosts the child Router
   */
  fun getChildRouter(container: ViewGroup): Router {
    return getChildRouter(container, null)
  }

  /**
   * Retrieves the child [Router] for the given container/tag combination. If no child router for
   * this container exists yet, it will be created. Note that multiple routers should not exist
   * in the same container unless a lot of care is taken to maintain order between them. Avoid using
   * the same container unless you have a great reason to do so (ex: ViewPagers).
   *
   * @param container The ViewGroup that hosts the child Router
   * @param tag       The router's tag or `null` if none is needed
   */
  fun getChildRouter(container: ViewGroup, tag: String?): Router {
    return getChildRouter(container, tag, true)!!
  }

  /**
   * Retrieves the child [Router] for the given container/tag combination. Note that multiple
   * routers should not exist in the same container unless a lot of care is taken to maintain order
   * between them. Avoid using the same container unless you have a great reason to do so (ex: ViewPagers).
   * The only time this method will return `null` is when the child router does not exist prior
   * to calling this method and the createIfNeeded parameter is set to false.
   *
   * @param container      The ViewGroup that hosts the child Router
   * @param tag            The router's tag or `null` if none is needed
   * @param createIfNeeded If true, a router will be created if one does not yet exist. Else `null` will be returned in this case.
   */
  fun getChildRouter(container: ViewGroup, tag: String?, createIfNeeded: Boolean): Router? {
    @IdRes val containerId = container.id
    check(containerId != View.NO_ID) { "You must set an id on your container." }
    var childRouter: ControllerHostedRouter? = null
    for (router in childRouters) {
      if (router.hostId == containerId && TextUtils.equals(tag, router.tag)) {
        childRouter = router
        break
      }
    }
    if (childRouter == null) {
      if (createIfNeeded) {
        childRouter = ControllerHostedRouter(container.id, tag)
        childRouter.setHost(this, container)
        childRouters.add(childRouter)
        if (isPerformingExitTransition) {
          childRouter.setDetachFrozen(true)
        }
      }
    } else if (!childRouter.hasHost()) {
      childRouter.setHost(this, container)
      childRouter.rebindIfNeeded()
    }
    return childRouter
  }

  /**
   * Removes a child [Router] from this Controller. When removed, all Controllers currently managed by
   * the [Router] will be destroyed.
   *
   * @param childRouter The router to be removed
   */
  fun removeChildRouter(childRouter: Router) {
    if (childRouter is ControllerHostedRouter && childRouters.remove(childRouter)) {
      childRouter.destroy(true)
    }
  }

  /**
   * Returns the host Activity of this Controller's [Router] or `null` if this
   * Controller has not yet been attached to an Activity or if the Activity has been destroyed.
   */
  val activityOrNull: Activity?
    get() = if (routerOrNull != null) router.activity else null
  override val activity: Activity
    get() = activityOrNull!!


  val windowOrNull: Window?
    get() = activityOrNull?.window
  val window: Window
    get() = windowOrNull!!

  /**
   * Returns the Resources from the host Activity or `null` if this Controller has not
   * yet been attached to an Activity or if the Activity has been destroyed.
   */
  val resourcesOrNull: Resources?
    get() = activityOrNull?.resources
  override fun getResources(): Resources = resourcesOrNull!!

  /**
   * Returns the Application Context derived from the host Activity or `null` if this Controller
   * has not yet been attached to an Activity or if the Activity has been destroyed.
   */
  val applicationContextOrNull: Context?
    get() = activityOrNull?.applicationContext

  override fun getApplicationContext(): Context = applicationContextOrNull!!

  /**
   * Returns the Controller with the given instance id or `null` if no such Controller
   * exists. May return the Controller itself or a matching descendant
   *
   * @param instanceId The instance ID being searched for
   */
  fun findController(instanceId: String): Controller? {
    if (this.instanceId == instanceId) {
      return this
    }
    for (router in childRouters) {
      val matchingChild = router.getControllerWithInstanceId(instanceId)
      if (matchingChild != null) {
        return matchingChild
      }
    }
    return null
  }

  /**
   * Returns all of this Controller's child Routers
   */
  fun getChildRouters(): List<Router> {
    val routers: MutableList<Router> = ArrayList(childRouters.size)
    routers.addAll(childRouters)
    return routers
  }
  /**
   * Returns the target Controller that was set with the [.setTargetController]
   * method or `null` if this Controller has no target.
   *
   * @return This Controller's target
   */
  /**
   * Optional target for this Controller. One reason this could be used is to send results back to the Controller
   * that started this one. Target Controllers are retained across instances. It is recommended
   * that Controllers enforce that their target Controller conform to a specific Interface.
   *
   * @param target The Controller that is the target of this one.
   */
  var targetController: Controller?
    get() = if (targetInstanceId != null) {
      router.rootRouter.getControllerWithInstanceId(targetInstanceId!!)
    } else null
    set(target) {
      if (targetInstanceId != null) {
        throw RuntimeException("Target controller already set. A controller's target may only be set once.")
      }
      targetInstanceId = target?.instanceId
    }

  /**
   * Called when this Controller's View is being destroyed. This should overridden to unbind the View
   * from any local variables.
   *
   * @param view The View to which this Controller should be bound.
   */
  protected open fun onDestroyView(view: View) {}

  /**
   * Called when this Controller begins the process of being swapped in or out of the host view.
   *
   * @param changeHandler The [ControllerChangeHandler] that's managing the swap
   * @param changeType    The type of change that's occurring
   */
  protected open fun onChangeStarted(
    changeHandler: ControllerChangeHandler,
    changeType: ControllerChangeType
  ) {
  }

  /**
   * Called when this Controller completes the process of being swapped in or out of the host view.
   *
   * @param changeHandler The [ControllerChangeHandler] that's managing the swap
   * @param changeType    The type of change that occurred
   */
  protected open fun onChangeEnded(
    changeHandler: ControllerChangeHandler,
    changeType: ControllerChangeType
  ) {
  }

  /**
   * Called when this Controller has a Context available to it. This will happen very early on in the lifecycle
   * (before a view is created). If the host activity is re-created (ex: for orientation change), this will be
   * called again when the new context is available.
   */
  protected open fun onContextAvailable(context: Context) {}

  /**
   * Called when this Controller's Context is no longer available. This can happen when the Controller is
   * destroyed or when the host Activity is destroyed.
   */
  protected open fun onContextUnavailable() {}

  /**
   * Called when this Controller is attached to its host ViewGroup
   *
   * @param view The View for this Controller (passed for convenience)
   */
  protected open fun onAttach(view: View) {}

  /**
   * Called when this Controller is detached from its host ViewGroup
   *
   * @param view The View for this Controller (passed for convenience)
   */
  protected open fun onDetach(view: View) {}

  /**
   * Called when this Controller has been destroyed.
   */
  protected open fun onDestroy() {}

  /**
   * Called when this Controller's host Activity is started
   */
  protected fun onActivityStarted(activity: Activity) {}

  /**
   * Called when this Controller's host Activity is resumed
   */
  protected fun onActivityResumed(activity: Activity) {}

  /**
   * Called when this Controller's host Activity is paused
   */
  protected fun onActivityPaused(activity: Activity) {}

  /**
   * Called when this Controller's host Activity is stopped
   */
  protected fun onActivityStopped(activity: Activity) {}

  /**
   * Called to save this Controller's View state. As Views can be detached and destroyed as part of the
   * Controller lifecycle (ex: when another Controller has been pushed on top of it), care should be taken
   * to save anything needed to reconstruct the View.
   *
   * @param view     This Controller's View, passed for convenience
   * @param outState The Bundle into which the View state should be saved
   */
  protected open fun onSaveViewState(view: View, outState: Bundle) {}

  /**
   * Restores data that was saved in the [.onSaveViewState] method. This should be overridden
   * to restore the View's state to where it was before it was destroyed.
   *
   * @param view           This Controller's View, passed for convenience
   * @param savedViewState The bundle that has data to be restored
   */
  protected open fun onRestoreViewState(view: View, savedViewState: Bundle) {}

  /**
   * Called to save this Controller's state in the event that its host Activity is destroyed.
   *
   * @param outState The Bundle into which data should be saved
   */
  protected open fun onSaveInstanceState(outState: Bundle) {}

  /**
   * Restores data that was saved in the [.onSaveInstanceState] method. This should be overridden
   * to restore this Controller's state to where it was before it was destroyed.
   *
   * @param savedInstanceState The bundle that has data to be restored
   */
  protected open fun onRestoreInstanceState(savedInstanceState: Bundle) {}

  /**
   * Calls startActivity(Intent) from this Controller's host Activity.
   */
  override fun startActivity(intent: Intent) {
    executeWithRouter(object : RouterRequiringFunc {
      override fun execute() {
        router.startActivity(intent)
      }
    })
  }

  /**
   * Calls startActivityForResult(Intent, int) from this Controller's host Activity.
   */
  fun startActivityForResult(intent: Intent, requestCode: Int) {
    executeWithRouter(object : RouterRequiringFunc {
      override fun execute() {
        router.startActivityForResult(
          instanceId,
          intent,
          requestCode
        )
      }
    })
  }

  /**
   * Calls startActivityForResult(Intent, int, Bundle) from this Controller's host Activity.
   */
  fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
    executeWithRouter(object : RouterRequiringFunc {
      override fun execute() {
        router.startActivityForResult(
          instanceId,
          intent,
          requestCode,
          options
        )
      }
    })
  }

  /**
   * Calls startIntentSenderForResult(IntentSender, int, Intent, int, int, int, Bundle) from this Controller's host Activity.
   */
  @Throws(SendIntentException::class)
  fun startIntentSenderForResult(
    intent: IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int,
    flagsValues: Int, extraFlags: Int, options: Bundle?
  ) {
    router.startIntentSenderForResult(
      instanceId,
      intent,
      requestCode,
      fillInIntent,
      flagsMask,
      flagsValues,
      extraFlags,
      options
    )
  }

  /**
   * Registers this Controller to handle onActivityResult responses. Calling this method is NOT
   * necessary when calling [.startActivityForResult]
   *
   * @param requestCode The request code being registered for.
   */
  fun registerForActivityResult(requestCode: Int) {
    executeWithRouter(object : RouterRequiringFunc {
      override fun execute() {
        router.registerForActivityResult(
          instanceId,
          requestCode
        )
      }
    })
  }

  /**
   * Should be overridden if this Controller has called startActivityForResult and needs to handle
   * the result.
   *
   * @param requestCode The requestCode passed to startActivityForResult
   * @param resultCode  The resultCode that was returned to the host Activity's onActivityResult method
   * @param data        The data Intent that was returned to the host Activity's onActivityResult method
   */
  open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

  /**
   * Calls requestPermission(String[], int) from this Controller's host Activity. Results for this request,
   * including [.shouldShowRequestPermissionRationale] and
   * [.onRequestPermissionsResult] will be forwarded back to this Controller by the system.
   */
  @TargetApi(Build.VERSION_CODES.M)
  fun requestPermissions(permissions: Array<String>, requestCode: Int) {
    requestedPermissions.addAll(permissions.toList())
    executeWithRouter(object : RouterRequiringFunc {
      override fun execute() {
        router.requestPermissions(
          instanceId,
          permissions,
          requestCode
        )
      }
    })
  }

  /**
   * Gets whether you should show UI with rationale for requesting a permission.
   * {@see android.app.Activity#shouldShowRequestPermissionRationale(String)}
   *
   * @param permission A permission this Controller has requested
   */
  fun shouldShowRequestPermissionRationale(permission: String): Boolean {
    return Build.VERSION.SDK_INT >= 23 && activity.shouldShowRequestPermissionRationale(permission)
  }

  /**
   * Should be overridden if this Controller has requested runtime permissions and needs to handle the user's response.
   *
   * @param requestCode  The requestCode that was used to request the permissions
   * @param permissions  The array of permissions requested
   * @param grantResults The results for each permission requested
   */
  open fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
  }

  /**
   * Should be overridden if this Controller needs to handle the back button being pressed.
   *
   * @return True if this Controller has consumed the back button press, otherwise false
   */
  fun handleBack(): Boolean {
    val childTransactions: MutableList<RouterTransaction> = ArrayList()
    for (childRouter in childRouters) {
      childTransactions.addAll(childRouter.getBackstack())
    }
    childTransactions.sortWith { o1: RouterTransaction, o2: RouterTransaction -> o2.transactionIndex - o1.transactionIndex }
    for (transaction in childTransactions) {
      val childController = transaction.controller
      if (childController.isAttached && childController.router.handleBack()) {
        return true
      }
    }
    return false
  }

  /**
   * Adds a listener for all of this Controller's lifecycle events
   *
   * @param lifecycleListener The listener
   */
  fun addLifecycleListener(lifecycleListener: LifecycleListener) {
    if (!lifecycleListeners.contains(lifecycleListener)) {
      lifecycleListeners.add(lifecycleListener)
    }
  }

  /**
   * Removes a previously added lifecycle listener
   *
   * @param lifecycleListener The listener to be removed
   */
  fun removeLifecycleListener(lifecycleListener: LifecycleListener) {
    lifecycleListeners.remove(lifecycleListener)
  }

  /**
   * Returns this Controller's [RetainViewMode]. Defaults to [RetainViewMode.RELEASE_DETACH].
   */
  fun getRetainViewMode(): RetainViewMode {
    return retainViewMode
  }

  /**
   * Sets this Controller's [RetainViewMode], which will influence when its view will be released.
   * This is useful when a Controller's view hierarchy is expensive to tear down and rebuild.
   */
  fun setRetainViewMode(retainViewMode: RetainViewMode) {
    this.retainViewMode = retainViewMode
    if (this.retainViewMode == RetainViewMode.RELEASE_DETACH && !isAttached) {
      removeViewReference()
    }
  }

  /**
   * Overrides the [ControllerChangeHandler] that should be used for pushing this Controller. If this is a
   * non-null value, it will be used instead of the handler from  the [RouterTransaction].
   */
  fun overridePushHandler(overriddenPushHandler: ControllerChangeHandler?) {
    this.overriddenPushHandler = overriddenPushHandler
  }

  /**
   * Overrides the [ControllerChangeHandler] that should be used for popping this Controller. If this is a
   * non-null value, it will be used instead of the handler from  the [RouterTransaction].
   */
  fun overridePopHandler(overriddenPopHandler: ControllerChangeHandler?) {
    this.overriddenPopHandler = overriddenPopHandler
  }

  /**
   * Registers/unregisters for participation in populating the options menu by receiving options-related
   * callbacks, such as [.onCreateOptionsMenu]
   *
   * @param hasOptionsMenu If true, this controller's options menu callbacks will be called.
   */
  fun setHasOptionsMenu(hasOptionsMenu: Boolean) {
    val invalidate = isAttached && !optionsMenuHidden && this.hasOptionsMenu != hasOptionsMenu
    this.hasOptionsMenu = hasOptionsMenu
    if (invalidate) {
      router.invalidateOptionsMenu()
    }
  }

  /**
   * Sets whether or not this controller's menu items should be visible. This is useful for hiding the
   * controller's options menu items when its UI is hidden, and not just when it is detached from the
   * window (the default).
   *
   * @param optionsMenuHidden Defaults to false. If true, this controller's menu items will not be shown.
   */
  fun setOptionsMenuHidden(optionsMenuHidden: Boolean) {
    val invalidate = isAttached && hasOptionsMenu && this.optionsMenuHidden != optionsMenuHidden
    this.optionsMenuHidden = optionsMenuHidden
    if (invalidate) {
      router.invalidateOptionsMenu()
    }
  }

  /**
   * Adds option items to the host Activity's standard options menu. This will only be called if
   * [.setHasOptionsMenu] has been called.
   *
   * @param menu     The menu into which your options should be placed.
   * @param inflater The inflater that can be used to inflate your menu items.
   */
  open fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {}

  /**
   * Prepare the screen's options menu to be displayed. This is called directly before showing the
   * menu and can be used modify its contents.
   *
   * @param menu The menu that will be displayed
   */
  fun onPrepareOptionsMenu(menu: Menu) {}

  /**
   * Called when an option menu item has been selected by the user.
   *
   * @param item The selected item.
   * @return True if this event has been consumed, false if it has not.
   */
  fun onOptionsItemSelected(item: MenuItem): Boolean {
    return false
  }

  fun prepareForHostDetach() {
    needsAttach = needsAttach || isAttached
    for (router in childRouters) {
      router.prepareForHostDetach()
    }
  }

  fun didRequestPermission(permission: String): Boolean {
    return requestedPermissions.contains(permission)
  }

  fun requestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    requestedPermissions.removeAll(permissions.toList())
    onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  fun onContextAvailable() {
    val context: Context? = router.activity
    if (context != null && !isContextAvailable) {
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preContextAvailable(this)
      }
      isContextAvailable = true
      onContextAvailable(context)
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postContextAvailable(this, context)
      }
    }
    for (childRouter in childRouters) {
      childRouter.onContextAvailable()
    }
  }

  fun executeWithRouter(listener: RouterRequiringFunc) {
    if (routerOrNull != null) {
      listener.execute()
    } else {
      onRouterSetListeners.add(listener)
    }
  }

  fun activityStarted(activity: Activity) {
    if (viewAttachHandler != null) {
      viewAttachHandler!!.onActivityStarted()
    }
    onActivityStarted(activity)
  }

  fun activityResumed(activity: Activity) {
    if (!isAttached && view != null && viewIsAttached) {
      attach(view!!)
    } else if (isAttached) {
      needsAttach = false
      hasSavedViewState = false
    }
    onActivityResumed(activity)
  }

  fun activityPaused(activity: Activity) {
    onActivityPaused(activity)
  }

  fun activityStopped(activity: Activity) {
    val attached = isAttached
    if (viewAttachHandler != null) {
      viewAttachHandler!!.onActivityStopped()
    }
    if (attached && activity.isChangingConfigurations) {
      needsAttach = true
    }
    onActivityStopped(activity)
  }

  fun activityDestroyed(activity: Activity) {
    if (activity.isChangingConfigurations) {
      detach(view!!, forceViewRefRemoval = true, blockViewRefRemoval = false)
    } else {
      destroy(true)
    }
    if (isContextAvailable) {
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preContextUnavailable(this, activity)
      }
      isContextAvailable = false
      onContextUnavailable()
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postContextUnavailable(this)
      }
    }
  }

  fun attach(view: View) {
    attachedToUnownedParent = routerOrNull == null || view.parent !== router.container
    if (attachedToUnownedParent || isBeingDestroyed) {
      return
    }
    if (parentController != null && !parentController!!.isAttached) {
      awaitingParentAttach = true
      return
    } else {
      awaitingParentAttach = false
    }
    hasSavedViewState = false
    var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
    for (lifecycleListener in listeners) {
      lifecycleListener.preAttach(this, view)
    }
    isAttached = true
    needsAttach = router.isActivityStopped
    onAttach(view)
    if (hasOptionsMenu && !optionsMenuHidden) {
      router.invalidateOptionsMenu()
    }
    listeners = ArrayList(lifecycleListeners)
    for (lifecycleListener in listeners) {
      lifecycleListener.postAttach(this@Controller, view)
    }
    for (childRouter in childRouters) {
      for (childTransaction in childRouter.backstack) {
        if (childTransaction.controller.awaitingParentAttach) {
          childTransaction.controller.attach(childTransaction.controller.view!!)
        }
      }
      if (childRouter.hasHost()) {
        childRouter.rebindIfNeeded()
      }
    }
  }

  fun detach(view: View, forceViewRefRemoval: Boolean, blockViewRefRemoval: Boolean) {
    if (!attachedToUnownedParent) {
      for (router in childRouters) {
        router.prepareForHostDetach()
      }
    }
    val removeViewRef =
      !blockViewRefRemoval && (forceViewRefRemoval || retainViewMode == RetainViewMode.RELEASE_DETACH || isBeingDestroyed)
    if (isAttached) {
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preDetach(this, view)
      }
      isAttached = false
      if (!awaitingParentAttach) {
        onDetach(view)
      }
      if (hasOptionsMenu && !optionsMenuHidden) {
        router.invalidateOptionsMenu()
      }
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postDetach(this, view)
      }
    }
    if (removeViewRef) {
      removeViewReference()
    }
  }

  private fun removeViewReference() {
    if (view != null) {
      if (!isBeingDestroyed && !hasSavedViewState) {
        saveViewState(view!!)
      }
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preDestroyView(this, view!!)
      }
      onDestroyView(view!!)
      viewAttachHandler!!.unregisterAttachListener(view)
      viewAttachHandler = null
      viewIsAttached = false
      if (isBeingDestroyed) {
        destroyedView = WeakReference(view)
      }
      view = null
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postDestroyView(this)
      }
      for (childRouter in childRouters) {
        childRouter.removeHost()
      }
    }
    if (isBeingDestroyed) {
      performDestroy()
    }
  }

  fun inflate(parent: ViewGroup): View {
    if (view != null && view!!.parent != null && view!!.parent !== parent) {
      detach(view!!, forceViewRefRemoval = true, blockViewRefRemoval = false)
      removeViewReference()
    }
    if (view == null) {
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preCreateView(this)
      }
      val savedViewState = if (viewState == null) null else viewState!!.getBundle(
        KEY_VIEW_STATE_BUNDLE
      )
      onInitialized(this, savedViewState)
      view = onCreateView(LayoutInflater.from(parent.context), parent, savedViewState)
      check(!(view === parent)) { "Controller's onCreateView method returned the parent ViewGroup. Perhaps you forgot to pass false for LayoutInflater.inflate's attachToRoot parameter?" }
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postCreateView(this, view!!)
      }
      restoreViewState(view!!)
      viewAttachHandler = ViewAttachHandler(object : ViewAttachListener {
        override fun onAttached() {
          viewIsAttached = true
          viewWasDetached = false
          attach(view!!)
        }

        override fun onDetached(fromActivityStop: Boolean) {
          viewIsAttached = false
          viewWasDetached = true
          if (!isDetachFrozen) {
            detach(view!!, false, fromActivityStop)
          }
        }

        override fun onViewDetachAfterStop() {
          if (!isDetachFrozen) {
            detach(view!!, false, false)
          }
        }
      })
      viewAttachHandler!!.listenForAttach(view)
    } else if (retainViewMode == RetainViewMode.RETAIN_DETACH) {
      restoreChildControllerHosts()
    }
    return view!!
  }

  private fun restoreChildControllerHosts() {
    for (childRouter in childRouters) {
      if (!childRouter.hasHost()) {
        val containerView = view!!.findViewById<View>(childRouter.hostId)
        if (containerView != null && containerView is ViewGroup) {
          childRouter.setHost(this, containerView)
          childRouter.rebindIfNeeded()
        }
      }
    }
  }

  private fun performDestroy() {
    if (isContextAvailable) {
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preContextUnavailable(this, activity)
      }
      isContextAvailable = false
      onContextUnavailable()
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postContextUnavailable(this)
      }
    }
    if (!isDestroyed) {
      var listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.preDestroy(this)
      }
      isDestroyed = true
      onDestroy()
      parentController = null
      listeners = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.postDestroy(this)
      }
    }
  }

  fun destroy() {
    destroy(false)
  }

  private fun destroy(removeViews: Boolean) {
    isBeingDestroyed = true
    if (routerOrNull != null) {
      router.unregisterForActivityResults(instanceId)
    }
    for (childRouter in childRouters) {
      childRouter.destroy(false)
    }
    if (!isAttached) {
      removeViewReference()
    } else if (removeViews) {
      detach(view!!, forceViewRefRemoval = true, blockViewRefRemoval = false)
    }
  }

  private fun saveViewState(view: View) {
    hasSavedViewState = true
    viewState = Bundle(javaClass.classLoader)
    val hierarchyState = SparseArray<Parcelable>()
    view.saveHierarchyState(hierarchyState)
    viewState!!.putSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY, hierarchyState)
    val stateBundle = Bundle(javaClass.classLoader)
    onSaveViewState(view, stateBundle)
    viewState!!.putBundle(KEY_VIEW_STATE_BUNDLE, stateBundle)
    val listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
    for (lifecycleListener in listeners) {
      lifecycleListener.onSaveViewState(this, viewState!!)
    }
  }

  private fun restoreViewState(view: View) {
    if (viewState != null) {
      view.restoreHierarchyState(viewState!!.getSparseParcelableArray(KEY_VIEW_STATE_HIERARCHY))
      val savedViewState = viewState!!.getBundle(KEY_VIEW_STATE_BUNDLE)
      savedViewState!!.classLoader = javaClass.classLoader
      onRestoreViewState(view, savedViewState)
      restoreChildControllerHosts()
      val listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.onRestoreViewState(this, viewState!!)
      }
    }
  }

  fun saveInstanceState(): Bundle {
    if (!hasSavedViewState && view != null) {
      saveViewState(view!!)
    }
    val outState = Bundle()
    outState.putString(KEY_CLASS_NAME, javaClass.name)
    outState.putBundle(KEY_VIEW_STATE, viewState)
    outState.putBundle(KEY_ARGS, args)
    outState.putString(KEY_INSTANCE_ID, instanceId)
    outState.putString(KEY_TARGET_INSTANCE_ID, targetInstanceId)
    outState.putStringArrayList(KEY_REQUESTED_PERMISSIONS, requestedPermissions)
    outState.putBoolean(KEY_NEEDS_ATTACH, needsAttach || isAttached)
    outState.putInt(KEY_RETAIN_VIEW_MODE, retainViewMode.ordinal)
    if (overriddenPushHandler != null) {
      outState.putBundle(KEY_OVERRIDDEN_PUSH_HANDLER, overriddenPushHandler!!.toBundle())
    }
    if (overriddenPopHandler != null) {
      outState.putBundle(KEY_OVERRIDDEN_POP_HANDLER, overriddenPopHandler!!.toBundle())
    }
    val childBundles = ArrayList<Bundle>(childRouters.size)
    for (childRouter in childRouters) {
      val routerBundle = Bundle()
      childRouter.saveInstanceState(routerBundle)
      childBundles.add(routerBundle)
    }
    outState.putParcelableArrayList(KEY_CHILD_ROUTERS, childBundles)
    val savedState = Bundle(javaClass.classLoader)
    onSaveInstanceState(savedState)
    val listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
    for (lifecycleListener in listeners) {
      lifecycleListener.onSaveInstanceState(this, savedState)
    }
    outState.putBundle(KEY_SAVED_STATE, savedState)
    return outState
  }

  private fun restoreInstanceState(savedInstanceState: Bundle) {
    viewState = savedInstanceState.getBundle(KEY_VIEW_STATE)
    if (viewState != null) {
      viewState!!.classLoader = javaClass.classLoader
    }
    instanceId = savedInstanceState.getString(KEY_INSTANCE_ID)!!
    targetInstanceId = savedInstanceState.getString(KEY_TARGET_INSTANCE_ID)
    requestedPermissions.addAll(savedInstanceState.getStringArrayList(KEY_REQUESTED_PERMISSIONS)!!)
    overriddenPushHandler = ControllerChangeHandler.fromBundle(
      savedInstanceState.getBundle(
        KEY_OVERRIDDEN_PUSH_HANDLER
      )
    )
    overriddenPopHandler = ControllerChangeHandler.fromBundle(
      savedInstanceState.getBundle(
        KEY_OVERRIDDEN_POP_HANDLER
      )
    )
    needsAttach = savedInstanceState.getBoolean(KEY_NEEDS_ATTACH)
    retainViewMode = RetainViewMode.values()[savedInstanceState.getInt(KEY_RETAIN_VIEW_MODE, 0)]
    val childBundles: List<Bundle>? = savedInstanceState.getParcelableArrayList(
      KEY_CHILD_ROUTERS
    )
    for (childBundle in childBundles!!) {
      val childRouter = ControllerHostedRouter()
      childRouter.restoreInstanceState(childBundle)
      childRouters.add(childRouter)
    }
    this.savedInstanceState = savedInstanceState.getBundle(KEY_SAVED_STATE)
    if (this.savedInstanceState != null) {
      this.savedInstanceState!!.classLoader = javaClass.classLoader
    }
    performOnRestoreInstanceState()
  }

  private fun performOnRestoreInstanceState() {
    if (savedInstanceState != null && routerOrNull != null) {
      onRestoreInstanceState(savedInstanceState!!)
      val listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
      for (lifecycleListener in listeners) {
        lifecycleListener.onRestoreInstanceState(this, savedInstanceState!!)
      }
      savedInstanceState = null
    }
  }

  fun changeStarted(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
    if (!changeType.isEnter) {
      isPerformingExitTransition = true
      for (router in childRouters) {
        router.setDetachFrozen(true)
      }
    }
    onChangeStarted(changeHandler, changeType)
    val listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
    for (lifecycleListener in listeners) {
      lifecycleListener.onChangeStart(this, changeHandler, changeType)
    }
  }

  fun changeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
    if (!changeType.isEnter) {
      isPerformingExitTransition = false
      for (router in childRouters) {
        router.setDetachFrozen(false)
      }
    }
    onChangeEnded(changeHandler, changeType)
    val listeners: List<LifecycleListener> = ArrayList(lifecycleListeners)
    for (lifecycleListener in listeners) {
      lifecycleListener.onChangeEnd(this, changeHandler, changeType)
    }
    if (isBeingDestroyed && !viewIsAttached && !isAttached && destroyedView != null) {
      val view = destroyedView!!.get()
      if (router.container != null && view != null && view.parent === router.container) {
        router.container.removeView(view)
      }
      destroyedView = null
    }
    changeHandler.onEnd()
  }

  fun createOptionsMenu(menu: Menu, inflater: MenuInflater) {
    if (isAttached && hasOptionsMenu && !optionsMenuHidden) {
      onCreateOptionsMenu(menu, inflater)
    }
  }

  fun prepareOptionsMenu(menu: Menu) {
    if (isAttached && hasOptionsMenu && !optionsMenuHidden) {
      onPrepareOptionsMenu(menu)
    }
  }

  fun optionsItemSelected(item: MenuItem): Boolean {
    return isAttached && hasOptionsMenu && !optionsMenuHidden && onOptionsItemSelected(item)
  }

  private fun ensureRequiredConstructor() {
    val constructors = javaClass.constructors
    if (getBundleConstructor(constructors) == null && getDefaultConstructor(constructors) == null) {
      throw RuntimeException("$javaClass does not have a constructor that takes a Bundle argument or a default constructor. Controllers must have one of these in order to restore their states.")
    }
  }

  /**
   * Modes that will influence when the Controller will allow its view to be destroyed
   */
  enum class RetainViewMode {
    /**
     * The Controller will release its reference to its view as soon as it is detached.
     */
    RELEASE_DETACH,

    /**
     * The Controller will retain its reference to its view when detached, but will still release the reference when a config change occurs.
     */
    RETAIN_DETACH
  }

  /**
   * Allows external classes to listen for lifecycle events in a Controller
   */
  open class LifecycleListener {
    open fun onChangeStart(
      controller: Controller,
      changeHandler: ControllerChangeHandler,
      changeType: ControllerChangeType
    ) {
    }

    open fun onChangeEnd(
      controller: Controller,
      changeHandler: ControllerChangeHandler,
      changeType: ControllerChangeType
    ) {
    }

    open fun preCreateView(controller: Controller) {}
    open fun postCreateView(controller: Controller, view: View) {}
    open fun preAttach(controller: Controller, view: View) {}
    open fun postAttach(controller: Controller, view: View) {}
    open fun preDetach(controller: Controller, view: View) {}
    open fun postDetach(controller: Controller, view: View) {}
    open fun preDestroyView(controller: Controller, view: View) {}
    open fun postDestroyView(controller: Controller) {}
    open fun preDestroy(controller: Controller) {}
    open fun postDestroy(controller: Controller) {}
    open fun preContextAvailable(controller: Controller) {}
    open fun postContextAvailable(controller: Controller, context: Context) {}
    open fun preContextUnavailable(controller: Controller, context: Context) {}
    open fun postContextUnavailable(controller: Controller) {}
    open fun onSaveInstanceState(controller: Controller, outState: Bundle) {}
    open fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {}
    open fun onSaveViewState(controller: Controller, outState: Bundle) {}
    open fun onRestoreViewState(controller: Controller, savedViewState: Bundle) {}
  }

  companion object {
    private const val KEY_CLASS_NAME = "Controller.className"
    private const val KEY_VIEW_STATE = "Controller.viewState"
    private const val KEY_CHILD_ROUTERS = "Controller.childRouters"
    private const val KEY_SAVED_STATE = "Controller.savedState"
    private const val KEY_INSTANCE_ID = "Controller.instanceId"
    private const val KEY_TARGET_INSTANCE_ID = "Controller.target.instanceId"
    private const val KEY_ARGS = "Controller.args"
    private const val KEY_NEEDS_ATTACH = "Controller.needsAttach"
    private const val KEY_REQUESTED_PERMISSIONS = "Controller.requestedPermissions"
    private const val KEY_OVERRIDDEN_PUSH_HANDLER = "Controller.overriddenPushHandler"
    private const val KEY_OVERRIDDEN_POP_HANDLER = "Controller.overriddenPopHandler"
    private const val KEY_VIEW_STATE_HIERARCHY = "Controller.viewState.hierarchy"
    const val KEY_VIEW_STATE_BUNDLE = "Controller.viewState.bundle"
    private const val KEY_RETAIN_VIEW_MODE = "Controller.retainViewMode"
    fun newInstance(bundle: Bundle): Controller {
      val className = bundle.getString(KEY_CLASS_NAME)
      val cls = ClassUtils.classForName<Any>(className!!, false)
      val constructors = cls!!.constructors
      val bundleConstructor = getBundleConstructor(constructors)
      val args = bundle.getBundle(KEY_ARGS)
      if (args != null) {
        args.classLoader = cls.classLoader
      }
      val controller: Controller
      try {
        if (bundleConstructor != null) {
          controller = bundleConstructor.newInstance(args) as Controller
        } else {
          controller = getDefaultConstructor(constructors)!!.newInstance() as Controller

          // Restore the args that existed before the last process death
          if (args != null) {
            controller.args.putAll(args)
          }
        }
      } catch (e: Exception) {
        throw RuntimeException(
          "An exception occurred while creating a new instance of " + className + ". " + e.message,
          e
        )
      }
      controller.restoreInstanceState(bundle)
      return controller
    }

    private fun getDefaultConstructor(constructors: Array<Constructor<*>>): Constructor<*>? {
      for (constructor in constructors) {
        if (constructor.parameterTypes.isEmpty()) {
          return constructor
        }
      }
      return null
    }

    private fun getBundleConstructor(constructors: Array<Constructor<*>>): Constructor<*>? {
      for (constructor in constructors) {
        if (constructor.parameterTypes.size == 1 && constructor.parameterTypes[0] == Bundle::class.java) {
          return constructor
        }
      }
      return null
    }
  }
  /**
   * Constructor that takes arguments that need to be retained across restarts.
   *
   * @param args Any arguments that need to be retained.
   */
  /**
   * Convenience constructor for use when no arguments are needed.
   */
  init {
    instanceId = UUID.randomUUID().toString()
    ensureRequiredConstructor()
  }
}