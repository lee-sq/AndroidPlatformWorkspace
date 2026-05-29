package com.holderzone.foundation.core.ui.base

import android.app.Activity
import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object ImmersiveMode {
    fun applyToActivityAndWindow(
        activity: Activity?,
        activeWindow: Window?,
    ) {
        val activityWindow = activity?.window ?: return
        val hideStatusBars = shouldHideStatusBars(activityWindow)
        applyToWindow(activityWindow, hideStatusBars)
        activeWindow?.let { window ->
            inheritActivitySystemUiVisibility(activity, window)
            applyToWindow(window, hideStatusBars)
        }
    }

    fun applyToActivityAndView(
        activity: Activity?,
        activeView: View?,
    ) {
        val activityWindow = activity?.window ?: return
        val hideStatusBars = shouldHideStatusBars(activityWindow)
        applyToWindow(activityWindow, hideStatusBars)
        activeView?.let { view ->
            inheritActivitySystemUiVisibility(activity, view)
            applyToView(view, hideStatusBars)
        }
    }

    @Suppress("DEPRECATION")
    fun inheritActivitySystemUiVisibility(
        activity: Activity?,
        window: Window,
    ) {
        inheritActivitySystemUiVisibility(activity, window.decorView)
    }

    @Suppress("DEPRECATION")
    fun inheritActivitySystemUiVisibility(
        activity: Activity?,
        view: View,
    ) {
        val activityFlags = activity?.window?.decorView?.systemUiVisibility ?: 0
        view.systemUiVisibility = activityFlags or IMMERSIVE_NAVIGATION_FLAGS
    }

    @Suppress("DEPRECATION")
    private fun applyToWindow(
        window: Window,
        hideStatusBars: Boolean,
    ) {
        applyLegacyFlags(window.decorView, hideStatusBars)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
            if (hideStatusBars) {
                hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyToView(
        view: View,
        hideStatusBars: Boolean,
    ) {
        applyLegacyFlags(view, hideStatusBars)
        ViewCompat.getWindowInsetsController(view)?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
            if (hideStatusBars) {
                hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun applyLegacyFlags(
        view: View,
        hideStatusBars: Boolean,
    ) {
        val statusFlags = if (hideStatusBars) {
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            0
        }
        view.systemUiVisibility = view.systemUiVisibility or IMMERSIVE_NAVIGATION_FLAGS or statusFlags
    }

    @Suppress("DEPRECATION")
    private fun shouldHideStatusBars(activityWindow: Window): Boolean {
        val activityFlags = activityWindow.decorView.systemUiVisibility
        val statusBarsHiddenByFlag = activityFlags and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
        val statusBarsHiddenByInsets = ViewCompat
            .getRootWindowInsets(activityWindow.decorView)
            ?.isVisible(WindowInsetsCompat.Type.statusBars()) == false
        return statusBarsHiddenByFlag || statusBarsHiddenByInsets
    }

    @Suppress("DEPRECATION")
    private const val IMMERSIVE_NAVIGATION_FLAGS =
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
}
