package com.example.smartwardrobe.ui.outfit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.model.Season
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.ai.ModelViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitBuilderScreen(
    onOutfitSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: OutfitBuilderViewModel = viewModel {
        OutfitBuilderViewModel(contentResolver = context.contentResolver)
    }
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Show 3D model viewer dialog
    state.generated3DModelUrl?.let { modelUrl ->
        ModelViewerDialog(
            modelUrl = modelUrl,
            onDismiss = { viewModel.clear3DModel() }
        )
    }

    // Navigate when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onOutfitSaved()
            viewModel.resetBuilder()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build Outfit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (state.isLoading && state.wardrobeItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                // Selected Items Preview
                SelectedItemsPreview(
                    selectedItems = viewModel.getSelectedItemsList(),
                    modifier = Modifier.padding(16.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Category Sections
                ClothingCategory.entries.forEach { category ->
                    val items = state.wardrobeItems[category] ?: emptyList()
                    if (items.isNotEmpty()) {
                        CategorySection(
                            category = category,
                            items = items,
                            selectedItem = state.selectedItems[category],
                            onItemSelected = { item ->
                                if (state.selectedItems[category] == item) {
                                    viewModel.deselectItem(category)
                                } else {
                                    viewModel.selectItem(category, item)
                                }
                            }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Outfit Details
                OutfitDetailsSection(
                    outfitName = state.outfitName,
                    onNameChange = viewModel::setOutfitName,
                    selectedOccasions = state.selectedOccasions,
                    onOccasionToggle = viewModel::toggleOccasion,
                    selectedSeason = state.selectedSeason,
                    onSeasonChange = viewModel::setSeason
                )

                // Error messages
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                if (state.generationError != null) {
                    Text(
                        text = state.generationError!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Generate 3D Button
                OutlinedButton(
                    onClick = { viewModel.generate3DPreview() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !state.isGenerating3D && !state.isLoading && viewModel.getSelectedItemsList().isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isGenerating3D) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating 3D...")
                    } else {
                        Text("Generate 3D Preview")
                    }
                }

                // Save Button
                Button(
                    onClick = { viewModel.saveOutfit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    enabled = !state.isLoading && !state.isGenerating3D && viewModel.getSelectedItemsList().isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isLoading) "Saving..." else "Save Outfit")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SelectedItemsPreview(
    selectedItems: List<WardrobeItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Outfit Preview",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select items below to build your outfit",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedItems) { item ->
                    PreviewItemCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun PreviewItemCard(item: WardrobeItem) {
    Card(
        modifier = Modifier
            .size(100.dp)
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
                        style = MaterialTheme.typography.titleLarge,
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
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(4.dp)
            ) {
                Text(
                    text = item.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: ClothingCategory,
    items: List<WardrobeItem>,
    selectedItem: WardrobeItem?,
    onItemSelected: (WardrobeItem) -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                SelectableItemCard(
                    item = item,
                    isSelected = selectedItem?.id == item.id,
                    onClick = { onItemSelected(item) }
                )
            }
        }
    }
}

@Composable
private fun SelectableItemCard(
    item: WardrobeItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() },
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OutfitDetailsSection(
    outfitName: String,
    onNameChange: (String) -> Unit,
    selectedOccasions: List<String>,
    onOccasionToggle: (String) -> Unit,
    selectedSeason: Season?,
    onSeasonChange: (Season?) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Outfit Name
        OutlinedTextField(
            value = outfitName,
            onValueChange = onNameChange,
            label = { Text("Outfit Name") },
            placeholder = { Text("e.g., Casual Friday") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Occasions
        Text(
            text = "Occasion",
            style = MaterialTheme.typography.titleSmall
        )

        val occasions = listOf("Casual", "Formal", "Work", "Party", "Sport", "Date")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(occasions.size) { index ->
                val occasion = occasions[index]
                FilterChip(
                    selected = selectedOccasions.contains(occasion),
                    onClick = { onOccasionToggle(occasion) },
                    label = { Text(occasion) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Season
        Text(
            text = "Season",
            style = MaterialTheme.typography.titleSmall
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Season.entries.size) { index ->
                val season = Season.entries[index]
                FilterChip(
                    selected = selectedSeason == season,
                    onClick = {
                        onSeasonChange(if (selectedSeason == season) null else season)
                    },
                    label = {
                        Text(season.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
