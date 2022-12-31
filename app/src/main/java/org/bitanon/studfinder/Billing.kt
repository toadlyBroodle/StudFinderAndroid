package org.bitanon.studfinder

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.FeatureType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val IN_APP_PRODUCT_ID = ""

class Billing {

	companion object {
		private const val TAG = "Billing"

		var billingClient: BillingClient? = null
		var subscriptionDetails: ProductDetails? = null
		var mHasPurchasedHideAdsUpgrade = false

		fun init(ctx: Context, lifecycleScope: LifecycleCoroutineScope) {

			val purchasesUpdatedListener =
				PurchasesUpdatedListener { billingResult, purchases ->
					if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
						for (purchase in purchases) {
							lifecycleScope.launch {
								withContext(Dispatchers.IO) {
									handlePurchase(purchase)
								}
							}
						}
					} else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
						// Handle an error caused by a user cancelling the purchase flow.
						Log.d(TAG, "User cancelled purchase flow")
					} else {
						// Handle any other error codes.
						Log.d(TAG, "Error BillingResponseCode: $billingResult")
					}

				}

			// get billingClient
			billingClient = BillingClient.newBuilder(ctx)
				.setListener(purchasesUpdatedListener)
				.enablePendingPurchases()
				.build()

			// connect to Google Play
			billingClient!!.startConnection(object : BillingClientStateListener {
				override fun onBillingSetupFinished(billingResult: BillingResult) {
					if (billingResult.responseCode ==  BillingResponseCode.OK) {
						Log.d(TAG, "The BillingClient is ready")

						// process purchases
						lifecycleScope.launch {
							withContext(Dispatchers.IO) {
								processPurchases()
							}
						}

					} else Log.d(TAG, "billingResult: $billingResult")
				}
				override fun onBillingServiceDisconnected() {
					Log.d(TAG, "The billing service disconnected")
					// Try to restart the connection on the next request to
					// Google Play by calling the startConnection() method.
				}
			})
		}

		// get subscription details using kotlin extensions
		private suspend fun processPurchases() {
			val product = QueryProductDetailsParams.Product.newBuilder().setProductId(
				IN_APP_PRODUCT_ID
			)
				.setProductType(BillingClient.ProductType.SUBS)
			val productList = ArrayList<QueryProductDetailsParams.Product>()
			productList.add(product.build())

			val params = QueryProductDetailsParams.newBuilder()
			params.setProductList(productList)

			// leverage queryProductDetails Kotlin extension function
			val productDetailsResult = withContext(Dispatchers.IO) {
				billingClient!!.queryProductDetails(params.build())
			}
			// get subscription details
			if (productDetailsResult.productDetailsList?.isNotEmpty() == true) {
				subscriptionDetails = productDetailsResult.productDetailsList!![0]
				Log.d(TAG, "subscriptionDetails: ${subscriptionDetails.toString()}")
			} else Log.d(TAG, "subscriptionDetails: ${productDetailsResult.billingResult}")
		}

		fun subscribe(activ: Activity, lifecycleScope: LifecycleCoroutineScope) {

			// check if device supports subscriptions, toast if it doesn't
			if (billingClient?.isFeatureSupported(FeatureType.SUBSCRIPTIONS)?.responseCode
				== BillingResponseCode.FEATURE_NOT_SUPPORTED) {
				StudFActivity.showToast(
					activ,
					activ.getString(R.string.toast_device_no_subscriptions)
				)
				return
			}

			if (subscriptionDetails == null) {
				// notify user of billing failure
				StudFActivity.showToast(
					activ.baseContext,
					activ.baseContext.getString(R.string.toast_billing_error)
				)
				// retry connecting to billing client
				init(activ, lifecycleScope)
				return
			}

			val productDetailsParamsList = listOf(
				BillingFlowParams.ProductDetailsParams.newBuilder()
					// retrieve a value for "productDetails" by calling queryProductDetailsAsync()
					.setProductDetails(subscriptionDetails!!)
					// to get an offer token, call ProductDetails.subscriptionOfferDetails()
					// for a list of offers that are available to the user
					.setOfferToken(subscriptionDetails!!.subscriptionOfferDetails.toString())
					.build()
			)

			val billingFlowParams = BillingFlowParams.newBuilder()
				.setProductDetailsParamsList(productDetailsParamsList)
				.build()

			// Launch the billing flow
			val billingResult = billingClient?.launchBillingFlow(activ, billingFlowParams)
			Log.d(TAG, "billingResult: $billingResult")
			Log.d(TAG, "billingClient.connectionState=${billingClient?.connectionState} -> 2=CONNECTED")
		}

		private suspend fun handlePurchase(purchase: Purchase) {
			if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
				// acknowledge purchase to google play, else will be refunded after 3 days
				if (!purchase.isAcknowledged) {
					val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
						.setPurchaseToken(purchase.purchaseToken)
					val ackPurchaseResult = withContext(Dispatchers.IO) {
						billingClient?.acknowledgePurchase(acknowledgePurchaseParams.build())
					}
					Log.d(TAG, "ackPurchaseResult: $ackPurchaseResult")
				}
				// TODO don't show ads
				// TODO remove prompt/response limits
			}
		}

		suspend fun fetchSubscription() {
			val params = QueryPurchasesParams.newBuilder()
				.setProductType(BillingClient.ProductType.SUBS)

			// uses queryPurchasesAsync Kotlin extension function
			val purchasesResult = billingClient?.queryPurchasesAsync(params.build())
			Log.d(TAG, "fetch subscription purchasesResult: $purchasesResult")
		}

	}
}