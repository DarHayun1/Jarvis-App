package com.darh.jarvisapp

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Fragment.launchRepeatOn(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(state, block)
    }
}

fun String.unescapeSpecialChars(): String {
    return replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\/", "/")
        .replace("\\b", "\b")
        .replace("\\f", "\u000C")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
}

public fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    return mapNotNull { (key, nullableValue) ->
        nullableValue?.let { key to it }
    }.toMap()
}