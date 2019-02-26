package jp.kiroru.imagetrimmer.sample

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import jp.kiroru.imagetrimmer.ImageTrimmerActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CHOOSER = 1
        private const val REQUEST_IMAGE_TRIMMER = 2
    }

    private lateinit var filePathProvider: FilePathProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filePathProvider = FilePathProvider(this)

        // Display the previous trimming result image
        val resultPath = filePathProvider.trimmingImageFile.absolutePath
        val image: Bitmap? = BitmapFactory.decodeFile(resultPath)
        imageView.setImageBitmap(image)

        trimButton.setOnClickListener {
            showChooser()
        }
    }

    /** Select the image source */
    private fun showChooser() {
        filePathProvider.deleteCaptureImageFile()
        filePathProvider.deleteTrimmingImageFile()
        imageView.setImageBitmap(null)

        // Generate Intent for camera startup
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, filePathProvider.captureImageUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // Generate Intent for Gallery
        val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/jpeg"
        }

        // Show Chooser
        val intent = Intent.createChooser(cameraIntent, "Image selection").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(galleryIntent))
        }

        startActivityForResult(intent, REQUEST_CHOOSER)
    }

    /** Display ImageTrimmerActivity */
    private fun showImageTrimmer(inputUri: Uri, outputFile: File) {
        // Customize settings
        // If not specified, the default value is applied
        val options = ImageTrimmerActivity.Options()
            .apply {
                zoomingMultiplier = 2f
                cancelButtonTitle = "Cancel"
                confirmButtonTitle = "OK"
                frameWidth = 4f
                frameColor = Color.WHITE
                frameDashPattern = floatArrayOf(8f, 4f, 4f, 4f)
            }

        // Display trimming activity
        val intent = ImageTrimmerActivity.createIntent(this, inputUri, outputFile, options)
        startActivityForResult(intent, REQUEST_IMAGE_TRIMMER)
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CHOOSER -> {
                // Receive selected image from external application
                if (resultCode != RESULT_OK) {
                    return
                }

                // Get results from Gallery or Camera
                val resultUri = (data?.data ?: filePathProvider.captureImageUri) ?: return

                // Reduce the image considering memory capacity and texture size
                val captureFile = filePathProvider.captureImageFile
                reduce(resultUri, captureFile)

                // Acquires Uri of the reduced image and File of the output destination and displays the trimming screen
                val captureUri = filePathProvider.captureImageUri
                val trimmingFile = filePathProvider.trimmingImageFile
                showImageTrimmer(captureUri, trimmingFile)
            }
            REQUEST_IMAGE_TRIMMER -> {
                if (resultCode != Activity.RESULT_OK) {
                    // Edit canceled
                    showAlert("Trimming canceled")
                    return
                }

                // Get results from temporary storage destination
                val resultPath = filePathProvider.trimmingImageFile.absolutePath
                val image = BitmapFactory.decodeFile(resultPath)
                imageView.setImageBitmap(image)
            }
        }
    }

    /** Perform rotation correction and reduction on the image */
    private fun reduce(imageUri: Uri, outputFile: File): Boolean {
        // Determine the maximum size of the image from the screen size
        val screenSize = Point().apply {
            windowManager.defaultDisplay.getRealSize(this)
        }
        val requestSize = Math.max(screenSize.x, screenSize.y)

        // Acquire a reduced image
        var bitmap = ImageFileUtils.createReducedBitmap(this, imageUri, requestSize)?.let {
            val rMatrix = Matrix().apply {
                postRotate(ImageFileUtils.getImageRotation(this@MainActivity, imageUri).toFloat())
            }
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, rMatrix, true)
        } ?: return false

        // Save reduced images
        return ImageFileUtils.saveBitmapToFile(bitmap, outputFile) &&
                ImageFileUtils.resetOrientation(outputFile.absolutePath)
    }
}
