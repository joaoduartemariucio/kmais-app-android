package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RoutePoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiPoint
import org.maplibre.geojson.Point

private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val ROUTE_SOURCE = "route"
private const val ENDS_SOURCE = "route-ends"

@Composable
fun RouteMapCard(
    path: List<RoutePoint>,
    origin: String,
    destination: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val description = stringResource(R.string.route_map)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (path.size >= 2) {
            RouteMap(
                path = path,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .semantics { contentDescription = description },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { runCatching { uriHandler.openUri(ExternalMaps.googleMaps(origin, destination)) } },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.open_in_google_maps), Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = { runCatching { uriHandler.openUri(ExternalMaps.waze(destination, path.lastOrNull())) } },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.open_in_waze), Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun RouteMap(
    path: List<RoutePoint>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val styleUrl = if (isSystemInDarkTheme()) DARK_STYLE else LIGHT_STYLE
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val endColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mapView =
        remember {
            MapLibre.getInstance(context)
            MapView(context).apply { onCreate(null) }
        }
    DisposableEffect(lifecycle, mapView) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(mapView, path, styleUrl) {
        mapView.getMapAsync { map ->
            map.uiSettings.setAllGesturesEnabled(false)
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isLogoEnabled = false
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                val points = path.map { Point.fromLngLat(it.longitude, it.latitude) }
                style.addSource(GeoJsonSource(ROUTE_SOURCE, LineString.fromLngLats(points)))
                style.addSource(GeoJsonSource(ENDS_SOURCE, MultiPoint.fromLngLats(listOf(points.first(), points.last()))))
                style.addLayer(
                    LineLayer("$ROUTE_SOURCE-line", ROUTE_SOURCE).withProperties(
                        PropertyFactory.lineColor(lineColor),
                        PropertyFactory.lineWidth(4f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    ),
                )
                style.addLayer(
                    CircleLayer("$ENDS_SOURCE-circle", ENDS_SOURCE).withProperties(
                        PropertyFactory.circleRadius(5f),
                        PropertyFactory.circleColor(lineColor),
                        PropertyFactory.circleStrokeColor(endColor),
                        PropertyFactory.circleStrokeWidth(2f),
                    ),
                )
                val bounds = LatLngBounds.Builder().includes(path.map { LatLng(it.latitude, it.longitude) }).build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 48))
            }
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}
