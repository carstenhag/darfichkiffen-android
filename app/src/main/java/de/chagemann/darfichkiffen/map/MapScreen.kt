package de.chagemann.darfichkiffen.map

import android.os.Build
import android.os.Build.VERSION_CODES
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.TileOverlay
import com.google.maps.android.compose.rememberCameraPositionState
import de.chagemann.darfichkiffen.DayNightPreviews
import de.chagemann.darfichkiffen.map.MapViewModel.SideEffect
import de.chagemann.darfichkiffen.map.MapViewModel.UiAction
import de.chagemann.darfichkiffen.map.MapViewModel.ViewState
import de.chagemann.darfichkiffen.ui.theme.DarfIchKiffenTheme
import kotlinx.coroutines.flow.MutableStateFlow

object MapUiConstants {
    val defaultMapUiSettings: MapUiSettings =
        MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            tiltGesturesEnabled = false,
            zoomControlsEnabled = false,
        )
    val horizontalContentMargin = 16.dp
    val verticalContentMargin = 32.dp
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state = viewModel.viewState.collectAsState()
    val onAction = { uiAction: UiAction -> viewModel.onAction(uiAction) }
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = MapConstants.locationPermissions,
        onPermissionsResult = { map ->
            val anyPermissionGranted = map.any { entry ->
                entry.key in MapConstants.locationPermissions && entry.value
            }
            if (anyPermissionGranted) {
                onAction(UiAction.GrantLocationPermission)
            }
        }
    )
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(MapConstants.mapStartLocation, MapConstants.mapStartZoom)
    }

    if (Build.VERSION.SDK_INT >= VERSION_CODES.ECLAIR_0_1) {
        Toast.makeText(LocalContext.current, "", Toast.LENGTH_SHORT)
    }

    val sideEffects = viewModel.effects
    LaunchedEffect("side-effect") {
        sideEffects.collect { effect ->
            when (effect) {
                SideEffect.RequestLocationPermissions -> {
                    locationPermissionState.launchMultiplePermissionRequest()
                }
                is SideEffect.AnimateMapToPosition -> {
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(effect.latLng, effect.zoom)
                    cameraPositionState.animate(update = cameraUpdate)
                }
                is SideEffect.UpdateCameraBearing -> {
                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(cameraPositionState.position.target)
                            .zoom(cameraPositionState.position.zoom)
                            .bearing(effect.bearing).build()
                    )
                    cameraPositionState.animate(update = cameraUpdate)
                }
            }
        }
    }

    MapScreenContent(state, onAction, cameraPositionState, locationPermissionState, modifier)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapScreenContent(
    state: State<ViewState>,
    onAction: (UiAction) -> Unit,
    cameraPositionState: CameraPositionState,
    locationPermissionState: MultiplePermissionsState?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = state.value.mapProperties,
            uiSettings = MapUiConstants.defaultMapUiSettings,
        ) {
            TileOverlay(
                tileProvider = state.value.tileSetting.tileProvider,
                transparency = 0.2f
            )
        }

        InfoBanner(
            text = stringResource(id = state.value.tileSetting.explanation),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(horizontal = MapUiConstants.horizontalContentMargin, vertical = MapUiConstants.verticalContentMargin)
        )

        CompassButton(
            cameraPositionState.position.bearing,
            onClick = {
                onAction(UiAction.ResetCameraBearing)
            },
            modifier = Modifier.align(Alignment.BottomStart)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MapUiConstants.verticalContentMargin) // this stuff is wrong
                .consumeWindowInsets(PaddingValues(16.dp))
                .systemGesturesPadding()
                .navigationBarsPadding()
        ) {
            TileSettingToggle(
                tileSetting = state.value.tileSetting,
                onAction = onAction,
            )
            Spacer(modifier = Modifier.height(4.dp))
            MapTypeToggle(
                state.value.mapProperties.mapType,
                onAction = onAction,
            )
        }

        LocationButton(
            isUpdatingLocation = state.value.isUpdatingLocation,
            isPermissionGranted = locationPermissionState?.isAnyLocationPermissionGranted() == true,
            onAction = onAction,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun MultiplePermissionsState.isAnyLocationPermissionGranted(): Boolean {
    return permissions.any { state ->
        state.permission in MapConstants.locationPermissions && state.status == PermissionStatus.Granted
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@DayNightPreviews
@Composable
private fun MapScreenPreview() {
    DarfIchKiffenTheme {
        MapScreenContent(
            state = MutableStateFlow(
                ViewState(
                    isUpdatingLocation = false,
                    mapProperties = MapProperties(),
                    tileSetting = TileSetting.Meters100
                ),
            ).collectAsState(),
            onAction = {},
            cameraPositionState = rememberCameraPositionState(),
            locationPermissionState = null,
        )
    }
}

