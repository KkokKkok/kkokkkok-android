package com.kkokkkok.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.protection.EditedPhotoExporter
import com.kkokkkok.android.protection.ProtectionPreview
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditedPhotoExporterTest {
    @Test fun exportPreservesPixelsAndStripsGpsAndRejectsMock() = runBlocking {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 29)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "synthetic-export.png")
        val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        ExifInterface(source).apply { setLatLong(37.0, 127.0); saveAttributes() }
        val preview = ProtectionPreview(Uri.fromFile(source), false, null, null, null, null, null)
        val exporter = EditedPhotoExporter(context)
        var saved: Uri? = null
        try {
            saved = exporter.saveToGallery(preview)
            context.contentResolver.openInputStream(saved)!!.use {
                val result = BitmapFactory.decodeStream(it)!!
                assertEquals(80, result.width); assertEquals(120, result.height)
                assertEquals(Color.GREEN, result.getPixel(30, 40)); result.recycle()
            }
            context.contentResolver.openInputStream(saved)!!.use { assertNull(ExifInterface(it).latLong) }
            try { exporter.saveToGallery(preview.copy(isMock = true)); fail("Mock export must fail") }
            catch (_: IllegalArgumentException) { }
            try { exporter.saveToGallery(preview.copy(editedPhoto = Uri.fromFile(File(context.cacheDir, "missing-image")))); fail("Invalid export must fail") }
            catch (_: Exception) { }
        } finally {
            saved?.let { context.contentResolver.delete(it, null, null) }
            source.delete()
        }
    }
}
