package com.example.smartwardrobe.ai

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A full-screen 3D model viewer using WebView with Google's model-viewer component.
 *
 * Supports .glb and .gltf files with:
 * - Rotate (drag)
 * - Zoom (pinch)
 * - Orbit controls
 *
 * @param modelUrl URL to the .glb or .gltf file
 * @param modelId Optional model ID for display
 * @param onBackClick Callback when back button is pressed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelViewerScreen(
    modelUrl: String,
    modelId: String? = null,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = modelId?.let { "Model: $it" } ?: "3D Viewer",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ModelViewerWebView(
                modelUrl = modelUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * WebView component that renders a 3D model using Google's model-viewer.
 *
 * model-viewer is a web component that provides:
 * - Auto-rotation
 * - Camera controls (orbit, zoom, pan)
 * - AR support (optional)
 * - Responsive sizing
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ModelViewerWebView(
    modelUrl: String,
    modifier: Modifier = Modifier
) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>3D Model Viewer</title>
            <script type="module" src="https://unpkg.com/@google/model-viewer/dist/model-viewer.min.js"></script>
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #f5f5f5;
                    overflow: hidden;
                }
                model-viewer {
                    width: 100%;
                    height: 100%;
                    background-color: #f5f5f5;
                    --poster-color: transparent;
                }
                .loading {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    font-family: sans-serif;
                    color: #666;
                }
            </style>
        </head>
        <body>
            <model-viewer
                src="$modelUrl"
                alt="3D clothing model"
                camera-controls
                auto-rotate
                shadow-intensity="1"
                exposure="0.8"
                environment-image="neutral"
                loading="eager"
            >
                <div class="loading" slot="poster">Loading 3D model...</div>
            </model-viewer>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                webViewClient = WebViewClient()

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // Load the HTML with model-viewer
                loadDataWithBaseURL(
                    "https://localhost/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            // Reload if URL changes
            webView.loadDataWithBaseURL(
                "https://localhost/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

/**
 * Dialog version of the 3D model viewer.
 * Use this for a quick preview without navigation.
 */
@Composable
fun ModelViewerDialog(
    modelUrl: String,
    modelId: String? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f),
        title = {
            Text(
                text = modelId?.let { "Model: $it" } ?: "3D Viewer",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                ModelViewerWebView(
                    modelUrl = modelUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}