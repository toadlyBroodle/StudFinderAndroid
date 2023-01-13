package org.bitanon.studfinder

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*

const val SHARED_PREFS = "STUD_FINDER_SHARED_PREFS"

//private const val TAG = "StudFActivity"
class StudFActivity : AppCompatActivity() {

	private var mStudFView: StudFView? = null
	private var powerBut: ToggleButton? = null
	var sensBar: SeekBar? = null
	var beeperBut: ToggleButton? = null
	private var instrBut: Button? = null
	//private var hideAdsBut: Button? = null

	private var sensTextView: TextView? = null
	private var mUIRelLay: RelativeLayout? = null
	private var mFirebaseAnalytics: FirebaseAnalytics? = null

	@SuppressLint("ClickableViewAccessibility")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

		// load advertising
		AdMob.init(this)

		// Remove action tool title bar
		supportActionBar?.hide()

		// Get instance of SensorManager
		mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

		// use main layout xml file to display content in activity
		setContentView(R.layout.activity_main)

		// get references to all UI elements
		mStudFView = findViewById<View>(R.id.studf_view) as StudFView
		powerBut = findViewById<View>(R.id.powerBut) as ToggleButton
		sensBar = findViewById<View>(R.id.sensitivityBar) as SeekBar
		beeperBut = findViewById<View>(R.id.beeperBut) as ToggleButton
		instrBut = findViewById<View>(R.id.instrBut) as Button
		//hideAdsBut = findViewById<View>(R.id.hideAdsBut) as Button
		mUIRelLay = findViewById<View>(R.id.ui_panels_layout) as RelativeLayout
		sensTextView = findViewById<View>(R.id.textView3) as TextView

		instrBut?.setOnClickListener {

			showInstructions()
		}

		beeperBut?.setOnCheckedChangeListener { _, isChecked ->

			mStudFView?.beepOn = isChecked

			// save preferences before ad shows and resets widget state
			savePrefs()

			// show beeper interstitial
			AdMob.showInterstitial(this)
		}

		sensBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
			override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
				// update sensitivity level
				mStudFView?.sensLvl = progress
				// save setting to preferences
				savePrefs()
			}
			override fun onStartTrackingTouch(seekBar: SeekBar) {}
			override fun onStopTrackingTouch(seekBar: SeekBar) {}
		})

		// power toggle button releases after ACTION_UP
		powerBut!!.setOnTouchListener { _, event ->
			if (event.action == MotionEvent.ACTION_DOWN) {
				powerBut!!.isChecked = true
				currentlyDetecting = true
			} else if (event.action == MotionEvent.ACTION_UP) {
				currentlyDetecting = false
				powerBut!!.isChecked = false
				powerBut!!.isPressed = false

				// show detect interstitial after power button released
				AdMob.showInterstitial(this)
			}
			false
		}
	}

	override fun onResume() {
		super.onResume()

		// Make sure screen stays on while animation running
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		// get preferences
		loadPrefs()
		// set widgets from prefs
		sensBar?.progress = prefsSensLvl
		beeperBut?.isChecked = prefsBeeperOn

		// start StudFView
		mStudFView?.startStudFView(prefsSensLvl, prefsBeeperOn)
	}

	override fun onStop() {
		super.onStop()
		// count run times
		prefsRunTimes++
		//Log.d(TAG, "Run times ->" + Integer.toString(prefsRunTimes));

		// save preferences to load next time
		savePrefs()

		// Stop StudFView
		mStudFView?.stopStudFView()
	}

	private fun showInstructions() {
		Firebase.logCustomEvent(INSTRUCTIONS_SHOW)

		// display alert containing instructions
		val instructions = AlertDialog.Builder(this)
		instructions.setTitle(getString(R.string.menu_instructions))
		instructions.setMessage(getString(R.string.instructions))
		instructions.setPositiveButton(getString(R.string.instr_but_lab)) { _, _ ->
			// show instructions interstitial
			AdMob.showInterstitial(this)
		}

		// send to my developer website
		instructions.setNeutralButton(R.string.rate_support) { _, _ ->
			Firebase.logCustomEvent(INSTRUCTIONS_SUPPORT_CLICK)
			val uri = Uri.parse("https://studfinderapp.com/")
			val intent = Intent(Intent.ACTION_VIEW, uri)
			startActivity(intent)
		}
		instructions.show()
	}

	private fun savePrefs() {

		prefsBeeperOn = beeperBut?.isChecked ?: true
		prefsSensLvl = sensBar?.progress ?: 9
		prefsMagLocX = StudFView.magPosX.toInt()
		prefsMagLocY = StudFView.magPosY.toInt()
		val editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit()
		editor.putBoolean("prefs_beeper_switch", prefsBeeperOn)
		editor.putInt("prefs_sensitivity_level", prefsSensLvl)
		editor.putInt("prefs_run_times", prefsRunTimes)
		editor.putInt("prefs_mag_loc_x", prefsMagLocX)
		editor.putInt("prefs_mag_loc_y", prefsMagLocY)
		editor.apply()
	}

	private fun loadPrefs() {

		// get shared preferences
		val prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
		prefsBeeperOn = prefs.getBoolean("prefs_beeper_switch", false)
		prefsSensLvl = prefs.getInt("prefs_sensitivity_level", 10)
		prefsRunTimes = prefs.getInt("prefs_run_times", 1)
		prefsMagLocX = prefs.getInt("prefs_mag_loc_x", 80)
		prefsMagLocY = prefs.getInt("prefs_mag_loc_y", 180)
	}

/*	private fun showMinimalistPromo() {
		// show ridiculously convoluted confirmation dialog
		val inflater = layoutInflater
		val dialog = AlertDialog.Builder(this)
		val dView = inflater.inflate(R.layout.minimalist_promo, null)
		dialog.setView(dView)
		dialog.setTitle("Download MinimaList")
		dialog.setMessage("Organize your life like a minimalist.")
		dialog.setPositiveButton("Try It") { dialog, which -> openStorePage() }
		dialog.setNegativeButton("Later", null)
		dialog.show()
	}

	private fun openStorePage() {
		val uri = Uri.parse("https://jg5ms.app.goo.gl/MPZf")
		val goToMarket = Intent(Intent.ACTION_VIEW, uri)
		// for >v21, send to play store via minimaList's dynamic link, and put new activity on backstack
		goToMarket.addFlags(
			Intent.FLAG_ACTIVITY_NO_HISTORY or
					Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
					Intent.FLAG_ACTIVITY_MULTIPLE_TASK
		)
		try {
			startActivity(goToMarket)
		} catch (ignore: Exception) {
		}
	}

	fun onMinimalistWatchDemo(view: View?) {
		startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://jg5ms.app.goo.gl/tEi4")))
	}*/

	companion object {
		@JvmField
		var mSensorManager: SensorManager? = null
		var prefsRunTimes = 0
		var prefsSensLvl = 0
		var prefsBeeperOn = false
		@JvmField
		var prefsMagLocX = 0
		@JvmField
		var prefsMagLocY = 0
		@JvmField
		var currentlyDetecting = false

		//fun showToast(ctx: Context, message: String) =
		//	Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
	}
}