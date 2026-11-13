package com.kkokkkok.android.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.R
import com.kkokkkok.android.location.GeoPoint
import com.kkokkkok.android.location.GeocodingRepository
import com.kkokkkok.android.location.PlaceSearchResult
import com.kkokkkok.android.ui.design.FigmaIcon
import com.kkokkkok.android.ui.design.KkokColors
import com.kkokkkok.android.ui.design.KkokText
import com.kkokkkok.android.ui.design.KkokTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Results(val items: List<PlaceSearchResult>) : SearchState
    data object Empty : SearchState
    data class Failed(val message: String) : SearchState
}

/**
 * Address search + Kakao map. The text field holds the address label; searching shows results, choosing one or
 * panning the map reports a WGS84 point and its address through [onPicked]. Location is only what the user chose.
 */
@Composable
fun LocationPicker(
    repository: GeocodingRepository,
    query: String,
    onQuery: (String) -> Unit,
    point: GeoPoint?,
    radiusMeters: Int?,
    onPicked: (GeoPoint, String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    mapHeight: Dp = 180.dp,
    fieldTag: String = "location-search-field",
) {
    var search by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var reverseJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    fun runSearch() {
        val text = query.trim()
        if (text.isEmpty()) { search = SearchState.Failed("주소나 장소 이름을 입력해 주세요."); return }
        keyboard?.hide()
        search = SearchState.Loading
        scope.launch {
            search = try {
                repository.search(text).let { if (it.isEmpty()) SearchState.Empty else SearchState.Results(it.take(5)) }
            } catch (e: Exception) {
                SearchState.Failed(e.message ?: "주소를 검색하지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(KkokColors.White)
                .border(1.5.dp, if (isError) KkokColors.High else KkokColors.Primary, RoundedCornerShape(14.dp))
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = { onQuery(it.take(100)); if (search !is SearchState.Loading) search = SearchState.Idle },
                modifier = Modifier.weight(1f).testTag(fieldTag),
                singleLine = true,
                textStyle = KkokTypography.BodyMedium.copy(color = KkokColors.Ink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) KkokText(placeholder, style = KkokTypography.BodySmall, color = KkokColors.Gray400)
                        inner()
                    }
                },
            )
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = ::runSearch)
                    .semantics { contentDescription = "주소 검색" }.testTag("$fieldTag-search"),
                contentAlignment = Alignment.Center,
            ) { FigmaIcon(R.drawable.figma_location_search, Modifier.size(20.dp)) }
        }

        when (val current = search) {
            SearchState.Idle -> Unit
            SearchState.Loading -> SearchNote("검색하고 있어요…")
            SearchState.Empty -> SearchNote("검색 결과가 없어요. 도로명이나 건물 이름으로 다시 검색해 주세요.")
            is SearchState.Failed -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KkokText(current.message, Modifier.weight(1f), KkokTypography.BodySmall, KkokColors.High)
                if (query.isNotBlank()) KkokText("다시 시도", Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = ::runSearch)
                    .padding(horizontal = 10.dp, vertical = 6.dp), KkokTypography.LabelLarge, KkokColors.Primary)
            }
            is SearchState.Results -> Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(KkokColors.White).testTag("location-search-results"),
            ) {
                current.items.forEach { result ->
                    Column(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) {
                            reverseJob?.cancel()
                            val label = result.address?.takeIf { result.isAddress } ?: result.name
                            onQuery(label)
                            onPicked(result.point, label)
                            search = SearchState.Idle
                        }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        KkokText(result.name, style = KkokTypography.LabelLarge)
                        result.address?.takeIf { it != result.name }?.let {
                            KkokText(it, style = KkokTypography.LabelSmall, color = KkokColors.Gray500)
                        }
                    }
                }
            }
        }

        KkokMap(
            center = point,
            radiusMeters = radiusMeters,
            interactive = true,
            modifier = Modifier.fillMaxWidth().height(mapHeight),
            onCameraIdle = { moved ->
                // Panning picks the map center; the address is filled in once the reverse lookup returns.
                onPicked(moved, query)
                reverseJob?.cancel()
                reverseJob = scope.launch {
                    val address = runCatching { repository.reverse(moved) }.getOrNull()
                    val label = address ?: "지도에서 선택한 위치"
                    onQuery(label)
                    onPicked(moved, label)
                }
            },
        )
        KkokText(
            if (point == null) "주소를 검색하거나 지도를 움직여 위치를 정해 주세요." else "지도를 움직이면 가운데 핀 위치로 바뀌어요.",
            style = KkokTypography.LabelSmall, color = KkokColors.Gray500,
        )
    }
}

@Composable
private fun SearchNote(text: String) {
    KkokText(text, Modifier.padding(start = 4.dp), KkokTypography.BodySmall, KkokColors.Gray600)
}
