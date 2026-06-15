package com.example.questgen.util

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Utility extensions to keep Fragment and View logic clean and DRY.
 */
fun Fragment.showDialog(
    title: String,
    message: String,
    positiveText: String = "OK",
    onPositive: (() -> Unit)? = null
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveText) { dialog, _ ->
            onPositive?.invoke()
            dialog.dismiss()
        }
        .show()
}

fun Fragment.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), message, duration).show()
}

fun Int.formatCoins(): String {
    return "$this GC"
}

/**
 * Collects a Flow safely within the Fragment's view lifecycle.
 * Uses repeatOnLifecycle(Lifecycle.State.STARTED) to automatically pause and resume collection.
 */
fun <T> Fragment.collectLatestFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest { action(it) }
        }
    }
}
