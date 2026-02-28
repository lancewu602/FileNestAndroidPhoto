package com.filenest.photo.data.api

/**
 * 后端接口返回的结构，其中 data 是数据
 */
data class Ret<T>(
    val code: Int,
    val message: String? = null,
    val data: T? = null,
)

fun isRetOk(ret: Ret<*>?): Boolean {
    return ret?.let { it.code == 200 } ?: false
}

fun retMsg(ret: Ret<*>?): String {
    return ret?.message ?: "未知异常"
}