package com.example.smartwardrobe.ui.additem

import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.smartwardrobe.data.model.ClothingCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    onBackClick: () -> Unit,
    onItemSaved: () -> Unit,
    onPickImage: (onResult: (Uri?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AddItemViewModel = remember { AddItemViewModel(context) }

    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Navigate back when item is saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onItemSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Item") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image Picker
            Text(
                text = "Photo",
                style = MaterialTheme.typography.titleMedium
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clickable {
                        onPickImage { uri ->
                            uri?.let { viewModel.onImageSelected(it) }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state.selectedImageUri != null) {
                    AsyncImage(
                        model = state.selectedImageUri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to select photo",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Name field
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name *") },
                placeholder = { Text("e.g., Blue Jeans") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category dropdown
            CategoryDropdown(
                selectedCategory = state.category,
                onCategorySelected = viewModel::onCategoryChange
            )

            // Color field
            OutlinedTextField(
                value = state.color,
                onValueChange = viewModel::onColorChange,
                label = { Text("Color") },
                placeholder = { Text("e.g., Navy Blue") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Brand field
            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::onBrandChange,
                label = { Text("Brand") },
                placeholder = { Text("e.g., Levi's") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Size field
            OutlinedTextField(
                value = state.size,
                onValueChange = viewModel::onSizeChange,
                label = { Text("Size") },
                placeholder = { Text("e.g., M, L, 32") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Tags field
            OutlinedTextField(
                value = state.tags,
                onValueChange = viewModel::onTagsChange,
                label = { Text("Tags") },
                placeholder = { Text("e.g., casual, summer, favorite") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Separate tags with commas") }
            )

            // Error message
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Save button
            Button(
                onClick = { viewModel.saveItem() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Item")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: ClothingCategory,
    onCategorySelected: (ClothingCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ClothingCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(category.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}