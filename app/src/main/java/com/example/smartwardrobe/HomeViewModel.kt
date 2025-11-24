package com.example.smartwardrobe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartwardrobe.data.model.WardrobeItem
import com.example.smartwardrobe.data.model.ClothingCategory
import com.example.smartwardrobe.data.repository.WardrobeRepository
import com.example.smartwardrobe.data.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: WardrobeRepository = WardrobeRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<WardrobeItem>>(emptyList())
    val items: StateFlow<List<WardrobeItem>> = _items

    init {
        viewModelScope.launch {
            repo.getAllWardrobeItemsFlow().collect { result ->
                if (result is Result.Success) {
                    _items.value = result.data
                }
            }
        }
    }

    suspend fun addItem() {
        val newItem = WardrobeItem(
            name = "New Item",
            category = ClothingCategory.TOP
        )
        repo.addWardrobeItem(newItem)
    }

    suspend fun deleteItem(itemId: String) {
        repo.deleteWardrobeItem(itemId)
    }
}
