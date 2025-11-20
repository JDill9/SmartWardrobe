package com.example.smartwardrobe.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Host composable that wires an Android image picker into ImageTo3DScreen.
 */
@Composable
fun ImageTo3DScreenHost() {
    var pendingCallback by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // When the user picks an image (or cancels), call the last callback
        pendingCallback?.invoke(uri)
        pendingCallback = null
    }

    ImageTo3DScreen { onResult ->
        // Store the callback so we can use it when the picker returns
        pendingCallback = onResult
        launcher.launch("image/*")
    }
}
