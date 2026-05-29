package com.holderzone.foundation.core.network

interface ServiceFactory {
    fun <T> create(serviceClass: Class<T>): T
    fun <T> create(serviceClass: Class<T>, baseUrl: String): T
}

inline fun <reified T> ServiceFactory.create(): T = create(T::class.java)

inline fun <reified T> ServiceFactory.create(baseUrl: String): T = create(T::class.java, baseUrl)
