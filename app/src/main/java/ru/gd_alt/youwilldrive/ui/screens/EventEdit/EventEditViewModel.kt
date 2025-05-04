package ru.gd_alt.youwilldrive.ui.screens.EventEdit

import androidx.lifecycle.ViewModel

sealed class EventEditState {
    data object Idle : EventEditState()
    data object Loading : EventEditState()
}


class EventEditViewModel : ViewModel() {

}