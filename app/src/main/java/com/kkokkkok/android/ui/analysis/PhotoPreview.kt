package com.kkokkkok.android.ui.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kkokkkok.android.R
import com.kkokkkok.android.analysis.AnalysisClue
import com.kkokkkok.android.analysis.NormalizedRegion
import com.kkokkkok.android.ui.design.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

internal sealed interface PhotoImage {
    data object Loading : PhotoImage
    data class Loaded(val bitmap: ImageBitmap) : PhotoImage
    data object Failed : PhotoImage
}

@Composable
internal fun rememberPhotoImage(uri: Uri, attempt: Int): PhotoImage {
    val context = LocalContext.current.applicationContext
    return produceState<PhotoImage>(PhotoImage.Loading, uri, attempt) {
        value = PhotoImage.Loading
        value = try {
            PhotoImage.Loaded(withContext(Dispatchers.IO) { decodePhoto(context, uri).asImageBitmap() })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            PhotoImage.Failed
        }
    }.value
}

private fun decodePhoto(context: Context, uri: Uri): Bitmap {
    val resolver = context.contentResolver
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    requireNotNull(resolver.openInputStream(uri)).use { BitmapFactory.decodeStream(it, null, options) }
    require(options.outWidth > 0 && options.outHeight > 0)
    options.inSampleSize = 1
    while (max(options.outWidth, options.outHeight) / options.inSampleSize > 1536) options.inSampleSize *= 2
    options.inJustDecodeBounds = false
    val bitmap = requireNotNull(resolver.openInputStream(uri)).use {
        requireNotNull(BitmapFactory.decodeStream(it, null, options))
    }
    val orientation = try {
        requireNotNull(resolver.openInputStream(uri)).use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        }
    }
    if (matrix.isIdentity) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
        if (it !== bitmap) bitmap.recycle()
    }
}

/** The preview frame uses the upright image's aspect ratio, so normalized regions map directly. */
private fun NormalizedRegion.bounds(width: Float, height: Float): Rect =
    Rect(left * width, top * height, right * width, bottom * height)

@Composable
internal fun PhotoPreview(
    photo: PhotoImage,
    clues: List<AnalysisClue> = emptyList(),
    scan: Int? = null,
    isMock: Boolean = true,
) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val bitmap = (photo as? PhotoImage.Loaded)?.bitmap
        val aspectRatio = bitmap?.let { it.width.toFloat() / it.height } ?: (3f / 2f)
        val maxHeight = if (scan == null) 360.dp else 300.dp
        val frameHeight = (maxWidth / aspectRatio).coerceAtMost(maxHeight)
        val frameWidth = (frameHeight * aspectRatio).coerceAtMost(maxWidth)

        Box(Modifier.size(width = frameWidth, height = frameHeight)
            .clip(RoundedCornerShape(24.dp)).background(KkokColors.White)) {
            if (bitmap == null) {
                KkokText(stringResource(R.string.analysis_photo_loading), Modifier.align(Alignment.Center), color = KkokColors.Gray500)
            } else {
                Image(
                    bitmap,
                    stringResource(if (isMock) R.string.analysis_photo_description else R.string.analysis_photo_description_live),
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                val density = LocalDensity.current
                val width = with(density) { frameWidth.toPx() }
                val height = with(density) { frameHeight.toPx() }
                clues.withIndex().reversed().forEach { (index, clue) ->
                    clue.region?.let { region ->
                        val rect = region.bounds(width, height)
                        val color = clue.risk?.color() ?: KkokColors.Gray500
                        Box(Modifier.offset(x = with(density) { rect.left.toDp() }, y = with(density) { rect.top.toDp() })
                            .wrapContentSize(Alignment.TopStart, unbounded = true)
                            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                            .testTag("analysis-clue-${clue.id}")
                            .background(color.copy(alpha = .16f), RoundedCornerShape(10.dp))
                            .border(3.dp, color, RoundedCornerShape(10.dp))) {
                            Box(Modifier.padding(4.dp).size(22.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
                                KkokText("${index + 1}", style = KkokTypography.LabelLarge,
                                    color = clue.risk?.textColor() ?: KkokColors.White)
                            }
                        }
                    }
                }
            }
            if (scan != null) {
                Box(Modifier.offset(y = frameHeight * (scan / 100f)).fillMaxWidth().height(4.dp).background(KkokColors.Primary))
            } else if (isMock && clues.any { it.region == null }) {
                KkokText(stringResource(R.string.result_metadata_example), Modifier.align(Alignment.BottomStart).padding(12.dp)
                    .background(KkokColors.Ink, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
                    KkokTypography.LabelMedium, KkokColors.White)
            }
        }
    }
}
