package com.kkokkkok.android.ui.map

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.GestureType
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.Polygon
import com.kakao.vectormap.shape.PolygonOptions
import com.kakao.vectormap.shape.PolygonStyle
import com.kkokkkok.android.BuildConfig
import com.kkokkkok.android.location.GeoPoint
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography

/** Map shown before the user picks a point (Seoul City Hall); never saved unless the user moves or searches. */
private val DEFAULT_CENTER = GeoPoint(37.5666, 126.9784)

/** Kakao zoom level that keeps the protection radius circle inside a ~180dp-high map. */
internal fun zoomForRadius(radiusMeters: Int?): Int = when {
    radiusMeters == null -> 16
    radiusMeters <= 100 -> 16
    radiusMeters <= 300 -> 15
    radiusMeters <= 500 -> 14
    else -> 13
}

/**
 * Kakao map with a fixed center pin (Figma 장소 추가·상세·촬영 위치). When [interactive], the user pans the map and
 * [onCameraIdle] reports the new center after a gesture; programmatic moves (search result, initial point) do not.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun KkokMap(
    center: GeoPoint?,
    radiusMeters: Int?,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onCameraIdle: (GeoPoint) -> Unit = {},
) {
    if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank() || !com.kakao.vectormap.KakaoMapSdk.isInitialized()) {
        MapMessage("지도를 사용할 수 없어요", "카카오맵 앱 키가 설정되지 않았어요.", modifier)
        return
    }
    var attempt by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    if (failed) {
        MapMessage("지도를 불러오지 못했어요", "연결을 확인하고 다시 시도해 주세요.", modifier, onRetry = { failed = false; attempt++ })
        return
    }
    val latestIdle by rememberUpdatedState(onCameraIdle)
    var map by remember { mutableStateOf<KakaoMap?>(null) }
    var circle by remember { mutableStateOf<Polygon?>(null) }
    var shownRadius by remember { mutableStateOf<Int?>(-1) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    Box(modifier.clip(RoundedCornerShape(14.dp)).background(KkokColors.Gray100).testTag("kkok-map")) {
        key(attempt) {
            val view = remember(attempt) { mutableStateOf<MapView?>(null) }
            AndroidView(
                factory = { context ->
                    MapView(context).also { mapView ->
                        view.value = mapView
                        // Keep pan gestures inside the map instead of scrolling the surrounding page.
                        if (interactive) mapView.setOnTouchListener { v, event ->
                            if (event.actionMasked == MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
                            false
                        }
                        val start = center ?: DEFAULT_CENTER
                        mapView.start(object : MapLifeCycleCallback() {
                            override fun onMapDestroy() { map = null }
                            override fun onMapError(error: Exception) { map = null; failed = true }
                        }, object : KakaoMapReadyCallback() {
                            override fun getPosition(): LatLng = LatLng.from(start.latitude, start.longitude)
                            override fun getZoomLevel(): Int = zoomForRadius(radiusMeters)
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                if (!interactive) GestureType.entries.forEach { kakaoMap.setGestureEnable(it, false) }
                                else kakaoMap.setOnCameraMoveEndListener { _, position, gesture ->
                                    if (gesture != GestureType.Unknown) {
                                        val point = runCatching { GeoPoint(position.position.latitude, position.position.longitude) }.getOrNull()
                                        if (point != null) latestIdle(point)
                                    }
                                }
                                map = kakaoMap
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            DisposableEffect(lifecycle, attempt) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> view.value?.resume()
                        Lifecycle.Event.ON_PAUSE -> view.value?.pause()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                    view.value?.finish()
                    map = null
                }
            }
        }
        // Move the camera and redraw the radius when the chosen point or radius changes.
        LaunchedEffect(map, center, radiusMeters) {
            val kakaoMap = map ?: return@LaunchedEffect
            val shapes = kakaoMap.shapeManager?.layer
            circle?.let { shapes?.remove(it) }
            circle = null
            if (center == null) return@LaunchedEffect
            val target = LatLng.from(center.latitude, center.longitude)
            val camera = kakaoMap.cameraPosition?.position
            val alreadyThere = camera != null && kotlin.math.abs(camera.latitude - center.latitude) < 1e-6 &&
                kotlin.math.abs(camera.longitude - center.longitude) < 1e-6
            // Re-zoom when the radius changes; a user pan (camera already there) keeps the user's zoom.
            if (!alreadyThere || shownRadius != radiusMeters)
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(target, zoomForRadius(radiusMeters)))
            shownRadius = radiusMeters
            if (radiusMeters != null && shapes != null) {
                circle = shapes.addPolygon(PolygonOptions.from(DotPoints.fromCircle(target, radiusMeters.toFloat()),
                    PolygonStyle.from(KkokColors.Primary.copy(alpha = .15f).toArgb(), 2f, KkokColors.Primary.copy(alpha = .7f).toArgb())))
            }
        }
        if (center != null || interactive) CenterPin(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun CenterPin(modifier: Modifier) {
    // Tip of the pin sits on the map center.
    Canvas(modifier.offset(y = (-20).dp).size(30.dp, 40.dp).semantics { contentDescription = "선택한 위치" }) {
        val centerX = size.width / 2
        drawCircle(KkokColors.Primary, radius = 14.dp.toPx(), center = Offset(centerX, 14.dp.toPx()))
        val pointer = Path().apply {
            moveTo(3.dp.toPx(), 20.dp.toPx())
            lineTo(centerX, size.height)
            lineTo(size.width - 3.dp.toPx(), 20.dp.toPx())
            close()
        }
        drawPath(pointer, KkokColors.Primary)
        drawCircle(KkokColors.White, radius = 5.dp.toPx(), center = Offset(centerX, 14.dp.toPx()))
    }
}

@Composable
private fun MapMessage(title: String, body: String, modifier: Modifier, onRetry: (() -> Unit)? = null) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(KkokColors.Gray100).testTag("kkok-map-message"),
        contentAlignment = Alignment.Center) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KkokText(title, style = KkokTypography.TitleMedium.copy(textAlign = TextAlign.Center))
            KkokText(body, style = KkokTypography.BodySmall.copy(textAlign = TextAlign.Center), color = KkokColors.Gray600)
            if (onRetry != null) KkokText("다시 시도", Modifier.clip(CircleShape).background(KkokColors.White)
                .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 14.dp, vertical = 8.dp),
                KkokTypography.LabelLarge, KkokColors.Primary)
        }
    }
}
