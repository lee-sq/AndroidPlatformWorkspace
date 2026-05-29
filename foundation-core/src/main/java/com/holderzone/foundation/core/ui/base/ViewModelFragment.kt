package com.holderzone.foundation.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class ViewModelFragment<VB : ViewBinding, VM : ViewModel> : BaseFragment<VB>() {
    protected val viewModel: VM by lazy(LazyThreadSafetyMode.NONE) {
        createViewModel()
    }

    protected open val viewModelScope: ViewModelScope = ViewModelScope.Fragment
    protected open val navGraphId: Int? = null
    protected open val navGraphRoute: String? = null
    protected open val viewModelClass: Class<VM>? = null

    final override fun onBindingCreated(binding: VB, savedInstanceState: Bundle?) {
        onViewModelCreated(viewModel)
        onBindingCreated(binding, viewModel, savedInstanceState)
    }

    protected open fun onViewModelCreated(viewModel: VM) = Unit

    protected open fun onBindingCreated(
        binding: VB,
        viewModel: VM,
        savedInstanceState: Bundle?,
    ) = Unit

    private fun createViewModel(): VM {
        val owner = resolveViewModelStoreOwner()
        val factory = (owner as? HasDefaultViewModelProviderFactory)
            ?.defaultViewModelProviderFactory
            ?: defaultViewModelProviderFactory
        return ViewModelProvider(owner, factory)[viewModelClass ?: resolveViewModelClass()]
    }

    private fun resolveViewModelStoreOwner(): ViewModelStoreOwner {
        return when (viewModelScope) {
            ViewModelScope.Fragment -> this
            ViewModelScope.Activity -> requireActivity()
            ViewModelScope.NavGraph -> {
                val route = navGraphRoute
                val id = navGraphId
                when {
                    !route.isNullOrBlank() -> findNavController().getBackStackEntry(route)
                    id != null -> findNavController().getBackStackEntry(id)
                    else -> error("NavGraph scoped ViewModel requires navGraphRoute or navGraphId.")
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveViewModelClass(): Class<VM> {
        var type: Type? = javaClass.genericSuperclass
        while (type != null) {
            if (type is ParameterizedType) {
                val rawType = type.rawType
                if (rawType == ViewModelFragment::class.java) {
                    return type.actualTypeArguments[1].toClass() as Class<VM>
                }
                type = (rawType as? Class<*>)?.genericSuperclass
            } else {
                type = (type as? Class<*>)?.genericSuperclass
            }
        }
        error("Cannot resolve ViewModel type for ${javaClass.name}.")
    }

    private fun Type.toClass(): Class<*> {
        return when (this) {
            is Class<*> -> this
            is ParameterizedType -> rawType as Class<*>
            else -> error("Unsupported ViewModel type: $this")
        }
    }

    enum class ViewModelScope {
        Fragment,
        Activity,
        NavGraph,
    }

    abstract override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB
}
