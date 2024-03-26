package de.chagemann.darfichkiffen.map

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.chagemann.darfichkiffen.LocationProviderService
import de.chagemann.darfichkiffen.R
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

    private fun getTileProvider(radius: Int): TileProvider {
        return object : UrlTileProvider(256, 256) {
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

    val tileProvider25Meters = getTileProvider(25)
    val tileProvider100Meters = getTileProvider(100)
}

enum class TileSetting(
    val tileProvider: TileProvider,
    @StringRes val title: Int,
    @StringRes val explanation: Int,
) {
    Meters25(MapConstants.tileProvider25Meters, R.string.tile_setting_25_meters_title, R.string.tile_setting_25_meters_explanation),
    Meters100(MapConstants.tileProvider100Meters, R.string.tile_setting_100_meters_title, R.string.tile_setting_100_meters_explanation)
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
                    isMyLocationEnabled = showMyLocation,
                ),
                tileSetting = TileSetting.Meters100,
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
            is UiAction.UpdateTileSetting ->_viewState.update {
                it.copy(tileSetting = uiAction.tileSetting)
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
        val tileSetting: TileSetting,
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
        data class UpdateTileSetting(val tileSetting: TileSetting) : UiAction
    }

    private fun hasCoarseLocationPermission() =
        PermissionChecker.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    private fun hasFineLocationPermission() =
        PermissionChecker.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
}
