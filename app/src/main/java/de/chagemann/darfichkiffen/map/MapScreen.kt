package de.chagemann.darfichkiffen.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import de.chagemann.darfichkiffen.R
import de.chagemann.darfichkiffen.map.MapViewModel.SideEffect
import de.chagemann.darfichkiffen.map.MapViewModel.UiAction
import de.chagemann.darfichkiffen.map.MapViewModel.ViewState
import de.chagemann.darfichkiffen.ui.theme.DarfIchKiffenTheme
import kotlinx.coroutines.flow.MutableStateFlow

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
    val onAction = { uiAction: UiAction -> viewModel.onAction(uiAction) }
    val locationPermissionState =
        rememberMultiplePermissionsState(permissions = MapConstants.locationPermissions)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startMapLocation, 6f)
    }

    val sideEffects = viewModel.effects
    LaunchedEffect("side-effect") {
        sideEffects.collect { effect ->
            when (effect) {
                SideEffect.RequestLocationPermissions -> {
                    locationPermissionState.launchMultiplePermissionRequest()
                }

                is SideEffect.AnimateMapToPosition -> {
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(effect.latLng, 16f)
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
            uiSettings = mapUiSettings,
        ) {
            Marker(
                state = rememberMarkerState(position = startMapLocation),
                title = "germany",
                snippet = "Marker in germany"
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

@Composable
private fun LocationButton(
    isUpdatingLocation: Boolean,
    isPermissionGranted: Boolean,
    onAction: (UiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonModifier = Modifier
        .systemGesturesPadding()
        .shadow(2.dp, MaterialTheme.shapes.small)
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
    val iconModifier = Modifier.size(24.dp)

    when {
        isUpdatingLocation -> {
            IconButton(
                onClick = { },
                modifier = modifier.then(buttonModifier)
            ) {
                CircularProgressIndicator(modifier = iconModifier, strokeWidth = 2.dp)
            }
        }

        isPermissionGranted -> {
            IconButton(
                onClick = { onAction(UiAction.CenterOnCurrentLocation) },
                modifier = modifier.then(buttonModifier)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.my_location_enabled),
                    contentDescription = null,
                    modifier = iconModifier,
                )
            }
        }

        else -> {
            IconButton(
                onClick = { onAction(UiAction.RequestLocationPermissions) },
                modifier = modifier
                    .then(buttonModifier)
                    .systemGesturesPadding()
                    .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.my_location_disabled),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
fun MultiplePermissionsState.isAnyLocationPermissionGranted(): Boolean {
    return permissions.any { state ->
        state.permission in MapConstants.locationPermissions && state.status == PermissionStatus.Granted
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showBackground = true)
@Composable
private fun MapScreenPreview() {
    DarfIchKiffenTheme {
        MapScreenContent(
            state = MutableStateFlow(ViewState(isUpdatingLocation = false)).collectAsState(),
            onAction = {},
            cameraPositionState = rememberCameraPositionState(),
            locationPermissionState = null,
        )
    }
}

@Preview
@Composable
private fun LocationButtonLoadingPreview() {
    DarfIchKiffenTheme {
        LocationButton(isUpdatingLocation = true, isPermissionGranted = false, onAction = {})
    }
}

@Preview
@Composable
private fun LocationButtonGrantedPreview() {
    DarfIchKiffenTheme {
        LocationButton(isUpdatingLocation = false, isPermissionGranted = true, onAction = {})
    }
}

@Preview
@Composable
private fun LocationButtonNotGrantedPreview() {
    DarfIchKiffenTheme {
        LocationButton(isUpdatingLocation = false, isPermissionGranted = false, onAction = {})
    }
}