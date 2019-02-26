package jp.kiroru.imagetrimmer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

/**
 * Created by ktakeguchi on 2019/03/08.
 * Copyright © 2018年 Kiroru Inc. All rights reserved.
 */
internal class ViewerImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : ImageView(context, attrs, defStyleAttr, defStyleRes) {
    private val TAG = this.javaClass.simpleName

    /** Minimum scale */
    private var minimumScale: Float = 1.0f

    /** Maximum scale */
    var maximumScale: Float = 5.0f

    /** Rectangle of Cropping Frame */
    var trimAreaFrame: RectF? = null
        set(value) {
            field = value
            updateBaseMatrix(drawable)
        }

    /** Current rotation angle */
    private var rotateDegree: Float = 0f

    /** The following properties may be null at pre-initialization stage */

    /** Matrix in initial display state */
    private val baseMatrix: Matrix? = Matrix()
    /** Matrix representing deformation after the initial state */
    private val mdfyMatrix: Matrix? = Matrix()
    /** Matrix which finally becomes imageMatrix */
    private val drawMatrix: Matrix? = Matrix()

    /** Get concat result of baseMatrix and mdfyMatrix */
    private fun getDrawMatrix(): Matrix? {
        return drawMatrix?.apply {
            set(baseMatrix)
            postConcat(mdfyMatrix)
        }
    }

    /** Get image size after drawMatrix reflection */
    private fun getImageRect(): RectF? {
        val matrix = getDrawMatrix() ?: return null
        return RectF(
            0f, 0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        ).apply {
            matrix.mapRect(this)
        }
    }

    /**
     * Rotate by the specified angle
     * @param degree degree
     */
    fun rotateTo(degree: Float) {
        rotateDegree = (rotateDegree + degree) % 360f
        updateBaseMatrix(drawable)
    }

    /**
     * Acquire the trimmed image
     * @return trimmed image
     */
    fun getTrimmedImage(): Bitmap? {
        val trimRect = trimAreaFrame ?: return null
        val imageRect = getImageRect() ?: return null
        var bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null
        val matrix = getDrawMatrix() ?: return null

        // Calculate scale taking rotation into account
        val scale = Math.sqrt(
            Math.pow(matrix.getValue(Matrix.MSCALE_X).toDouble(), 2.0) +
                    Math.pow(matrix.getValue(Matrix.MSKEW_Y).toDouble(), 2.0)
        ).toFloat()

        // Convert the coordinates for the purpose of cropping with the drawable size
        val left = (trimRect.left - imageRect.left) / scale
        val top = (trimRect.top - imageRect.top) / scale
        val width = trimRect.width() / scale
        val height = trimRect.height() / scale

        // Calculate the magnification of the original image for drawable
        val srcScale = bitmap.width / drawable.intrinsicWidth

        // Convert the coordinates for the purpose of cropping with the original image size
        val srcLeft = (left * srcScale).toInt()
        val srcTop = (top * srcScale).toInt()
        val srcWidth = (width * srcScale).toInt()
        val srcHeight = (height * srcScale).toInt()

        // Reflect only rotation
        val rMatrix = Matrix().apply {
            preRotate(rotateDegree)
        }

        // Since reflection of Matrix and Crop at the same time may cause memory violation, it is carried out in two stages
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rMatrix, false)
        bitmap = Bitmap.createBitmap(bitmap, srcLeft, srcTop, srcWidth, srcHeight)

        return bitmap
    }

    /** Adjust baseMatrix to fill cropping frame */
    private fun updateBaseMatrix(drawable: Drawable?) {
        val drawable = drawable ?: return
        val matrix = baseMatrix ?: return

        // Image rectangle
        val srcRect = RectF(
            0f, 0f,
            drawable.intrinsicWidth.toFloat(),
            drawable.intrinsicHeight.toFloat()
        )

        // Cropping rectangle
        val dstRect = trimAreaFrame ?: RectF(
            0f, 0f,
            imageViewWidth.toFloat(),
            imageViewHeight.toFloat()
        )

        matrix.reset()

        // Adjust the size to fill dstRect while maintaining the aspect ratio
        val scaleX = dstRect.width() / srcRect.width()
        val scaleY = dstRect.height() / srcRect.height()
        val scale = Math.max(scaleX, scaleY)
        matrix.postScale(scale, scale)

        // Move to the center of dstRect
        val transX = dstRect.centerX() - srcRect.width() * scale / 2f
        val transY = dstRect.centerY() - srcRect.height() * scale / 2f
        matrix.postTranslate(transX, transY)

        // Rotate relative to the center of dstRect
        matrix.postRotate(rotateDegree, dstRect.centerX(), dstRect.centerY())

        resetImageMatrix()
    }

    /** Reset mdfyMatrix and update display */
    private fun resetImageMatrix() {
        val matrix = mdfyMatrix ?: return

        matrix.reset()
        adjustToFitInTheBoundary()
        imageMatrix = getDrawMatrix()
    }

    /** Adjust so that the image does not come out of the range of the Cropping frame */
    private fun adjustToFitInTheBoundary(): Boolean {
        val imageRect = getImageRect() ?: return false
        val matrix = mdfyMatrix ?: return false

        val imageWidth = imageRect.width()
        val frameWidth = trimAreaFrame?.width() ?: imageViewWidth.toFloat()
        val frameLeft = trimAreaFrame?.left ?: 0f
        val frameRight = trimAreaFrame?.right ?: frameWidth
        var deltaX = when {
            imageWidth <= frameWidth -> frameLeft + (frameWidth - imageWidth) / 2 - imageRect.left
            imageRect.left > frameLeft -> frameLeft - imageRect.left
            imageRect.right < frameRight -> frameRight - imageRect.right
            else -> 0f
        }

        val imageHeight = imageRect.height()
        val frameHeight = trimAreaFrame?.height() ?: imageViewHeight.toFloat()
        val frameTop = trimAreaFrame?.top ?: 0f
        val frameBottom = trimAreaFrame?.bottom ?: frameHeight
        var deltaY = when {
            imageHeight <= frameHeight -> frameTop + (frameHeight - imageHeight) / 2 - imageRect.top
            imageRect.top > frameTop -> frameTop - imageRect.top
            imageRect.bottom < frameBottom -> frameBottom - imageRect.bottom
            else -> 0f
        }

        matrix.postTranslate(deltaX, deltaY)
        return true
    }

    /** Gesture event */
    private val viewerGestureAdapter =
        ViewerGestureAdapter(context, object : ViewerGestureAdapter.ViewerGestureAdapterListener {
            override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
                val matrix = mdfyMatrix ?: return

                // X and Y have the same value because it is assumed that the aspect ratio is kept.
                val currentScale = matrix.getValue(Matrix.MSCALE_X)
                var scaleFactor = if (scaleFactor >= 1.0f) {
                    1 + (scaleFactor - 1) / currentScale
                } else {
                    1 - (1 - scaleFactor) / currentScale
                }

                // If the post-scale size exceeds the threshold, round it to the threshold
                val scale = scaleFactor * currentScale
                if (scale < minimumScale) {
                    scaleFactor = minimumScale / currentScale
                } else if (maximumScale < scale) {
                    scaleFactor = maximumScale / currentScale
                }

                matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                if (adjustToFitInTheBoundary()) {
                    imageMatrix = getDrawMatrix()
                }
            }

            override fun onScroll(deltaX: Float, deltaY: Float) {
                val matrix = mdfyMatrix ?: return

                matrix.postTranslate(deltaX, deltaY)
                if (adjustToFitInTheBoundary()) {
                    imageMatrix = getDrawMatrix()
                }
            }
        })

    /** Layout change listener */
    private val layoutChangeListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                updateBaseMatrix(drawable)
            }
        }

    init {
        // Forcibly MATRIX
        scaleType = ScaleType.MATRIX
        addOnLayoutChangeListener(layoutChangeListener)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        viewerGestureAdapter.onTouchEvent(event)
        return true
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        updateBaseMatrix(drawable)
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        updateBaseMatrix(drawable)
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        updateBaseMatrix(drawable)
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        updateBaseMatrix(drawable)
        return changed
    }
}

private fun Matrix.getValue(index: Int): Float {
    val values = FloatArray(9)
    getValues(values)
    return values[index]
}

private val ImageView.imageViewWidth
    get() = width - paddingLeft - paddingRight

private val ImageView.imageViewHeight
    get() = height - paddingTop - paddingBottom
