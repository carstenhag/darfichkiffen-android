package de.chagemann.darfichkiffen.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import de.chagemann.darfichkiffen.LocationProviderService
import de.chagemann.darfichkiffen.toLatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
    private val json: Json,
    private val locationProviderService: LocationProviderService
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        ViewState(
            isUpdatingLocation = false
        )
    )
    val viewState = _viewState.asStateFlow()

    private val _effects: Channel<SideEffect> = Channel()
    val effects = _effects.receiveAsFlow()

    private var job: Job? = null // todo: extract logic regarding this in a service

    fun onAction(uiAction: UiAction) {
        when (uiAction) {
            UiAction.CenterOnCurrentLocation -> centerOnLocation()

            UiAction.RequestLocationPermissions -> viewModelScope.launch {
                _effects.send(SideEffect.RequestLocationPermissions)
            }
        }
    }

    private fun centerOnLocation() {
        if (job != null) return
        job = viewModelScope.launch {
            val location = locationProviderService.awaitLastLocation()
            if (location != null) {
                _viewState.update { it.copy(isUpdatingLocation = false) }
                _effects.send(SideEffect.AnimateMapToPosition(latLng = location.toLatLng()))
            }
        }.also {
            it.invokeOnCompletion { job = null }
        }
        viewModelScope.launch {
            delay(200)
            if (job?.isActive == true) {
                _viewState.update { it.copy(isUpdatingLocation = true) }
            }
        }
    }

    data class ViewState(
        val isUpdatingLocation: Boolean,
    )

    sealed interface SideEffect {
        data object RequestLocationPermissions : SideEffect
        data class AnimateMapToPosition(val latLng: LatLng) : SideEffect
    }

    sealed interface UiAction {
        data object CenterOnCurrentLocation : UiAction
        data object RequestLocationPermissions : UiAction
    }
}
