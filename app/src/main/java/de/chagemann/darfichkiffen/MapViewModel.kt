package de.chagemann.darfichkiffen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

object MapConstants {
    val locationPermissions = listOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    )
}

@HiltViewModel
class MapViewModel @Inject constructor(
    val json: Json
) : ViewModel() {

    private val _viewState = MutableStateFlow(ViewState(Any()))
    val viewState = _viewState.asStateFlow()

    private val _effects: Channel<SideEffect> = Channel()
    val effects = _effects.receiveAsFlow()

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            UiAction.CenterOnCurrentLocation -> TODO()
            UiAction.RequestLocationPermissions -> viewModelScope.launch {
                _effects.send(SideEffect.RequestLocationPermissions)
            }
        }
    }

    data class ViewState(
        val x: Any
    )

    sealed interface SideEffect {
        data object RequestLocationPermissions: SideEffect
    }

    sealed interface UiAction {
        data object CenterOnCurrentLocation : UiAction
        data object RequestLocationPermissions : UiAction
    }
}


