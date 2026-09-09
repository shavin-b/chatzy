package com.orderflow.autoresponder.core.logger

import android.util.Log

object StructuredLogger {
    private const val PREFIX = "[OrderFlow]"

    fun d(tag: String, message: String) {
        Log.d("$PREFIX:$tag", message)
    }

    fun i(tag: String, message: String) {
        Log.i("$PREFIX:$tag", message)
        Log.w("$PREFIX:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$PREFIX:$tag", message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$PREFIX:$tag", message, throwable)
    }
}
