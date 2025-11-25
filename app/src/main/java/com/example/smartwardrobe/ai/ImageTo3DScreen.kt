package com.example.smartwardrobe.ai

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.smartwardrobe.data.repository.ModelCacheRepository

/**
 * Main UI for the Image → 3D flow.
 *
 * @param repository        AiRepository implementation (TripoAiRepository or MockAiRepository)
 * @param cacheRepository   Local model cache (not used directly by UI)
 * @param onPickImage       Callback that launches the image picker and returns a Uri (or null)
 */
@Composable
fun ImageTo3DScreen(
    repository: AiRepository,
    cacheRepository: ModelCacheRepository,
    onPickImage: (onResult: (Uri?) -> Unit) -> Unit
) {
    // Simple manual ViewModel instance wired to given repository + cache
    val viewModel = remember {
        ImageTo3DViewModel(
            repository = repository,
            cacheRepository = cacheRepository
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    // Controls if the 3D viewer dialog is visible
    var showViewerDialog by remember { mutableStateOf(false) }

    // Grab the first model (if present) from the AI response
    val model = uiState.result?.models?.firstOrNull()
    val remoteModelUrl = model?.modelUrl.orEmpty()

    LaunchedEffect(remoteModelUrl) {
        if (remoteModelUrl.isNotBlank()) {
            Log.d("ImageTo3DScreen", "Remote model URL for viewer: $remoteModelUrl")
        }
        if (uiState.cachedModelPath != null) {
            Log.d(
                "ImageTo3DScreen",
                "Model also cached at: ${uiState.cachedModelPath} (viewer still uses REMOTE URL)"
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Image → 3D Model (Tripo AI)",
                style = MaterialTheme.typography.headlineSmall
            )

            // Selected image preview
            uiState.selectedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Selected clothing image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Pick image button
            OutlinedButton(
                onClick = {
                    onPickImage { uri ->
                        uri?.let {
                            viewModel.onImageSelected(it)
                        }
                    }
                }
            ) {
                Text("Pick Image")
            }

            // Generate 3D button
            Button(
                onClick = { viewModel.generate3D() },
                enabled = uiState.selectedImageUri != null && !uiState.isProcessing
            ) {
                Text("Generate 3D")
            }

            // Loading indicator
            if (uiState.isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Text("Generating 3D model…")
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // If we got a model back, show its ID and a "View 3D Model" button
            model?.let { m ->
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Model ID: ${m.id}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // IMPORTANT: view uses REMOTE HTTPS URL, not cached file path
                if (remoteModelUrl.isNotBlank()) {
                    Button(
                        onClick = { showViewerDialog = true }
                    ) {
                        Text("View 3D Model")
                    }
                }
            }
        }

        // 3D Model Viewer Dialog
        if (showViewerDialog && remoteModelUrl.isNotBlank()) {
            ModelViewerDialog(
                modelUrl = remoteModelUrl,  // <-- REMOTE HTTPS URL HERE
                modelId = model?.id,
                onDismiss = { showViewerDialog = false }
            )
        }
    }
}
