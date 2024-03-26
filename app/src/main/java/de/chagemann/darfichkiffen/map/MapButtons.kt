package de.chagemann.darfichkiffen.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.MapType
import de.chagemann.darfichkiffen.R

@Composable
fun TileSettingToggle(
    tileSetting: TileSetting,
    onAction: (MapViewModel.UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val newTileSetting = if (tileSetting == TileSetting.Meters100) {
        TileSetting.Meters25
    } else {
        TileSetting.Meters100
    }
    IconButton(
        onClick = { onAction(MapViewModel.UiAction.UpdateTileSetting(newTileSetting)) },
        modifier = modifier
            .shadow(2.dp, MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
    ) {
        Text(
            text = stringResource(id = tileSetting.title),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun MapTypeToggle(
    mapType: MapType,
    onAction: (MapViewModel.UiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val newMapType = if (mapType == MapType.NORMAL) {
        MapType.SATELLITE
    } else {
        MapType.NORMAL
    }
    val icon = if (mapType == MapType.NORMAL) {
        R.drawable.map_type_normal
    } else {
        R.drawable.map_type_satellite
    }
    IconButton(
        onClick = { onAction(MapViewModel.UiAction.UpdateMapType(newMapType)) },
        modifier = modifier
            .shadow(2.dp, MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun CompassButton(
    bearing: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = bearing % 360 !in -2f..2f
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .padding(start = 16.dp, bottom = 32.dp) // this stuff is wrong
                .consumeWindowInsets(PaddingValues(16.dp))
                .systemGesturesPadding()
                .navigationBarsPadding()
                .shadow(2.dp, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
        ) {
            val bearingIconOffset = -45f // the icon has to be turned to face north
            val realBearing = bearing + bearingIconOffset
            val iconModifier = Modifier
                .size(24.dp)
                .rotate(realBearing)
            Box {
                Icon(
                    painter = painterResource(id = R.drawable.compass),
                    contentDescription = null,
                    modifier = iconModifier
                )
                Icon(
                    painter = painterResource(id = R.drawable.compass_needle_north),
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun LocationButton(
    isUpdatingLocation: Boolean,
    isPermissionGranted: Boolean,
    onAction: (MapViewModel.UiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonModifier = Modifier
        .padding(end = 16.dp, bottom = 32.dp) // this stuff is wrong
        .consumeWindowInsets(PaddingValues(16.dp))
        .systemGesturesPadding()
        .navigationBarsPadding()
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
                onClick = { onAction(MapViewModel.UiAction.CenterOnCurrentLocation) },
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
                onClick = { onAction(MapViewModel.UiAction.RequestLocationPermissions) },
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