package com.kkokkkok.android.protection

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/** Only exports server-produced, upright edited images; PNG encoding drops source metadata. */
class EditedPhotoExporter(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun saveToGallery(preview: ProtectionPreview): Uri = withContext(Dispatchers.IO) {
        check(Build.VERSION.SDK_INT >= 29)
        require(!preview.isMock) { "모의 편집 결과는 저장할 수 없어요." }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KkokKkok")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("저장 공간을 준비하지 못했어요.")
        try {
            write(preview, uri)
            val published = resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
            if (published != 1) throw IOException("사진 저장을 완료하지 못했어요.")
            uri
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    suspend fun saveToDocument(preview: ProtectionPreview, destination: Uri) = withContext(Dispatchers.IO) {
        require(!preview.isMock) { "모의 편집 결과는 저장할 수 없어요." }
        try { write(preview, destination) }
        catch (error: Exception) {
            runCatching { android.provider.DocumentsContract.deleteDocument(resolver, destination) }
            throw error
        }
    }

    private fun write(preview: ProtectionPreview, destination: Uri) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(preview.editedPhoto)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth.toLong() * bounds.outHeight > 24_000_000) {
            throw IOException("저장할 사진을 읽을 수 없거나 사진이 너무 커요.")
        }
        val bitmap = resolver.openInputStream(preview.editedPhoto)?.use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("저장할 사진을 읽을 수 없어요.")
        try {
            resolver.openOutputStream(destination, "w")?.use {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("사진을 저장하지 못했어요.")
            } ?: throw IOException("저장 위치에 접근하지 못했어요.")
        } finally { bitmap.recycle() }
    }

    companion object { fun fileName() = "KkokKkok_${UUID.randomUUID()}.png" }
}
