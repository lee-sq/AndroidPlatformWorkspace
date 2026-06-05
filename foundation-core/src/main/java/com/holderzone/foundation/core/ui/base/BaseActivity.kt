package com.holderzone.foundation.core.ui.base

import android.Manifest
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.holderzone.foundation.core.R
import com.holderzone.foundation.core.platform.keyboard.SoftKeyboardUtils
import com.holderzone.foundation.core.platform.logging.AppLogger
import com.holderzone.foundation.core.ui.feedback.FoundationPermissionDialog
import com.holderzone.foundation.core.ui.feedback.toast.StyledToast
import com.permissionx.guolindev.PermissionX

abstract class BaseActivity : AppCompatActivity() {
    protected open val forceDefaultFontScale: Boolean = false
    protected open val hideNavigationBarOnCreate: Boolean = true
    protected open val hideSystemBarsOnCreate: Boolean = false
    protected open val keepSystemBarsHidden: Boolean = true

    protected open val log = AppLogger

    open override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hideNavigationBarOnCreate) {
            SoftKeyboardUtils.setHideNavigationBarChangeListener(this)
            SoftKeyboardUtils.hideNavigationBar(this)
        }
        if (hideSystemBarsOnCreate) {
            hideSystemBars()
        }
    }

    open override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && hideSystemBarsOnCreate && keepSystemBarsHidden) {
            hideSystemBars()
        }
    }

    open override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (forceDefaultFontScale && newConfig.fontScale != DEFAULT_FONT_SCALE) {
            applyDefaultFontScale(super.getResources())
        }
    }

    open override fun getResources(): Resources {
        val resources = super.getResources()
        if (forceDefaultFontScale && resources.configuration.fontScale != DEFAULT_FONT_SCALE) {
            applyDefaultFontScale(resources)
        }
        return resources
    }

    protected fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    protected fun showSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    open fun hideNav() {
        hideSystemBars()
    }

    open fun getPermission(
        permissions: List<String> = defaultPermissions(),
    ) {
        PermissionX.init(this)
            .permissions(*permissions.toTypedArray())
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    createPermissionDialog(
                        deniedList = deniedList,
                        message = resources.getString(R.string.foundation_permission_agreement_tips),
                        positiveText = resources.getString(R.string.foundation_permission_agree),
                        negativeText = resources.getString(R.string.foundation_permission_cancel),
                    ),
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    createPermissionDialog(
                        deniedList = deniedList,
                        message = resources.getString(R.string.foundation_permission_notify_go_system_setting),
                        positiveText = resources.getString(R.string.foundation_permission_go_setting),
                        negativeText = resources.getString(R.string.foundation_permission_cancel),
                    ),
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    onPermissionGet()
                } else {
                    StyledToast.error(
                        this,
                        resources.getString(
                            R.string.foundation_permission_denied,
                            deniedList.joinToString(),
                        ),
                        StyledToast.LENGTH_SHORT,
                    ).show()
                }
            }
    }

    open fun onPermissionGet() = Unit

    private fun createPermissionDialog(
        deniedList: List<String>,
        message: String,
        positiveText: String,
        negativeText: String?,
    ): FoundationPermissionDialog {
        return FoundationPermissionDialog(
            context = this,
            permissions = deniedList,
            title = resources.getString(R.string.foundation_permission_title),
            message = message,
            positiveText = positiveText,
            negativeText = negativeText,
        )
    }

    private fun applyDefaultFontScale(resources: Resources) {
        val configuration = Configuration(resources.configuration).apply {
            fontScale = DEFAULT_FONT_SCALE
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private companion object {
        private const val DEFAULT_FONT_SCALE = 1f

        private fun defaultPermissions(): List<String> {
            return buildList {
                add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= 33) {
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                add(Manifest.permission.INTERNET)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
}
