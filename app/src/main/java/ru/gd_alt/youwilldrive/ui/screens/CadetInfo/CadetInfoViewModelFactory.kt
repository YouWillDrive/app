package ru.gd_alt.youwilldrive.ui.screens.CadetInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.gd_alt.youwilldrive.data.DataStoreManager

class CadetInfoViewModelFactory(
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CadetInfoViewModel::class.java)) {
            return CadetInfoViewModel(dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}