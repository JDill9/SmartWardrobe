package com.example.smartwardrobe.ui.additem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Host composable that wires an Android image picker into AddItemScreen.
 */
@Composable
fun AddItemScreenHost(
    onBackClick: () -> Unit,
    onItemSaved: () -> Unit
) {
    var pendingCallback by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pendingCallback?.invoke(uri)
        pendingCallback = null
    }

    AddItemScreen(
        onBackClick = onBackClick,
        onItemSaved = onItemSaved,
        onPickImage = { onResult ->
            pendingCallback = onResult
            launcher.launch("image/*")
        }
    )
}