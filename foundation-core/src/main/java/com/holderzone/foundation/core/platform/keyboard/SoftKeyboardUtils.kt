@file:Suppress("DEPRECATION")

package com.holderzone.foundation.core.platform.keyboard

import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AbsListView
import android.widget.EditText
import android.widget.ScrollView
import java.lang.reflect.Method

object SoftKeyboardUtils {
    fun toggleSoftKeyboard(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    @SuppressLint("ObsoleteSdkInt")
    fun hideNavigationBar(activity: Activity) {
        if (Build.VERSION.SDK_INT in 12..18) {
            activity.window.decorView.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            )
            activity.window.decorView.systemUiVisibility = navigationHideFlags()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun hideNavigationBar(dialog: Dialog) {
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
        )
        dialog.window?.decorView?.systemUiVisibility = navigationHideFlags()
    }

    @SuppressLint("ObsoleteSdkInt")
    fun setHideNavigationBarChangeListener(activity: Activity) {
        activity.window.decorView.setOnSystemUiVisibilityChangeListener {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            )
            activity.window.decorView.systemUiVisibility =
                navigationHideFlags() or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun setHideNavigationBarChangeListener(dialog: Dialog) {
        dialog.window?.decorView?.setOnSystemUiVisibilityChangeListener {
            dialog.window?.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            )
            dialog.window?.decorView?.systemUiVisibility = navigationHideFlags()
        }
    }

    fun showNavigationBar(activity: Activity, systemUiVisibility: Int = 0) {
        activity.window.decorView.systemUiVisibility = systemUiVisibility
    }

    fun hideSoftKeyboardFromFocus(activity: Activity) {
        activity.currentFocus?.let { view ->
            val inputMethodManager =
                activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS,
            )
        }
    }

    fun hideSoftKeyboardFromDecorView(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
    }

    fun hideSoftKeyboard(context: Context, view: View?) {
        if (view == null) return
        val inputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(
            view.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS,
        )
    }

    fun hideSoftKeyboard(context: Context, views: List<View>?) {
        views?.forEach { view -> hideSoftKeyboard(context, view) }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun registerTouchEvent(activity: Activity, content: ViewGroup? = null) {
        val viewGroup = content ?: activity.findViewById<View>(R.id.content) as? ViewGroup ?: return
        registerScrollViewTouchListeners(viewGroup, activity)
        viewGroup.setOnTouchListener { _, motionEvent ->
            dispatchTouchEvent(activity, motionEvent)
            false
        }
    }

    fun dispatchTouchEvent(activity: Activity, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = activity.currentFocus
            if (view != null && shouldHideInput(view, event)) {
                hideSoftInput(activity.applicationContext, view.windowToken)
            }
        }
        return false
    }

    fun dispatchTouchEvent(dialog: Dialog, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = dialog.currentFocus
            if (view != null && shouldHideInput(view, event)) {
                hideSoftInput(dialog.context, view.windowToken)
            }
        }
        return false
    }

    fun isSoftInputShowing(context: Context): Boolean {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        return inputMethodManager?.isActive == true
    }

    fun disableShowSoftInput(editText: EditText) {
        if (Build.VERSION.SDK_INT <= 10) {
            editText.inputType = InputType.TYPE_NULL
            return
        }
        invokeSoftInputFocusMethod(editText, "setShowSoftInputOnFocus")
        invokeSoftInputFocusMethod(editText, "setSoftInputShownOnFocus")
    }

    fun hideSoftKeyboardByFlag(activity: Activity) = toggleSoftKeyboard(activity)

    fun hideNavKey(activity: Activity) = hideNavigationBar(activity)

    fun hideNavKeyDialog(dialog: Dialog) = hideNavigationBar(dialog)

    fun setHideNavKeyChanagListener(activity: Activity) = setHideNavigationBarChangeListener(activity)

    fun setDialogHideNavKeyChanagListener(dialog: Dialog) = setHideNavigationBarChangeListener(dialog)

    fun showNavKey(activity: Activity, systemUiVisibility: Int) =
        showNavigationBar(activity, systemUiVisibility)

    fun hideSoftKeyboardByFocusView(activity: Activity) = hideSoftKeyboardFromFocus(activity)

    fun hideSoftKeyboardByDecorView(activity: Activity) = hideSoftKeyboardFromDecorView(activity)

    fun hideSoftKeyboardBySpecifiedView(context: Context, view: View?) = hideSoftKeyboard(context, view)

    fun hideSoftKeyboardBySpecifiedViews(context: Context, viewList: List<View>?) =
        hideSoftKeyboard(context, viewList)

    @SuppressLint("ClickableViewAccessibility")
    private fun registerScrollViewTouchListeners(viewGroup: ViewGroup, activity: Activity) {
        for (index in 0 until viewGroup.childCount) {
            when (val child = viewGroup.getChildAt(index)) {
                is ScrollView, is AbsListView -> child.setOnTouchListener { _, motionEvent ->
                    dispatchTouchEvent(activity, motionEvent)
                    false
                }

                is ViewGroup -> registerScrollViewTouchListeners(child, activity)
            }
        }
    }

    private fun shouldHideInput(view: View, event: MotionEvent): Boolean {
        if (view is EditText) {
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            if (rect.contains(event.x.toInt(), event.y.toInt())) {
                return false
            }
        }
        return true
    }

    private fun hideSoftInput(context: Context, token: IBinder?) {
        if (token == null) return
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun invokeSoftInputFocusMethod(editText: EditText, methodName: String) {
        runCatching {
            val method: Method =
                EditText::class.java.getMethod(methodName, Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(editText, false)
        }
    }

    private fun navigationHideFlags(): Int {
        return View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LOW_PROFILE
    }
}
