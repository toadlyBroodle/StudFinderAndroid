package org.bitanon.studfinder

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.*

private const val interstitialId = "ca-app-pub-9043912704472803/5989861145"
private const val interstitialTestId = "ca-app-pub-3940256099942544/1033173712"

private const val TAG = "AdMob"
class AdMob {
	companion object {

		private var adShownTime = Date()
		var mInterstitialAd: InterstitialAd? = null

		fun init(ctx: Context) {
			Log.d(TAG, "initializing AdMob")

			var adId = interstitialId
			// when developing, use test ad id
			if (BuildConfig.DEBUG)
				adId = interstitialTestId

			// init AdMob
			MobileAds.initialize(ctx) {}

			val adRequest = AdRequest.Builder().build()

			InterstitialAd.load(ctx, adId,
				adRequest, object : InterstitialAdLoadCallback() {
					override fun onAdFailedToLoad(adError: LoadAdError) {
						Log.d(TAG, adError.toString())
						Firebase.logCustomEvent(AD_INTERSTITIAL_LOAD_FAIL)
						mInterstitialAd = null
					}

					override fun onAdLoaded(interstitialAd: InterstitialAd) {
						Firebase.logCustomEvent(AD_INTERSTITIAL_LOAD_SUCCESS)
						mInterstitialAd = interstitialAd

						mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
							override fun onAdClicked() {
								// Called when a click is recorded for an ad.
								Firebase.logCustomEvent(AD_INTERSTITIAL_CLICK)
							}

							override fun onAdDismissedFullScreenContent() {
								// Called when ad is dismissed.
								Firebase.logCustomEvent(AD_INTERSTITIAL_DISMISS)
								mInterstitialAd = null
							}

							override fun onAdFailedToShowFullScreenContent(p0: AdError) {
								// Called when ad fails to show.
								Firebase.logCustomEvent(AD_INTERSTITIAL_SHOW_FAIL)
								mInterstitialAd = null
							}

							override fun onAdImpression() {
								// Called when an impression is recorded for an ad.
								Log.d(TAG, "Ad recorded an impression.")
							}

							override fun onAdShowedFullScreenContent() {
								// Called when ad is shown.
								Firebase.logCustomEvent(AD_INTERSTITIAL_SHOW_SUCCESS)
							}
						}
					}
				})
		}

		fun showInterstitial(activ: Activity?) {

			// load new ad if last one is null
			if (mInterstitialAd == null) {
				Log.d(TAG, "Ad not shown: not loaded, loading new one")
				activ?.baseContext?.let { init(it) }
				return
			}

			// don't show ad if <45s since last impression
			if (!hasSufficientTimePassed(adShownTime, Date())) {
				Log.d(TAG, "Ad not shown: <45s since last")
				return
			}

			if (activ != null && mInterstitialAd != null) {
				mInterstitialAd!!.show(activ)
			} else {
				Firebase.logCustomEvent(AD_INTERSTITIAL_SHOW_ERROR)
			}
		}

		private fun hasSufficientTimePassed(d1: Date, d2: Date): Boolean {
			val t1: Int = (d1.time % (24 * 60 * 60 * 1000L)).toInt()
			val t2: Int = (d2.time % (24 * 60 * 60 * 1000L)).toInt()

			//Log.d(TAG, "time diff = " + (t2 - t1));
			return if (t2 - t1 > 45000) {
				// update ad last shown time
				adShownTime = d2
				true
			} else false
		}
	}
}