package com.example.smartwardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smartwardrobe.ai.ModelViewerDialog
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.ui.saved.OutfitWithItems
import com.example.smartwardrobe.ui.saved.SavedOutfitsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Saved(
    viewModel: SavedOutfitsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Show 3D model viewer dialog
    state.viewing3DModelUrl?.let { modelUrl ->
        ModelViewerDialog(
            modelUrl = modelUrl,
            onDismiss = { viewModel.close3DViewer() }
        )
    }

    // Dialog state for delete confirmation
    var outfitToDelete by remember { mutableStateOf<String?>(null) }

    // Delete confirmation dialog
    outfitToDelete?.let { outfitId ->
        AlertDialog(
            onDismissRequest = { outfitToDelete = null },
            title = { Text("Delete Outfit") },
            text = { Text("Are you sure you want to delete this outfit?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOutfit(outfitId)
                        outfitToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { outfitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Outfits") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.outfits.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No saved outfits",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Build an outfit to see it here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.outfits) { outfitWithItems ->
                        OutfitCard(
                            outfitWithItems = outfitWithItems,
                            onFavoriteClick = { viewModel.toggleFavorite(outfitWithItems.outfit.id) },
                            onDeleteClick = { outfitToDelete = outfitWithItems.outfit.id },
                            onView3DClick = { modelUrl -> viewModel.view3DModel(modelUrl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitCard(
    outfitWithItems: OutfitWithItems,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onView3DClick: (String) -> Unit
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.items

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with name and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outfit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (outfit.occasion.isNotEmpty()) {
                        Text(
                            text = outfit.occasion.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show 3D viewer button if model exists
                    outfit.ai3DModelUrl?.let { modelUrl ->
                        TextButton(
                            onClick = { onView3DClick(modelUrl) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View 3D", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (outfit.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (outfit.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items preview
            if (items.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        OutfitItemPreview(item = item)
                    }
                }
            } else {
                Text(
                    text = "No items found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Season tag
            outfit.season?.let { season ->
                Spacer(modifier = Modifier.height(12.dp))
                AssistChip(
                    onClick = { },
                    label = {
                        Text(season.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
private fun OutfitItemPreview(item: WardrobeItem) {
    Card(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = java.io.File(item.imageUrl),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Category label
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(2.dp)
            ) {
                Text(
                    text = item.category.name.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}