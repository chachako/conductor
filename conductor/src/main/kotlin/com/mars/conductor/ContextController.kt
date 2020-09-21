@file:Suppress("SpellCheckingInspection", "MissingPermission")

package com.mars.conductor

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.view.Display
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executor

/*
 * author: 凛
 * date: 2020/8/6 11:21 PM
 * github: https://github.com/oh-eRin
 * description: controller based on context
 */
abstract class ContextController : Context() {
  abstract val activity: Activity

  @RequiresApi(Build.VERSION_CODES.Q)
  override fun bindIsolatedService(
    service: Intent,
    flags: Int,
    instanceName: String,
    executor: Executor,
    conn: ServiceConnection
  ): Boolean {
    return activity.bindIsolatedService(service, flags, instanceName, executor, conn)
  }

  override fun bindService(service: Intent?, conn: ServiceConnection, flags: Int): Boolean {
    return activity.bindService(service, conn, flags)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  override fun bindService(
    service: Intent,
    flags: Int,
    executor: Executor,
    conn: ServiceConnection
  ): Boolean {
    return activity.bindService(service, flags, executor, conn)
  }

  override fun checkCallingOrSelfPermission(permission: String): Int {
    return activity.checkCallingOrSelfPermission(permission)
  }

  override fun checkCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int): Int {
    return activity.checkCallingOrSelfUriPermission(uri, modeFlags)
  }

  override fun checkCallingPermission(permission: String): Int {
    return activity.checkCallingPermission(permission)
  }

  override fun checkCallingUriPermission(uri: Uri?, modeFlags: Int): Int {
    return activity.checkCallingUriPermission(uri, modeFlags)
  }

  override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
    return activity.checkPermission(permission, pid, uid)
  }

  @RequiresApi(Build.VERSION_CODES.M)
  override fun checkSelfPermission(permission: String): Int {
    return activity.checkSelfPermission(permission)
  }

  override fun checkUriPermission(uri: Uri?, pid: Int, uid: Int, modeFlags: Int): Int {
    return activity.checkUriPermission(uri, pid, uid, modeFlags)
  }

  override fun checkUriPermission(
    uri: Uri?,
    readPermission: String?,
    writePermission: String?,
    pid: Int,
    uid: Int,
    modeFlags: Int
  ): Int {
    return activity.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags)
  }

  override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
    return activity.createConfigurationContext(overrideConfiguration)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  override fun createContextForSplit(splitName: String?): Context {
    return activity.createContextForSplit(splitName)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun createDeviceProtectedStorageContext(): Context {
    return activity.createDeviceProtectedStorageContext()
  }

  override fun createDisplayContext(display: Display): Context {
    return activity.createDisplayContext(display)
  }

  override fun createPackageContext(packageName: String?, flags: Int): Context {
    return activity.createPackageContext(packageName, flags)
  }

  override fun databaseList(): Array<String> {
    return activity.databaseList()
  }

  override fun deleteDatabase(name: String?): Boolean {
    return activity.deleteDatabase(name)
  }

  override fun deleteFile(name: String?): Boolean {
    return activity.deleteFile(name)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun deleteSharedPreferences(name: String?): Boolean {
    return activity.deleteSharedPreferences(name)
  }

  override fun enforceCallingOrSelfPermission(permission: String, message: String?) {
    activity.enforceCallingOrSelfPermission(permission, message)
  }

  override fun enforceCallingOrSelfUriPermission(uri: Uri?, modeFlags: Int, message: String?) {
    activity.enforceCallingOrSelfUriPermission(uri, modeFlags, message)
  }

  override fun enforceCallingPermission(permission: String, message: String?) {
    activity.enforceCallingPermission(permission, message)
  }

  override fun enforceCallingUriPermission(uri: Uri?, modeFlags: Int, message: String?) {
    activity.enforceCallingUriPermission(uri, modeFlags, message)
  }

  override fun enforcePermission(permission: String, pid: Int, uid: Int, message: String?) {
    activity.enforcePermission(permission, pid, uid, message)
  }

  override fun enforceUriPermission(
    uri: Uri?,
    pid: Int,
    uid: Int,
    modeFlags: Int,
    message: String?
  ) {
    activity.enforceUriPermission(uri, pid, uid, modeFlags, message)
  }

  override fun enforceUriPermission(
    uri: Uri?,
    readPermission: String?,
    writePermission: String?,
    pid: Int,
    uid: Int,
    modeFlags: Int,
    message: String?
  ) {
    activity.enforceUriPermission(
      uri,
      readPermission,
      writePermission,
      pid,
      uid,
      modeFlags,
      message
    )
  }

  override fun fileList(): Array<String> {
    return activity.fileList()
  }

  override fun getApplicationContext(): Context {
    return activity.applicationContext
  }

  override fun getApplicationInfo(): ApplicationInfo {
    return activity.applicationInfo
  }

  override fun getAssets(): AssetManager {
    return activity.assets
  }

  fun getBaseContext(): Context {
    return activity.baseContext
  }

  override fun getCacheDir(): File {
    return activity.cacheDir
  }

  override fun getClassLoader(): ClassLoader {
    return activity.classLoader
  }

  override fun getCodeCacheDir(): File {
    return activity.codeCacheDir
  }

  override fun getContentResolver(): ContentResolver {
    return activity.contentResolver
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun getDataDir(): File {
    return activity.dataDir
  }

  override fun getDatabasePath(name: String?): File {
    return activity.getDatabasePath(name)
  }

  override fun getDir(name: String?, mode: Int): File {
    return activity.getDir(name, mode)
  }

  override fun getExternalCacheDir(): File? {
    return activity.externalCacheDir
  }

  override fun getExternalCacheDirs(): Array<File> {
    return activity.externalCacheDirs
  }

  override fun getExternalFilesDir(type: String?): File? {
    return activity.getExternalFilesDir(type)
  }

  override fun getExternalFilesDirs(type: String?): Array<File> {
    return activity.getExternalFilesDirs(type)
  }

  override fun getExternalMediaDirs(): Array<File> {
    return activity.externalMediaDirs
  }

  override fun sendStickyOrderedBroadcastAsUser(
    intent: Intent?,
    user: UserHandle?,
    resultReceiver: BroadcastReceiver?,
    scheduler: Handler?,
    initialCode: Int,
    initialData: String?,
    initialExtras: Bundle?
  ) {
    activity.sendStickyOrderedBroadcastAsUser(
      intent,
      user,
      resultReceiver,
      scheduler,
      initialCode,
      initialData,
      initialExtras
    )
  }

  override fun sendStickyOrderedBroadcast(
    intent: Intent?,
    resultReceiver: BroadcastReceiver?,
    scheduler: Handler?,
    initialCode: Int,
    initialData: String?,
    initialExtras: Bundle?
  ) {
    activity.sendStickyOrderedBroadcast(
      intent,
      resultReceiver,
      scheduler,
      initialCode,
      initialData,
      initialExtras
    )
  }

  override fun sendStickyBroadcastAsUser(intent: Intent?, user: UserHandle?) {
    activity.sendStickyBroadcastAsUser(intent, user)
  }

  override fun sendStickyBroadcast(intent: Intent?) {
    activity.sendStickyBroadcast(intent)
  }

  override fun startIntentSender(
    intent: IntentSender?,
    fillInIntent: Intent?,
    flagsMask: Int,
    flagsValues: Int,
    extraFlags: Int,
    options: Bundle?
  ) {
    activity.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags, options)
  }

  override fun startIntentSender(
    intent: IntentSender?,
    fillInIntent: Intent?,
    flagsMask: Int,
    flagsValues: Int,
    extraFlags: Int
  ) {
    activity.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags)
  }

  override fun startInstrumentation(
    className: ComponentName,
    profileFile: String?,
    arguments: Bundle?
  ): Boolean {
    return activity.startInstrumentation(className, profileFile, arguments)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  override fun startForegroundService(service: Intent?): ComponentName? {
    return activity.startForegroundService(service)
  }

  override fun startActivity(intent: Intent, options: Bundle?) {
    activity.startActivity(intent, options)
  }

  override fun startActivity(intent: Intent) {
    activity.startActivity(intent)
  }

  override fun startActivities(intents: Array<out Intent>?, options: Bundle?) {
    activity.startActivities(intents, options)
  }

  override fun startActivities(intents: Array<out Intent>?) {
    activity.startActivities(intents)
  }

  override fun sendOrderedBroadcast(intent: Intent?, receiverPermission: String?) {
    activity.sendOrderedBroadcast(intent, receiverPermission)
  }

  override fun sendBroadcastAsUser(
    intent: Intent?,
    user: UserHandle?,
    receiverPermission: String?
  ) {
    activity.sendBroadcastAsUser(intent, user, receiverPermission)
  }

  override fun sendBroadcastAsUser(intent: Intent?, user: UserHandle?) {
    activity.sendBroadcastAsUser(intent, user)
  }

  override fun sendBroadcast(intent: Intent?, receiverPermission: String?) {
    activity.sendBroadcast(intent, receiverPermission)
  }

  override fun sendBroadcast(intent: Intent?) {
    activity.sendBroadcast(intent)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  override fun revokeUriPermission(targetPackage: String?, uri: Uri?, modeFlags: Int) {
    activity.revokeUriPermission(targetPackage, uri, modeFlags)
  }

  override fun revokeUriPermission(uri: Uri?, modeFlags: Int) {
    activity.revokeUriPermission(uri, modeFlags)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  override fun registerReceiver(
    receiver: BroadcastReceiver?,
    filter: IntentFilter?,
    broadcastPermission: String?,
    scheduler: Handler?,
    flags: Int
  ): Intent? {
    return activity.registerReceiver(receiver, filter, broadcastPermission, scheduler, flags)
  }

  override fun registerReceiver(
    receiver: BroadcastReceiver?,
    filter: IntentFilter?,
    broadcastPermission: String?,
    scheduler: Handler?
  ): Intent? {
    return activity.registerReceiver(receiver, filter, broadcastPermission, scheduler)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  override fun registerReceiver(
    receiver: BroadcastReceiver?,
    filter: IntentFilter?,
    flags: Int
  ): Intent? {
    return activity.registerReceiver(receiver, filter, flags)
  }

  override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
    return activity.registerReceiver(receiver, filter)
  }

  override fun registerComponentCallbacks(callback: ComponentCallbacks?) {
    activity.registerComponentCallbacks(callback)
  }

  override fun openOrCreateDatabase(
    name: String?,
    mode: Int,
    factory: SQLiteDatabase.CursorFactory?,
    errorHandler: DatabaseErrorHandler?
  ): SQLiteDatabase {
    return activity.openOrCreateDatabase(name, mode, factory, errorHandler)
  }

  override fun openOrCreateDatabase(
    name: String?,
    mode: Int,
    factory: SQLiteDatabase.CursorFactory?
  ): SQLiteDatabase {
    return activity.openOrCreateDatabase(name, mode, factory)
  }

  override fun openFileOutput(name: String?, mode: Int): FileOutputStream {
    return activity.openFileOutput(name, mode)
  }

  override fun openFileInput(name: String?): FileInputStream {
    return activity.openFileInput(name)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun moveSharedPreferencesFrom(sourceContext: Context?, name: String?): Boolean {
    return activity.moveSharedPreferencesFrom(sourceContext, name)
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun moveDatabaseFrom(sourceContext: Context?, name: String?): Boolean {
    return activity.moveDatabaseFrom(sourceContext, name)
  }

  override fun isRestricted(): Boolean {
    return activity.isRestricted
  }

  @RequiresApi(Build.VERSION_CODES.N)
  override fun isDeviceProtectedStorage(): Boolean {
    return activity.isDeviceProtectedStorage
  }

  override fun grantUriPermission(toPackage: String?, uri: Uri?, modeFlags: Int) {
    activity.grantUriPermission(toPackage, uri, modeFlags)
  }

  override fun getTheme(): Resources.Theme {
    return activity.theme
  }

  @RequiresApi(Build.VERSION_CODES.M)
  override fun getSystemServiceName(serviceClass: Class<*>): String? {
    return activity.getSystemServiceName(serviceClass)
  }

  override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
    return activity.getSharedPreferences(name, mode)
  }

  override fun getResources(): Resources {
    return activity.resources
  }

  override fun getPackageResourcePath(): String {
    return activity.packageResourcePath
  }

  override fun getPackageName(): String {
    return activity.packageName
  }

  override fun getPackageManager(): PackageManager {
    return activity.packageManager
  }

  override fun getPackageCodePath(): String {
    return activity.packageCodePath
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  override fun getOpPackageName(): String {
    return activity.opPackageName
  }

  override fun getObbDirs(): Array<File> {
    return activity.obbDirs
  }

  override fun getObbDir(): File {
    return activity.obbDir
  }

  override fun getNoBackupFilesDir(): File {
    return activity.noBackupFilesDir
  }

  override fun getMainLooper(): Looper {
    return activity.mainLooper
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun getMainExecutor(): Executor {
    return activity.mainExecutor
  }

  override fun getFilesDir(): File {
    return activity.filesDir
  }

  override fun getFileStreamPath(name: String?): File {
    return activity.getFileStreamPath(name)
  }

  override fun getSystemService(name: String): Any? {
    return activity.getSystemService(name)
  }

  override fun sendOrderedBroadcast(
    intent: Intent,
    receiverPermission: String?,
    resultReceiver: BroadcastReceiver?,
    scheduler: Handler?,
    initialCode: Int,
    initialData: String?,
    initialExtras: Bundle?
  ) {
    activity.sendOrderedBroadcast(
      intent,
      receiverPermission,
      resultReceiver,
      scheduler,
      initialCode,
      initialData,
      initialExtras
    )
  }

  override fun sendOrderedBroadcastAsUser(
    intent: Intent?,
    user: UserHandle?,
    receiverPermission: String?,
    resultReceiver: BroadcastReceiver?,
    scheduler: Handler?,
    initialCode: Int,
    initialData: String?,
    initialExtras: Bundle?
  ) {
    activity.sendOrderedBroadcastAsUser(
      intent,
      user,
      receiverPermission,
      resultReceiver,
      scheduler,
      initialCode,
      initialData,
      initialExtras
    )
  }

  override fun setTheme(resid: Int) {
    activity.setTheme(resid)
  }

  override fun startService(service: Intent?): ComponentName? {
    return activity.startService(service)
  }

  override fun stopService(name: Intent?): Boolean {
    return activity.stopService(name)
  }

  override fun unbindService(conn: ServiceConnection) {
    activity.unbindService(conn)
  }

  override fun unregisterComponentCallbacks(callback: ComponentCallbacks?) {
    activity.unregisterComponentCallbacks(callback)
  }

  override fun unregisterReceiver(receiver: BroadcastReceiver?) {
    activity.unregisterReceiver(receiver)
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  override fun updateServiceGroup(conn: ServiceConnection, group: Int, importance: Int) {
    activity.updateServiceGroup(conn, group, importance)
  }

  override fun equals(other: Any?): Boolean {
    return activity.equals(other)
  }

  override fun hashCode(): Int {
    return activity.hashCode()
  }

  override fun toString(): String {
    return activity.toString()
  }

  override fun clearWallpaper() {
    activity.clearWallpaper()
  }

  override fun getWallpaper(): Drawable {
    return activity.wallpaper
  }

  override fun getWallpaperDesiredMinimumHeight(): Int {
    return activity.wallpaperDesiredMinimumHeight
  }

  override fun getWallpaperDesiredMinimumWidth(): Int {
    return activity.wallpaperDesiredMinimumWidth
  }

  override fun peekWallpaper(): Drawable {
    return activity.peekWallpaper()
  }

  override fun removeStickyBroadcast(intent: Intent?) {
    activity.removeStickyBroadcast(intent)
  }

  override fun removeStickyBroadcastAsUser(intent: Intent?, user: UserHandle?) {
    activity.removeStickyBroadcastAsUser(intent, user)
  }

  override fun setWallpaper(bitmap: Bitmap?) {
    activity.setWallpaper(bitmap)
  }

  override fun setWallpaper(data: InputStream?) {
    activity.setWallpaper(data)
  }
}