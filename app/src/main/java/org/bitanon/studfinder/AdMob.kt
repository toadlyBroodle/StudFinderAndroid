package org.bitanon.studfinder

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.BuildConfig
import java.util.*

const val interstitialId = ""
const val interstitialTestId = ""

class AdMob {
	companion object {
		private val TAG = "AdMob"

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
						mInterstitialAd = null
					}

					override fun onAdLoaded(interstitialAd: InterstitialAd) {
						Log.d(TAG, "Ad was loaded.")
						mInterstitialAd = interstitialAd
					}
				})

			mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
				override fun onAdClicked() {
					// Called when a click is recorded for an ad.
					Log.d(TAG, "Ad was clicked.")
				}

				override fun onAdDismissedFullScreenContent() {
					// Called when ad is dismissed.
					Log.d(TAG, "Ad dismissed fullscreen content.")
					mInterstitialAd = null
				}

				override fun onAdFailedToShowFullScreenContent(p0: AdError) {
					// Called when ad fails to show.
					Log.e(TAG, "Ad failed to show fullscreen content.")
					mInterstitialAd = null
				}

				override fun onAdImpression() {
					// Called when an impression is recorded for an ad.
					Log.d(TAG, "Ad recorded an impression.")
				}

				override fun onAdShowedFullScreenContent() {
					// Called when ad is shown.
					Log.d(TAG, "Ad showed fullscreen content.")
				}
			}
		}

		fun showInterstitial(activ: Activity?) {

			// if ad not shown in past 45secs, and not premium, then don't show ad
			if (Billing.mHasPurchasedHideAdsUpgrade &&
				!hasSufficientTimePassed(adShownTime, Date())
			)
				return

			if (activ != null && mInterstitialAd != null) {
				mInterstitialAd!!.show(activ)
			} else {
				Log.d(TAG, "The interstitial ad wasn't loaded yet.")
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