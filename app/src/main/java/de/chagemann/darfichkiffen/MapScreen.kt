package de.chagemann.darfichkiffen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import de.chagemann.darfichkiffen.ui.theme.DarfIchKiffenTheme

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state = viewModel.viewState.collectAsState()

    val singapore = LatLng(1.35, 0.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 2f)
    }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            position = singapore,
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }
}

@Preview
@Composable
private fun MapScreenPreview() {
    DarfIchKiffenTheme {
        MapScreen()
    }
}