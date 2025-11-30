package com.example.smartwardrobe.ai

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream
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
 * @param modelUrl URL to the .glb or .gltf file (MUST be a valid HTTP/HTTPS URL)
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
    // Treat modelUrl as the final URL we want model-viewer to load.
    val modelSrc = modelUrl

    Log.d("ModelViewerWebView", "Loading model from: $modelSrc")

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>3D Model Viewer</title>
            <script type="module" src="https://unpkg.com/@google/model-viewer@4.1.0/dist/model-viewer.min.js"></script>
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
                .error {
                    position: absolute;
                    top: 50%;
                    left: 50%;
                    transform: translate(-50%, -50%);
                    font-family: sans-serif;
                    color: #f44336;
                    text-align: center;
                    padding: 20px;
                }
            </style>
        </head>
        <body>
            <model-viewer
                id="viewer"
                src="$modelSrc"
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
            <script>
                const viewer = document.getElementById('viewer');

                viewer.addEventListener('load', () => {
                    console.log('Model loaded successfully');
                });

                viewer.addEventListener('error', (event) => {
                    console.error('Model loading error:', event);
                    console.error('Model URL:', '$modelSrc');
                    document.body.innerHTML = '<div class="error">Failed to load 3D model<br><small>Check console for details</small></div>';
                });

                viewer.addEventListener('progress', (event) => {
                    console.log('Loading progress:', event.detail.totalProgress);
                });

                console.log('Model viewer initialized with URL:', '$modelSrc');
            </script>
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

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return null

                        // Intercept .glb file requests (handles URLs with query params)
                        if (url.contains(".glb")) {
                            Log.d("ModelViewer-Intercept", "Intercepting GLB request: $url")

                            // Extract model ID from URL (UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
                            val modelIdMatch = Regex("([a-f0-9-]{36})").find(url)
                            val modelId = modelIdMatch?.value

                            if (modelId != null) {
                                // Check if model exists in cache
                                val cacheDir = File(context.cacheDir, "3d_models")
                                val cachedFile = File(cacheDir, "$modelId.glb")

                                if (cachedFile.exists()) {
                                    Log.d("ModelViewer-Intercept", "✓ Serving from cache: ${cachedFile.absolutePath}")
                                    try {
                                        val inputStream = FileInputStream(cachedFile)

                                        // IMPORTANT: Add CORS headers so fetch() API accepts the response
                                        val headers = mutableMapOf<String, String>()
                                        headers["Access-Control-Allow-Origin"] = "*"
                                        headers["Access-Control-Allow-Methods"] = "GET, OPTIONS"
                                        headers["Access-Control-Allow-Headers"] = "Content-Type"

                                        return WebResourceResponse(
                                            "model/gltf-binary",
                                            "binary",
                                            200,                    // HTTP 200 OK
                                            "OK",                   // Status message
                                            headers,                // CORS headers
                                            inputStream
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ModelViewer-Intercept", "✗ Error reading cached file", e)
                                    }
                                } else {
                                    Log.d("ModelViewer-Intercept", "Cache miss: ${cachedFile.absolutePath}")
                                }
                            } else {
                                Log.w("ModelViewer-Intercept", "Could not extract model ID from URL: $url")
                            }
                        }

                        // Fall back to default behavior (fetch from network)
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                // Enable console logging
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Log.d(
                            "ModelViewer-JS",
                            "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"
                        )
                        return true
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // Use a dummy HTTPS base URL (not null) so everything is treated as web content.
                loadDataWithBaseURL(
                    "https://example.com",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            // Reload if HTML changes (e.g., different modelSrc)
            webView.loadDataWithBaseURL(
                "https://example.com",
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
    onDismiss: () -> Unit,
    onSave: (() -> Unit)? = null  // Optional save callback for Build tab
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show Save button only when onSave callback is provided (Build tab)
                onSave?.let { saveCallback ->
                    Button(onClick = {
                        saveCallback()
                        onDismiss()
                    }) {
                        Text("Save 3D Model")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
