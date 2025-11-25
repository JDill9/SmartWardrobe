package com.example.smartwardrobe.ai

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.smartwardrobe.data.repository.ModelCacheRepository

// Explicit imports (same package, but this guarantees resolution)
import com.example.smartwardrobe.ai.MockAiRepository
import com.example.smartwardrobe.ai.TripoAiRepository

enum class AiModel {
    MOCK,           // Mock data for testing
    TRIPOAI         // Tripo AI Official API (FAST <1sec, FREE) ⭐ USE THIS
}

/**
 * Host composable that wires an Android image picker into ImageTo3DScreen.
 *
 * @param aiModel Which AI model to use (default: TRIPOAI - FAST & FREE)
 */
@Composable
fun ImageTo3DScreenHost(aiModel: AiModel = AiModel.TRIPOAI) {
    val context = LocalContext.current
    var pendingCallback by remember { mutableStateOf<((Uri?) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // When the user picks an image (or cancels), call the last callback
        pendingCallback?.invoke(uri)
        pendingCallback = null
    }

    // Create the appropriate repository based on aiModel selection
    val repository = remember(aiModel) {
        when (aiModel) {
            AiModel.MOCK -> MockAiRepository()
            AiModel.TRIPOAI -> TripoAiRepository(
                contentResolver = context.contentResolver,
                // TODO: consider moving this to a secure config later
                tripoApiKey = "tsk_97_kK-zVy2bQzZBIkbeSuwsf3XD3Za6Sk3g8tPJZBd0"
            )
        }
    }

    // Create model cache repository
    val cacheRepository = remember {
        ModelCacheRepository(context)
    }

    ImageTo3DScreen(
        repository = repository,
        cacheRepository = cacheRepository,
        onPickImage = { onResult ->
            // Store the callback so we can use it when the picker returns
            pendingCallback = onResult
            launcher.launch("image/*")
        }
    )
}
