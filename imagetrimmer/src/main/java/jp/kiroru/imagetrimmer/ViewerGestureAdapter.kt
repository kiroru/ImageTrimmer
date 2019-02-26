package jp.kiroru.imagetrimmer

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * Created by ktakeguchi on 2019/03/08.
 * Copyright © 2018年 Kiroru Inc. All rights reserved.
 */
internal class ViewerGestureAdapter(
    context: Context,
    private val listener: ViewerGestureAdapterListener
) {
    interface ViewerGestureAdapterListener {
        fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
        fun onScroll(deltaX: Float, deltaY: Float)
    }

    /** Scale gesture */
    private val scaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private var focusX = 0f
            private var focusY = 0f

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                focusX = detector.focusX
                focusY = detector.focusY
                return super.onScaleBegin(detector)
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                listener.onScale(detector.scaleFactor, focusX, focusY)
                return true
            }
        })

    /** Scroll gesture */
    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            private var lastX = 0.0f
            private var lastY = 0.0f

            override fun onDown(e: MotionEvent): Boolean {
                // Holds the coordinates of the first pointer index
                lastX = e.x
                lastY = e.y
                return super.onDown(e)
            }

            override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                // Do not process if first pointer index is different from scroll start
                // Because the scroll position will jump depending on the order of releasing finger after multi-tap
                if (e1.getPointerId(0) != e2.getPointerId(0)) {
                    return super.onScroll(e1, e2, distanceX, distanceY)
                }

                val deltaX = e2.x - lastX
                val deltaY = e2.y - lastY

                lastX = e2.x
                lastY = e2.y

                listener.onScroll(deltaX, deltaY)

                return true
            }
        })

    /** Pass MotionEvent from View's onTouchEvent */
    fun onTouchEvent(event: MotionEvent) {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
    }
}