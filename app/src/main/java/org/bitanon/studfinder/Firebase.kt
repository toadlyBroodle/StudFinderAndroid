package org.bitanon.studfinder

import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

// custom analytics events
const val INSTRUCTIONS_SHOW = "instructions_show"
const val INSTRUCTIONS_SUPPORT_CLICK = "instructions_support_click"
const val AD_INTERSTITIAL_LOAD_SUCCESS = "ad_interstitial_load_success"
const val AD_INTERSTITIAL_LOAD_FAIL = "ad_interstitial_load_fail"
const val AD_INTERSTITIAL_SHOW_SUCCESS = "ad_interstitial_show_success"
const val AD_INTERSTITIAL_SHOW_FAIL = "ad_interstitial_show_fail"
const val AD_INTERSTITIAL_SHOW_ERROR = "ad_interstitial_show_error"
const val AD_INTERSTITIAL_CLICK = "ad_interstitial_click"
const val AD_INTERSTITIAL_DISMISS = "ad_interstitial_dismiss"

private const val TAG = "Firebase"
class Firebase {
	companion object {

		private var firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

		fun logCustomEvent(id: String) {
			Log.d(TAG, "logCustomEvent: $id")
			firebaseAnalytics.logEvent(id) {
				param(FirebaseAnalytics.Param.ITEM_ID, id)
				param(FirebaseAnalytics.Param.CONTENT_TYPE, id)
			}
		}
	}
}