package de.chagemann.darfichkiffen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import de.chagemann.darfichkiffen.MapViewModel.*
import de.chagemann.darfichkiffen.ui.theme.DarfIchKiffenTheme

private val mapUiSettings: MapUiSettings =
    MapUiSettings(
        compassEnabled = false,
        indoorLevelPickerEnabled = false,
        mapToolbarEnabled = false,
        zoomControlsEnabled = false,
    )

private val startMapLocation = LatLng(52.0, 10.45)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state = viewModel.viewState.collectAsState()
    val onAction = { uiAction: UiAction -> viewModel.onAction(uiAction)}
    val locationPermissionState =
        rememberMultiplePermissionsState(permissions = MapConstants.locationPermissions)

    val sideEffects = viewModel.effects
    LaunchedEffect("side-effect") {
        sideEffects.collect { effect ->
            when (effect) {
                SideEffect.RequestLocationPermissions -> {
                    locationPermissionState.launchMultiplePermissionRequest()
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startMapLocation, 6f)
    }
    Box(modifier = Modifier) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
        ) {
            Marker(
                state = rememberMarkerState(position = startMapLocation),
                title = "germany",
                snippet = "Marker in germany"
            )
        }

        LocationButton(
            isPermissionGranted = locationPermissionState.isAnyLocationPermissionGranted(),
            onAction = onAction,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun LocationButton(
    isPermissionGranted: Boolean,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPermissionGranted) {
        IconButton(
            onClick = { onAction(UiAction.CenterOnCurrentLocation) },
            modifier = modifier
                .systemGesturesPadding()
                .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.my_location_enabled),
                contentDescription = null
            )
        }
    } else {
        IconButton(
            onClick = { onAction(UiAction.RequestLocationPermissions) },
            modifier = modifier
                .systemGesturesPadding()
                .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.my_location_disabled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun MultiplePermissionsState.isAnyLocationPermissionGranted(): Boolean {
    return permissions.any { state ->
        state.permission in MapConstants.locationPermissions && state.status == PermissionStatus.Granted
    }
}

@Preview
@Composable
private fun MapScreenPreview() {
    DarfIchKiffenTheme {
        MapScreen()
    }
}