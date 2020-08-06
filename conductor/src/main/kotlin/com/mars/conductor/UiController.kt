@file:Suppress("SpellCheckingInspection")

package com.mars.conductor

import android.content.Context
import com.mars.tools.ktx.ContextProvider

/*
 * author: 凛
 * date: 2020/8/6 11:21 PM
 * github: https://github.com/oh-eRin
 * description: support mars-library vlayer
 */
abstract class UiController : ComponentController(), ContextProvider {
  override fun provideContext(): Context = activity
}