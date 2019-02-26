package jp.kiroru.imagetrimmer.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by ktakeguchi on 2019/03/11.
 * Copyright © 2018年 Kiroru Inc. All rights reserved.
 */
class ImageFileUtils {
    companion object {

        /** Calculate inSampleSize to fit within requestSize */
        fun calculateInSampleSize(context: Context, uri: Uri, requestSize: Int): Int {
            var stream: InputStream? = null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            return try {
                stream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(stream, null, options)
                val longer = Math.max(options.outWidth, options.outHeight)
                if (longer > requestSize) {
                    longer / requestSize
                } else {
                    1
                }
            } catch (e: Exception) {
                1
            } finally {
                stream?.close()
            }
        }

        /** Acquire image rotation from Uri */
        fun getImageRotation(context: Context, uri: Uri): Int {
            var stream: InputStream? = null
            return try {
                stream = context.contentResolver.openInputStream(uri)
                val exifInterface = ExifInterface(stream)
                val exifOrientation =
                    exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                when (exifOrientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } catch (e: Exception) {
                0
            } finally {
                stream?.close()
            }
        }

        /** Retrieve the reduced image below requestSize */
        fun createReducedBitmap(context: Context, uri: Uri, requestSize: Int): Bitmap? {
            var stream: InputStream? = null
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(context, uri, requestSize)
            }
            return try {
                stream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(stream, null, options)
            } catch (e: Exception) {
                null
            } finally {
                stream?.close()
            }
        }

        /** Save images to a file */
        fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
            var stream: OutputStream? = null
            return try {
                stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            } catch (e: Exception) {
                false
            } finally {
                stream?.close()
            }
        }

        /** Reset file Orientation tag */
        fun resetOrientation(filePath: String): Boolean {
            return try {
                val exifInterface = ExifInterface(filePath)
                exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "0")
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}