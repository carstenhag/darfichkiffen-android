package de.chagemann.darfichkiffen.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.UrlTileProvider
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
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject

object MapConstants {
    val mapStartLocation = LatLng(52.0, 10.45)
    val mapStartZoom = 6f
    val locationPermissions = listOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    )
    val tileProvider = object : UrlTileProvider(256, 256) {
        val radius = 100 // tiles for 100m radius

        override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {
            val url = "https://bubatzkarte.de/tiles/radius/$radius/$zoom/$x/$y.png"
            return if (!checkTileExists(x, y, zoom)) {
                null
            } else try {
                URL(url)
            } catch (e: MalformedURLException) {
                throw AssertionError(e)
            }
        }

        private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
            val minZoom = 4
            val maxZoom = Int.MAX_VALUE
            return zoom in minZoom..maxZoom
        }
    }
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
