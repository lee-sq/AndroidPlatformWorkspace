package com.holderzone.foundation.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.holderzone.foundation.core.navigation.AppNavigator
import com.holderzone.foundation.core.platform.logging.AppLogger
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    protected open val log = AppLogger

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) {
            "ViewBinding is only valid between onCreateView and onDestroyView."
        }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBindingCreated(binding, savedInstanceState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected open fun onBindingCreated(binding: VB, savedInstanceState: Bundle?) = Unit

    protected fun navigateTo(route: String, navOptions: NavOptions? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            AppNavigator.navigateTo(route, navOptions)
        }
    }

    protected fun navigateBack() {
        viewLifecycleOwner.lifecycleScope.launch {
            AppNavigator.navigateBack()
        }
    }

    protected fun navigateBack(result: Map<String, Any>) {
        viewLifecycleOwner.lifecycleScope.launch {
            AppNavigator.navigateBack(result)
        }
    }

    protected fun navigateBackTo(
        route: String,
        inclusive: Boolean = false,
        result: Map<String, Any> = emptyMap(),
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            AppNavigator.navigateBackTo(route, inclusive, result)
        }
    }

    protected fun <T> observeNavigationResult(
        key: String,
        clearAfterHandle: Boolean = true,
        onResult: (T) -> Unit,
    ) {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<T>(key)
            ?.observe(viewLifecycleOwner) { value ->
                onResult(value)
                if (clearAfterHandle) {
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<T>(key)
                }
            }
    }
}
