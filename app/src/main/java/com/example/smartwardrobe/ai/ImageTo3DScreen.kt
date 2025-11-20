package com.example.smartwardrobe.ai

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * Screen for the flow:
 * 1) User selects a clothing image
 * 2) Image is sent to AI (mock or real)
 * 3) Preview 3D models are displayed
 *
 * @param onPickImage callback provided by the host (Activity/Nav graph)
 *        to launch an image picker. It gets a lambda that should be
 *        invoked with the resulting Uri (or null if cancelled).
 */
@Composable
fun ImageTo3DScreen(
    onPickImage: (onResult: (Uri?) -> Unit) -> Unit
) {
    // For now, use the mock AI repo so the feature works without a backend.
    val viewModel: ImageTo3DViewModel = viewModel(
        factory = ImageTo3DViewModelFactory(MockAiRepository())
    )

    val state by viewModel.uiState.collectAsState()

    // Optional place to hook Snackbar/Toast later if you want
    LaunchedEffect(state.errorMessage) {
        // e.g., show snackbar
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Image → 3D Preview",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1) Image picker area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable {
                    // Host (Activity/Nav) will actually launch the picker
                    onPickImage { uri ->
                        uri?.let { viewModel.onImageSelected(it) }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (state.selectedImageUri != null) {
                AsyncImage(
                    model = state.selectedImageUri,
                    contentDescription = "Selected clothing image",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Tap to select a clothing image")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2) Generate button
        Button(
            onClick = { viewModel.generate3D() },
            enabled = state.selectedImageUri != null && !state.isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isProcessing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating 3D Preview…")
                }
            } else {
                Text("Generate 3D Preview")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3) Result models
        state.result?.models?.let { models ->
            if (models.isNotEmpty()) {
                Text(
                    text = "Preview Models",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                models.forEach { model ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Model ID: ${model.id}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            AsyncImage(
                                model = model.previewImageUrl,
                                contentDescription = "3D Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Model URL: ${model.modelUrl}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
