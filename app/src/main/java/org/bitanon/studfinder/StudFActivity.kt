package org.bitanon.studfinder

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*

private const val TAG = "StudFActivity"
const val SHARED_PREFS = "STUD_FINDER_SHARED_PREFS"

class StudFActivity : AppCompatActivity(), ParentListenerInterface {

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
		// load in-app billing stuff
		//Billing.init(this, lifecycleScope)
		// load advertising stuff
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

			saveData()
			showInstructions()
		}

		beeperBut?.setOnClickListener {
			mStudFView?.beepOn = it.isActivated

			// show beeper interstitial
			AdMob.showInterstitial(this)
		}

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
		loadData()

		// start StudFView
		mStudFView?.startStudFView(sensBar?.progress ?: 9,
			beeperBut?.isChecked ?: true)
	}

	override fun onPause() {
		super.onPause()

		// save preferences to use next time
		saveData()

		// Stop StudFView
		mStudFView?.stopStudFView()
	}

	override fun onStop() {
		super.onStop()

		// increment Run Times variable for use in rating prompt and save
		prefsRunTimes++
		saveData()

		//Log.d(TAG, "Run times ->" + Integer.toString(prefsRunTimes));
	}

	private fun showInstructions() {

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
			val uri = Uri.parse("https://studfinderapp.com/")
			val intent = Intent(Intent.ACTION_VIEW, uri)
			startActivity(intent)
		}
		instructions.show()
	}

/*	fun onRemoveAdsPushed(v: View) {

		// log event to firebase
		val bundle = Bundle()
		bundle.putString(FirebaseAnalytics.Param.ITEM_ID, v.id.toString())
		bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, v.resources.getResourceName(v.id))
		bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button_remove_ads")
		mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

		saveData()

		//Log.d(TAG, "Remove Ads button clicked; launching purchase flow for upgrade.");

		*//* for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. *//*
		val payload = "FUCKGOOGLE!"
		mBilling?.mHelper.launchPurchaseFlow(
			this,
			InAppPurchase.SKU_HIDE_ADS,
			InAppPurchase.RC_REQUEST,
			mBilling?.mPurchaseFinishedListener,
			payload
		)
	}*/

	private fun saveData() {

		prefsBeeperOn = beeperBut?.isChecked ?: true
		prefsSensLvl = sensBar?.progress ?: 9
		prefsMagLocX = StudFView.magPosX.toInt()
		prefsMagLocY = StudFView.magPosY.toInt()
		val editor = getPreferences(MODE_PRIVATE).edit()
		editor.putBoolean("prefs_beeper_switch", prefsBeeperOn)
		editor.putInt("prefs_sensitivity_level", prefsSensLvl)
		editor.putInt("prefs_run_times", prefsRunTimes)
		editor.putInt("prefs_mag_loc_x", prefsMagLocX)
		editor.putInt("prefs_mag_loc_y", prefsMagLocY)
		editor.putBoolean("prefs_prem", Billing.mHasPurchasedHideAdsUpgrade
		)
		editor.apply()

		//Log.d(TAG, "Saved data: hide_ads = " + String.valueOf(mHasPurchasedHideAdsUpgrade));
	}

	private fun loadData() {

		// get preferences
		val prefs = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
		beeperBut?.isChecked = prefs.getBoolean("prefs_beeper_switch", false)
		sensBar?.progress = prefs.getInt("prefs_sensitivity_level", 10)
		prefsRunTimes = prefs.getInt("prefs_run_times", 1)
		prefsMagLocX = prefs.getInt("prefs_mag_loc_x", 80)
		prefsMagLocY = prefs.getInt("prefs_mag_loc_y", 180)
		Billing.mHasPurchasedHideAdsUpgrade =
			prefs.getBoolean("prefs_prem", false)

		//Log.d(TAG, "Loaded data: hide_ads = " + String.valueOf(mHasPurchasedHideAdsUpgrade));
	}

	// updates UI to reflect model
	override fun updateUi() {

		// "Upgrade" button is only visible if the user is not premium
		//hideAdsBut!!.visibility =
			//if (Billing.mHasPurchasedHideAdsUpgrade) View.GONE else View.VISIBLE
	}

	override fun alert(str: String?) {
		val bld = AlertDialog.Builder(this)
		bld.setMessage(str)
		bld.setNeutralButton("OK", null)
		Log.d(TAG, "Showing alert dialog: $str")
		bld.create().show()
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

		fun showToast(ctx: Context, message: String) =
			Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
	}
}