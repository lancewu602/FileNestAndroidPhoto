package com.filenest.photo.data.api

data class Ret<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
)

fun isRetOk(ret: Ret<*>?): Boolean {
    return ret?.let { it.code == 200 } ?: false
}

fun retMsg(ret: Ret<Any?>?): String {
    return ret?.message ?: "未知异常"
}