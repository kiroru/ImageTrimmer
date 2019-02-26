package jp.kiroru.imagetrimmer

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Created by ktakeguchi on 2019/03/08.
 * Copyright © 2018年 Kiroru Inc. All rights reserved.
 */
internal class TrimFrameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val frameWeight = 0.75f
    private val framePaint = Paint()
    private val framePaintRect = RectF()

    /** local range of the trimming area */
    val trimAreaBounds = RectF()

    /** Range of trimming area from parent view */
    val trimAreaFrame = RectF()

    private var _frameWidth: Float = dp2Px(1.0f, context)
    private var _frameColor: Int = Color.RED
    private var _frameDashPattern: FloatArray = floatArrayOf(5f, 5f)

    /** Width of dashed line indicating trimming area */
    var frameWidth: Float
        get() = _frameWidth
        set(value) {
            _frameWidth = value
            invalidateAttributes()
        }

    /** Color of dashed line */
    var frameColor: Int
        get() = _frameColor
        set(value) {
            _frameColor = value
            invalidateAttributes()
        }

    /**
     * Pattern of dashed line
     * The array must contain an even number of entries (>=2), with
     * the even indices specifying the "on" intervals, and the odd indices
     * specifying the "off" intervals.
     */
    var frameDashPattern: FloatArray
        get() = _frameDashPattern
        set(value) {
            _frameDashPattern = value
            invalidateAttributes()
        }

    /** Layout change listener */
    private val layoutChangeListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                val w = right - left
                val h = bottom - top
                val frameSize =
                    Math.min(w * frameWeight, h * frameWeight)

                trimAreaBounds.set(
                    paddingLeft + frameWidth,
                    paddingTop + frameWidth,
                    paddingLeft + paddingRight + frameSize - frameWidth * 2f,
                    paddingTop + paddingBottom + frameSize - frameWidth * 2f
                )

                trimAreaFrame.set(
                    (w - trimAreaBounds.width()) / 2f,
                    (h - trimAreaBounds.height()) / 2f,
                    (w - trimAreaBounds.width()) / 2f + trimAreaBounds.width(),
                    (h - trimAreaBounds.height()) / 2f + trimAreaBounds.height()
                )

                framePaintRect.set(
                    trimAreaFrame.left + frameWidth / 2f,
                    trimAreaFrame.top + frameWidth / 2f,
                    trimAreaFrame.right - frameWidth / 2f,
                    trimAreaFrame.bottom - frameWidth / 2f
                )
            }
        }

    init {
        init(attrs, defStyleAttr)
        addOnLayoutChangeListener(layoutChangeListener)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.libkrTrimFrameView, defStyle, 0
        )

        _frameColor = a.getColor(
            R.styleable.libkrTrimFrameView_frameColor,
            _frameColor
        )
        _frameWidth = a.getDimension(
            R.styleable.libkrTrimFrameView_frameWidth,
            _frameWidth
        )

        a.recycle()

        invalidateAttributes()
    }

    private fun invalidateAttributes() {
        framePaint.apply {
            color = frameColor
            strokeWidth = frameWidth
            pathEffect = DashPathEffect(frameDashPattern, 0f)
            style = Paint.Style.STROKE
        }
    }

    private fun dp2Px(dp: Float, context: Context): Float {
        return dp * context.resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(framePaintRect, framePaint)
    }
}
