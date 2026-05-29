package com.holderzone.foundation.core.ui.feedback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.holderzone.foundation.core.ui.feedback.toast.StyledToast

class ToastLoadingHost(
    private val context: Context,
    private val loadingOverlay: View? = null,
    private val loadingTipsView: TextView? = null,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentToast: Toast? = null
    private var clearToastRunnable: Runnable? = null

    fun handle(event: ToastEvent) {
        when (event) {
            is ToastEvent.ShowToast -> showToast(event)
            is ToastEvent.ShowLoading -> showLoading(event.tips)
            ToastEvent.DismissLoading -> dismissLoading()
        }
    }

    fun clear() {
        clearToastRunnable?.let(mainHandler::removeCallbacks)
        clearToastRunnable = null
        currentToast?.cancel()
        currentToast = null
        StyledToast.clearLastToast()
        dismissLoading()
    }

    private fun showToast(event: ToastEvent.ShowToast) {
        dismissLoading()
        clearToastRunnable?.let(mainHandler::removeCallbacks)
        currentToast?.cancel()
        val toast = event.toToast()
        currentToast = toast
        toast.show()
        scheduleToastRelease(toast, event.duration)
    }

    private fun showLoading(tips: String?) {
        loadingTipsView?.text = tips.orEmpty()
        loadingTipsView?.isVisible = !tips.isNullOrBlank()
        loadingOverlay?.visibility = View.VISIBLE
    }

    private fun dismissLoading() {
        loadingOverlay?.visibility = View.GONE
    }

    private fun scheduleToastRelease(toast: Toast, duration: Duration) {
        val delayMillis = when (duration) {
            Duration.SHORT -> 2_500L
            Duration.LONG -> 4_000L
        }
        val runnable = Runnable {
            if (currentToast === toast) {
                currentToast = null
            }
            StyledToast.clearLastToast(toast)
            clearToastRunnable = null
        }
        clearToastRunnable = runnable
        mainHandler.postDelayed(runnable, delayMillis)
    }

    private fun Duration.toToastLength(): Int {
        return when (this) {
            Duration.SHORT -> Toast.LENGTH_SHORT
            Duration.LONG -> Toast.LENGTH_LONG
        }
    }

    private fun ToastEvent.ShowToast.toToast(): Toast {
        val length = duration.toToastLength()
        return when (type) {
            ToastType.SUCCESS -> StyledToast.success(context, message, length)
            ToastType.ERROR -> StyledToast.error(context, message, length)
            ToastType.INFO -> StyledToast.info(context, message, length)
        }
    }
}
