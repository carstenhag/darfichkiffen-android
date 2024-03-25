package de.chagemann.darfichkiffen.map

import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
            return if (!checkTileExists(zoom)) {
                null
            } else try {
                URL(url)
            } catch (e: MalformedURLException) {
                throw AssertionError(e)
            }
        }

        private fun checkTileExists(zoom: Int): Boolean {
            val minZoom = 4
            val maxZoom = Int.MAX_VALUE
            return zoom in minZoom..maxZoom
        }
    }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val locationProviderService: LocationProviderService
) : ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> by lazy {
        val showMyLocation = hasCoarseLocationPermission()
        MutableStateFlow(
            ViewState(
                isUpdatingLocation = false,
                mapProperties = MapProperties(
                    mapType = MapType.NORMAL,
                    isMyLocationEnabled = showMyLocation
                )
            )
        )
    }

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
            UiAction.GrantLocationPermission -> viewModelScope.launch {
                _viewState.update { it.copy(mapProperties = it.mapProperties.copy(isMyLocationEnabled = true)) }
                centerOnLocation()
            }
            UiAction.ResetCameraBearing -> viewModelScope.launch {
                _effects.send(SideEffect.UpdateCameraBearing(bearing = 0f))
            }
            is UiAction.UpdateMapType -> _viewState.update {
                it.copy(mapProperties = it.mapProperties.copy(mapType = uiAction.newMapType))
            }
        }
    }

    private fun centerOnLocation() {
        if (job != null) return
        job = viewModelScope.launch {
            val location = locationProviderService.awaitLastLocation()
            if (location != null) {
                _viewState.update { it.copy(isUpdatingLocation = false) }
                val zoom = if (hasFineLocationPermission()) {
                    16f
                } else {
                    13f
                }
                _effects.send(SideEffect.AnimateMapToPosition(latLng = location.toLatLng(), zoom = zoom))
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
        val mapProperties: MapProperties,
    )

    sealed interface SideEffect {
        data object RequestLocationPermissions : SideEffect
        data class AnimateMapToPosition(val latLng: LatLng, val zoom: Float) : SideEffect
        data class UpdateCameraBearing(val bearing: Float) : SideEffect
    }

    sealed interface UiAction {
        data object CenterOnCurrentLocation : UiAction
        data object RequestLocationPermissions : UiAction
        data object GrantLocationPermission : UiAction
        data object ResetCameraBearing : UiAction
        data class UpdateMapType(val newMapType: MapType) : UiAction
    }

    private fun hasCoarseLocationPermission() =
        PermissionChecker.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    private fun hasFineLocationPermission() =
        PermissionChecker.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}
