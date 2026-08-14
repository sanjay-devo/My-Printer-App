package com.myprinter.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.myprinter.app.models.FileType
import com.myprinter.app.models.PrintItem
import java.io.InputStream

object PrintUtils {

    fun getPrintItemFromUri(context: Context, uri: Uri): PrintItem? {
        val contentResolver = context.contentResolver
        var fileName = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val type = contentResolver.getType(uri)
        return if (type?.contains("pdf") == true) {
            getPdfMetadata(context, uri, fileName)
        } else {
            getImageMetadata(context, uri, fileName)
        }
    }

    private fun getImageMetadata(context: Context, uri: Uri, fileName: String): PrintItem? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, options)
                
                var orientation = ExifInterface.ORIENTATION_NORMAL
                context.contentResolver.openInputStream(uri)?.use { exifInput ->
                    val exif = ExifInterface(exifInput)
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }

                PrintItem(
                    uri = uri,
                    fileName = fileName,
                    fileType = FileType.IMAGE,
                    pageCount = 1,
                    widthPx = options.outWidth,
                    heightPx = options.outHeight,
                    orientation = orientation
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getPdfMetadata(context: Context, uri: Uri, fileName: String): PrintItem? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                var maxWidth = 0
                var maxHeight = 0
                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    maxWidth = page.width
                    maxHeight = page.height
                    page.close()
                }
                renderer.close()
                PrintItem(
                    uri = uri,
                    fileName = fileName,
                    fileType = FileType.PDF,
                    pageCount = pageCount,
                    widthPx = maxWidth,
                    heightPx = maxHeight,
                    orientation = 0
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(input, null, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                
                context.contentResolver.openInputStream(uri)?.use { input2 ->
                    val bitmap = BitmapFactory.decodeStream(input2, null, options)
                    bitmap?.let { applyExifRotation(context, uri, it) }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val renderer = PdfRenderer(pfd)
                if (pageIndex >= renderer.pageCount) return null
                val page = renderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    fun drawItemToCanvas(context: Context, canvas: Canvas, item: PrintItem, pageIndex: Int, pageWidth: Int, pageHeight: Int) {
        val bitmap = if (item.fileType == FileType.IMAGE) {
            decodeSampledBitmap(context, item.uri, pageWidth, pageHeight)
        } else {
            renderPdfPage(context, item.uri, pageIndex)
        }

        bitmap?.let { b ->
            val src = Rect(0, 0, b.width, b.height)
            val scale = Math.min(pageWidth.toFloat() / b.width, pageHeight.toFloat() / b.height)
            val dx = (pageWidth - b.width * scale) / 2
            val dy = (pageHeight - b.height * scale) / 2

            val dst = Rect(
                dx.toInt(),
                dy.toInt(),
                (dx + b.width * scale).toInt(),
                (dy + b.height * scale).toInt()
            )
            canvas.drawBitmap(b, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }
}
