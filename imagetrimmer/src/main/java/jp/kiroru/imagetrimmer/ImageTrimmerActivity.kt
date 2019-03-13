package jp.kiroru.imagetrimmer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.libkr_activity_image_trimmer.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.Serializable

class ImageTrimmerActivity : AppCompatActivity() {

    class Options(
        /** Maximum zoom magnification */
        var zoomingMultiplier: Float = 2.0f,

        /** Title of the cancel button */
        var cancelButtonTitle: String = "Cancel",

        /** Title color of the cancel button */
        @ColorInt var cancelButtonTitleColor: Int = Color.WHITE,

        /** Title of the confirm button */
        var confirmButtonTitle: String = "OK",

        /** Title color of the confirm button */
        @ColorInt var confirmButtonTitleColor: Int = Color.WHITE,

        /** Width of dashed line indicating trimming area */
        var frameWidth: Float = 1.0f,

        /** Color of dashed line */
        @ColorInt var frameColor: Int = Color.RED,

        /**
         * Pattern of dashed line
         * The array must contain an even number of entries (>=2), with
         * the even indices specifying the "on" intervals, and the odd indices
         * specifying the "off" intervals.
         */
        var frameDashPattern: FloatArray = floatArrayOf(4f, 4f)
    ) : Serializable

    companion object {
        private const val EXTRA_INPUT_URI = "input_uri"
        private const val EXTRA_OUTPUT_FILE = "output_file"
        private const val EXTRA_OPTIONS = "options"

        /**
         * Generate Intent
         *
         * Save the resulting image in outputFile.
         * The path is not returned to ActivityResult, so please manage it with the caller.
         *
         * @param context: Context
         * @param inputUri: URI of the image to be trimmed
         * @param outputFile: File to write the trimming result image
         * @param options: Options that can be null
         * @return Please pass this intent to startActivityForResult
         */
        @JvmStatic
        fun createIntent(context: Context, inputUri: Uri, outputFile: File, options: Options? = null): Intent =
            Intent(context, ImageTrimmerActivity::class.java)
                .apply {
                    putExtra(EXTRA_INPUT_URI, inputUri)
                    putExtra(EXTRA_OUTPUT_FILE, outputFile)
                    putExtra(EXTRA_OPTIONS, options)
                }
    }

    /** Configurable options */
    private var options = Options()

    /** Input uri */
    private var inputUri: Uri? = null

    /** Output file */
    private var outputFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.libkr_activity_image_trimmer)

        options = (intent.extras.get(EXTRA_OPTIONS) as? Options) ?: options
        inputUri = intent.extras.get(EXTRA_INPUT_URI) as? Uri
        outputFile = intent.extras.get(EXTRA_OUTPUT_FILE) as? File

        // When the size of the input image exceeds the gl texture size,
        // it can not draw due to the restriction.
        // Since it is the behavior of ImageView, it is not processed as a library,
        // but it may be considered in the future.

        setupViews()
    }

    /** Perform view setup */
    private fun setupViews() {
        cancelButton.setTextColor(options.cancelButtonTitleColor)
        cancelButton.text = options.cancelButtonTitle
        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        confirmButton.setTextColor(options.confirmButtonTitleColor)
        confirmButton.text = options.confirmButtonTitle
        confirmButton.setOnClickListener {
            // Save and quantify the result file
            val image = trimImageView.getTrimmedImage()

            val result = if (image != null && outputFile != null) {
                saveImage(image, outputFile!!)
            } else {
                false
            }

            val resultCode = if (result) {
                Activity.RESULT_OK
            } else {
                Activity.RESULT_CANCELED
            }

            setResult(resultCode, Intent())
            finish()
        }

        trimFrameView.frameWidth = options.frameWidth
        trimFrameView.frameColor = options.frameColor
        trimFrameView.frameDashPattern = options.frameDashPattern
        trimFrameView.addOnLayoutChangeListener(trimFrameViewLayoutChangeListener)

        // load trim image
        trimImageView.maximumScale = options.zoomingMultiplier
        trimImageView.setImageURI(inputUri)
    }

    /** Save the result image */
    private fun saveImage(image: Bitmap, outputFile: File): Boolean {
        var stream: OutputStream? = null
        return try {
            stream = FileOutputStream(outputFile)
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        } catch (e: Exception) {
            false
        } finally {
            stream?.close()
        }
    }

    /** Tell trimImageView the layout change of trimFrameView */
    private val trimFrameViewLayoutChangeListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            trimImageView.trimAreaFrame = trimFrameView.trimAreaFrame
        }
}
