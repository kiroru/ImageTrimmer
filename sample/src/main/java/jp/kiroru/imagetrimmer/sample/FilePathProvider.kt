package jp.kiroru.imagetrimmer.sample

import android.content.Context
import android.net.Uri
import android.support.v4.content.FileProvider
import java.io.File

/**
 * Created by ktakeguchi on 2019/03/11.
 * Copyright © 2018年 Kiroru Inc. All rights reserved.
 */
class FilePathProvider(context: Context) {

    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"
        private const val CAPTURE_IMAGE_NAME = "capture.jpg"
        private const val TRIMMING_IMAGE_NAME = "trimming.jpg"
    }

    // ----------
    // Temporary save destination of captured image
    // ----------

    private val _captureImageFile =
        File(context.filesDir, "$CAPTURE_IMAGE_NAME")
            .also {
                if (!it.exists()) {
                    it.createNewFile()
                }
            }

    val captureImageFile: File
        get() = _captureImageFile

    private val _captureImageUri =
        FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            captureImageFile
        )

    val captureImageUri: Uri
        get() = _captureImageUri

    fun deleteCaptureImageFile() {
        captureImageFile.delete()
    }

    // ----------
    // Temporary save destination of trimming image
    // ----------

    private val _trimmingImageFile =
        File(context.filesDir, "$TRIMMING_IMAGE_NAME")
            .also {
                if (!it.exists()) {
                    it.createNewFile()
                }
            }

    val trimmingImageFile: File
        get() = _trimmingImageFile

    fun deleteTrimmingImageFile() {
        trimmingImageFile.delete()
    }
}