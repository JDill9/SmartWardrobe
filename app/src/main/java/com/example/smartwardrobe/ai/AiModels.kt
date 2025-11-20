package com.example.smartwardrobe.ai

data class RenderedModel(
    val id: String,
    val previewImageUrl: String, // URL to a rendered preview (PNG/JPEG)
    val modelUrl: String         // URL to a .glb/.gltf or similar
)

data class AiRenderResponse(
    val models: List<RenderedModel>
)
