package com.kkokkkok.android.ui.photo

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.kkokkkok.android.R
import com.kkokkkok.android.ui.analysis.PhotoImage
import com.kkokkkok.android.ui.analysis.rememberPhotoImage
import com.kkokkkok.android.ui.design.*

@Composable
internal fun PhotoLocationScreen(
    photo: Uri,
    state: PhotoLocationState,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onLocation: (String) -> Unit,
    onCategory: (String) -> Unit,
    onStart: () -> Unit,
    batchPhotoIndex: Int? = null,
    batchPhotoCount: Int = 0,
    geocoding: com.kkokkkok.android.location.GeocodingRepository = remember {
        com.kkokkkok.android.location.BackendGeocodingRepository(com.kkokkkok.android.BuildConfig.BACKEND_BASE_URL)
    },
) {
    // The chosen map point is only used to show the map; the analysis request keeps sending the address text.
    var point by remember(photo) { mutableStateOf<com.kkokkkok.android.location.GeoPoint?>(null) }
    var showErrorToast by remember { mutableStateOf(false) }
    LaunchedEffect(state.error) {
        if (state.error) {
            showErrorToast = true
            kotlinx.coroutines.delay(2_400)
            showErrorToast = false
        } else showErrorToast = false
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(30.dp)
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtLeast(18.dp)
    val photoImage = rememberPhotoImage(photo, 0)

    Column(Modifier.fillMaxSize().background(KkokColors.Background).padding(top = top)) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(48.dp)
                .clip(CircleShape).clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = "뒤로 가기" }, contentAlignment = Alignment.Center) {
                IconBadge(R.drawable.figma_back, 40.dp, KkokColors.White)
            }
            KkokText("촬영 위치 입력", Modifier.align(Alignment.Center), KkokTypography.TitleLarge)
            KkokText("2 / 3", Modifier.align(Alignment.CenterEnd).padding(end = 20.dp),
                KkokTypography.LabelLarge, KkokColors.Primary)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KkokColors.White)
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(KkokColors.Gray100), contentAlignment = Alignment.Center) {
                    val bitmap = (photoImage as? PhotoImage.Loaded)?.bitmap
                    if (bitmap != null) Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else KkokText("사진", style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    KkokText(
                        if (batchPhotoIndex != null) "사진 $batchPhotoIndex / $batchPhotoCount"
                        else "선택한 사진",
                        style = KkokTypography.LabelMedium, color = KkokColors.Gray600,
                    )
                    KkokText("사진이 선택되었어요", style = KkokTypography.LabelLarge)
                }
                KkokText("변경", Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onChangePhoto)
                    .padding(horizontal = 10.dp, vertical = 8.dp), KkokTypography.LabelLarge, KkokColors.Primary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KkokText("어디에서 찍은 사진인가요?", style = KkokTypography.TitleLarge)
                    KkokText("필수", Modifier.clip(CircleShape).background(KkokColors.Secondary)
                        .padding(horizontal = 8.dp, vertical = 4.dp), KkokTypography.LabelSmall, KkokColors.Primary)
                }
                KkokText("정확한 위치를 알아야 사진 속 단서가 그 장소를 드러내는지 판단할 수 있어요.",
                    style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            }

            com.kkokkkok.android.ui.map.LocationPicker(
                repository = geocoding,
                query = state.location,
                onQuery = onLocation,
                point = point,
                radiusMeters = null,
                onPicked = { picked, label -> point = picked; if (label.isNotBlank()) onLocation(label) },
                placeholder = "도로명, 건물명, 장소 이름을 입력",
                isError = state.error,
                fieldTag = "photo-location-field",
            )
            if (state.error) KkokText("위치를 입력해야 검사를 시작할 수 있어요", Modifier.padding(start = 4.dp),
                KkokTypography.LabelMedium, KkokColors.High)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KkokText("이 장소는", style = KkokTypography.LabelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("집 주변", "학교", "직장", "자주 가는 곳").forEach { category ->
                        val selected = state.category == category
                        KkokText(category, Modifier.weight(1f).clip(CircleShape)
                            .background(if (selected) KkokColors.Secondary else KkokColors.White)
                            .border(1.dp, if (selected) KkokColors.Primary else KkokColors.Gray200, CircleShape)
                            .clickable(role = Role.Button) { onCategory(category) }
                            .padding(horizontal = 4.dp, vertical = 9.dp),
                            KkokTypography.LabelSmall, if (selected) KkokColors.Primary else KkokColors.Gray600)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(KkokColors.White)
                .padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(R.drawable.figma_lock, 28.dp, KkokColors.Point)
                KkokText("입력한 위치는 이번 분석에만 전달되며 사진 태그로 저장되지 않아요.",
                    style = KkokTypography.BodySmall, color = KkokColors.Gray600)
            }
        }
        val enabled = state.location.isNotBlank()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showErrorToast) Row(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                .background(KkokColors.Ink).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FigmaIcon(R.drawable.figma_account_alert, Modifier.size(20.dp))
                KkokText("촬영 위치를 먼저 입력해 주세요", style = KkokTypography.BodySmall, color = KkokColors.White)
            }
            Box(Modifier.fillMaxWidth().height(58.dp).clip(CircleShape)
                .background(if (enabled) KkokColors.Primary else androidx.compose.ui.graphics.Color(0xFFFFD0A0))
                .clickable(enabled = enabled, role = Role.Button, onClick = onStart)
                .testTag("photo-location-submit"), contentAlignment = Alignment.Center) {
                KkokText("위치 노출 검사 시작", style = KkokTypography.TitleMedium, color = KkokColors.White)
            }
        }
    }
}
