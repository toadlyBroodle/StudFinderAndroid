package org.bitanon.studfinder

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

// extend View to override onTouchEvent() to catch index out of range exception
open class CaughtView: View {

	constructor(context: Context) : super(context)

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(ev: MotionEvent): Boolean =
		try { // catch this buggy function
			super.onTouchEvent(ev)
		} catch (e: IllegalArgumentException) {
			//uncomment if you really want to see these errors
			Log.e("CaughtView", "onTouchEvent error: ${e.message}")
			false
		}
}