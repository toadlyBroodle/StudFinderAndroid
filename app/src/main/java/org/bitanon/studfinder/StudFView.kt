package org.bitanon.studfinder

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.SensorEventListener
import android.media.MediaPlayer
import android.hardware.Sensor
import android.view.MotionEvent
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.lang.Exception
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class StudFView(context: Context?, attributes: AttributeSet?) : View(context, attributes),
	SensorEventListener {
	private val TAG = "StudFView"

	var sensLvl = 9
	var beepOn = true

	private var mMagFieldListener: Sensor? = StudFActivity.mSensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
	private var magFldArrayX: IntArray? = null
	private val magFldArrayY: IntArray
	private val magFldArrayZ: IntArray
	private val currMagFld: IntArray
	private var nextArrayCount: Int
	private var modeX: Int? = null
	private var modeY: Int
	private var modeZ: Int
	private var sdX: Int
	private var sdY: Int
	private var sdZ: Int
	private var avgSD = 0
	private var avgDeltaMagFld = 0
	private var alternCounter: Int
	private val paintText: Paint
	private val paintTextSmall: Paint
	private val paintLEDGreen: Paint
	private val paintLEDRed1: Paint
	private val paintLEDRed2: Paint
	var beep: MediaPlayer? = null

	// touch shit
	private var viewWidth = 0
	private var viewHeight = 0
	private var mActivePointerId = INVALID_POINTER_ID
	private var mLastTouchX = 0f
	private var mLastTouchY = 0f
	private var magnetIcon: Bitmap
	var magnetPainter: Paint

	init {
		// initialize variables
		magFldArrayX = IntArray(ARRAYSIZE)
		magFldArrayY = IntArray(ARRAYSIZE)
		magFldArrayZ = IntArray(ARRAYSIZE)
		currMagFld = IntArray(3)
		modeX = 0
		modeY = 0
		modeZ = 0
		sdX = 0
		sdY = 0
		sdZ = 0
		nextArrayCount = 0
		alternCounter = 0

		// set paint attributes for late use in onDraw()
		paintText = Paint()
		paintText.color = Color.BLACK
		paintText.strokeWidth = 2f
		paintText.textSize = 38f
		paintText.style = Paint.Style.FILL_AND_STROKE
		paintTextSmall = Paint()
		paintTextSmall.color = Color.BLACK
		paintTextSmall.strokeWidth = 2f
		paintTextSmall.textSize = 20f
		paintTextSmall.style = Paint.Style.FILL_AND_STROKE
		paintLEDGreen = Paint()
		paintLEDGreen.color = Color.GREEN
		paintLEDRed1 = Paint()
		paintLEDRed1.color = Color.RED
		paintLEDRed2 = Paint()
		paintLEDRed2.color = Color.rgb(150, 0, 0)
		magnetPainter = Paint()
		magnetIcon = BitmapFactory.decodeResource(resources, R.drawable.magnet)
		// get magnet location preferences
		magPosX = StudFActivity.prefsMagLocX.toFloat()
		magPosY = StudFActivity.prefsMagLocY.toFloat()

		// beep stuff
		try {
			beep = MediaPlayer.create(context, R.raw.beep)
		} catch (e: Exception) {
			//FirebaseCrash.report(e)
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(ev: MotionEvent): Boolean {
		when (ev.action) {
			MotionEvent.ACTION_DOWN -> {
				val pointerIndex = ev.actionIndex
				val x = ev.getX(pointerIndex)
				val y = ev.getY(pointerIndex)

				// Remember where we started (for dragging)
				mLastTouchX = x
				mLastTouchY = y

				// Save the ID of this pointer (for dragging)
				mActivePointerId = ev.getPointerId(0)
			}
			MotionEvent.ACTION_MOVE -> {

				// Find the index of the active pointer and fetch its position
				val pointerIndex = ev.findPointerIndex(mActivePointerId)
				val x = ev.getX(pointerIndex)
				val y = ev.getY(pointerIndex)

				// Calculate the distance moved
				val dx = x - mLastTouchX
				val dy = y - mLastTouchY
				magPosX += dx
				magPosY += dy

				// keep magnetIcon within appropriate bounds
				if (magPosX <= 0) magPosX = 0f
				if (magPosX >= viewWidth - magnetIcon.width) magPosX =
					(viewWidth - magnetIcon.width).toFloat()
				if (magPosY <= 0) magPosY = 0f
				if (magPosY >= viewHeight - magnetIcon.height) magPosY =
					(viewHeight - magnetIcon.height).toFloat()
				invalidate()

				// Remember this touch position for the next move event
				mLastTouchX = x
				mLastTouchY = y
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				mActivePointerId = INVALID_POINTER_ID
			}
			MotionEvent.ACTION_POINTER_UP -> {
				val pointerIndex = ev.actionIndex
				val pointerId = ev.getPointerId(pointerIndex)
				if (pointerId == mActivePointerId) {
					// This was our active pointer going up. Choose a new
					// active pointer and adjust accordingly.
					val newPointerIndex = if (pointerIndex == 0) 1 else 0
					mLastTouchX = ev.getX(newPointerIndex)
					mLastTouchY = ev.getY(newPointerIndex)
					mActivePointerId = ev.getPointerId(newPointerIndex)
				}
			}
		}
		return true
	}

	fun startStudFView(sensitivity: Int, beep: Boolean) {

		sensLvl = sensitivity
		beepOn = beep

		// register sensor listener
		StudFActivity.mSensorManager!!.registerListener(
			this, mMagFieldListener,
			SensorManager.SENSOR_DELAY_UI
		)
	}

	fun stopStudFView() {

		// release beep media player
		beep?.release()

		// unregister sensor listener
		StudFActivity.mSensorManager!!.unregisterListener(this)
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		viewHeight = h
		viewWidth = w

		// load bitmaps and scale according to screen size
		val opts = BitmapFactory.Options()
		//opts.inDither = true
		opts.inPreferredConfig = Bitmap.Config.RGB_565
	}

	override fun onSensorChanged(event: SensorEvent) {
		if (event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return

		// if at end of array, start back at beginning
		if (nextArrayCount >= ARRAYSIZE) nextArrayCount = 0
		currMagFld[0] = event.values[0].toInt()
		currMagFld[1] = event.values[1].toInt()
		currMagFld[2] = event.values[2].toInt()
		magFldArrayX?.set(nextArrayCount, currMagFld[0])
		magFldArrayY[nextArrayCount] = currMagFld[1]
		magFldArrayZ[nextArrayCount] = currMagFld[2]
		nextArrayCount++
	}

	override fun onAccuracyChanged(arg0: Sensor, arg1: Int) {}

	// Algorithms to monitor change in magnetic fields in x- y- z- axis
	private fun doCalculationsOnMagData() {

		// get modes of field strength arrays
		modeX = magFldArrayX?.let { mode(it) }
		modeY = mode(magFldArrayY)
		modeZ = mode(magFldArrayZ)

		// get standard deviations of field strength arrays
		sdX = magFldArrayX?.let { stanDev(it) }!!
		sdY = stanDev(magFldArrayY)
		sdZ = stanDev(magFldArrayZ)

		// find difference between currMagFld reading and mode -> deltaMagFld
		val deltaMagFldX = modeX?.minus(currMagFld[0])?.let { abs(it) }
		val deltaMagFldY = abs(modeY - currMagFld[1])
		val deltaMagFldZ = abs(modeZ - currMagFld[2])

		// find average standard deviations and delta magnetic field strengths
		avgSD = (sdX + sdY + sdZ) / 3
		if (deltaMagFldX != null) {
			avgDeltaMagFld = (deltaMagFldX + deltaMagFldY + deltaMagFldZ) / 3
		}

		// Log.d(TAG, "avgSD ->" + avgSD + "avgDelta ->" + avgDeltaMagFld);
	}

	public override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		// call method to perform calculations on mag field data
		doCalculationsOnMagData()

		// if on button pushed then light up LEDs according to sensitivity
		if (StudFActivity.currentlyDetecting) {
			// if sensBar null, then use default sensitivity of '9'
			val revProgValues = reverseSeekBarValues(sensLvl)
			val thresh1 = 1 * 0.5f * revProgValues
			val thresh2 = 2 * 0.5f * revProgValues
			val thresh3 = 3 * 0.5f * revProgValues
			val thresh4 = 4 * 0.5f * revProgValues

			// If magnetic readings stable then light up green LED
			if (avgSD < thresh1) {
				canvas.drawCircle(
					(this.width / 2).toFloat(),
					(this.height * 0.232).roundToInt().toFloat(), 20f,  // old value: 0.199
					paintLEDGreen
				)
			}

			// if deltaMag crosses second threshold light up first red LED
			if (avgDeltaMagFld > thresh2) {
				canvas.drawCircle(
					(this.width / 2).toFloat(),
					(this.height * 0.201).roundToInt().toFloat(), 20f,  //0.168
					paintLEDRed1
				)

				// if deltaMag crosses third threshold light up second red LED
				if (avgDeltaMagFld > thresh3) {
					canvas.drawCircle(
						(this.width / 2).toFloat(),
						(this.height * 0.169).roundToInt().toFloat(), 20f,  //0.136
						paintLEDRed1
					)

					// if deltaMag crosses fourth threshold light up third red
					// LED
					if (avgDeltaMagFld > thresh4) {

						// blink top LED light
						if (alternCounter % 30 <= 15) {
							canvas.drawCircle(
								(this.width / 2).toFloat(),
								(this.height * 0.136).roundToInt().toFloat(), 20f, paintLEDRed1
							)
							alternCounter++

							// activate alarm if preference selected
							if (beepOn) {
								try {
									beep?.start()
								} catch (e: Exception) {
									//FirebaseCrash.report(e)
									Log.d(TAG, "")
								}
							}
						} else {
							canvas.drawCircle(
								(this.width / 2).toFloat(),
								(this.height * 0.136).roundToInt().toFloat(), 20f, paintLEDRed2
							)
							alternCounter++
						}
					}
				}
			}
		}

		// draw magnet marker
		canvas.drawBitmap(magnetIcon, magPosX, magPosY, magnetPainter)
		// and redraw immediately
		invalidate()
	}

	private fun reverseSeekBarValues(p: Int): Int {

		// invert progress values of seekbar
		return when (p) {
			0 -> 10
			1 -> 9
			2 -> 8
			3 -> 7
			4 -> 6
			5 -> 5
			6 -> 4
			7 -> 3
			8 -> 2
			9 -> 1
			else -> 5
		}
	}

	companion object {
		private const val ARRAYSIZE = 20
		private const val INVALID_POINTER_ID = -10
		var magPosX = 80f
		var magPosY = 80f

		// calculate the mode of an array
		fun mode(a: IntArray): Int {
			if (a.isEmpty()) return 0
			var maxValue = 0
			var maxCount = 0
			for (anA1 in a) {
				var count = 0
				for (anA in a) {
					if (anA == anA1) ++count
				}
				if (count > maxCount) {
					maxCount = count
					maxValue = anA1
				}
			}
			return maxValue
		}

		// calculate the average of an array
		private fun mean(a: IntArray): Int {
			if (a.isEmpty()) return 0
			var sum = 0
			for (anA in a) {
				sum += anA
			}
			return sum / a.size
		}

		// calculate the standard deviation of an array
		fun stanDev(a: IntArray): Int {
			if (a.size <= 1) return 0
			val avg = mean(a)
			var sum = 0
			for (anA in a) {
				sum += (anA - avg) * (anA - avg)
			}
			return sqrt((sum / (a.size - 1)).toDouble()).toInt()
		}
	}
}