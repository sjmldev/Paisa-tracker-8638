package com.example

import android.content.Context
import android.util.Log
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

object AdManager {
    private var isInitialized = false
    private var startAppAd: StartAppAd? = null
    private var isAdLoading = false

    @Synchronized
    fun initialize(context: Context) {
        if (isInitialized) return
        Log.d("PaisaAds", "ADS_INIT_START")
        try {
            StartAppSDK.init(context, "204875974", false)
            // Disable return ads to avoid disruptive launching experience
            StartAppSDK.enableReturnAds(false)
            startAppAd = StartAppAd(context.applicationContext)
            isInitialized = true
            Log.d("PaisaAds", "ADS_INIT_SUCCESS")
            // Preload immediately on SDK initialization
            preloadAd(context.applicationContext)
        } catch (e: Exception) {
            Log.e("PaisaAds", "ADS_INIT_FAILED: ${e.message}")
        }
    }

    @Synchronized
    fun preloadAd(context: Context) {
        if (!isInitialized) {
            initialize(context)
            return
        }
        val ad = startAppAd ?: return
        if (ad.isReady) {
            Log.d("PaisaAds", "ADS_LOAD_SUCCESS: Ad already ready")
            return
        }
        if (isAdLoading) {
            Log.d("PaisaAds", "ADS_LOAD_START: Already loading, skipping duplicate request")
            return
        }

        isAdLoading = true
        Log.d("PaisaAds", "ADS_LOAD_START")
        try {
            ad.loadAd(object : AdEventListener {
                override fun onReceiveAd(receivedAd: Ad) {
                    isAdLoading = false
                    Log.d("PaisaAds", "ADS_LOAD_SUCCESS")
                }

                override fun onFailedToReceiveAd(receivedAd: Ad?) {
                    isAdLoading = false
                    Log.e("PaisaAds", "ADS_LOAD_FAILED")
                }
            })
        } catch (e: Exception) {
            isAdLoading = false
            Log.e("PaisaAds", "ADS_LOAD_FAILED exception: ${e.message}")
        }
    }

    fun isAdReady(): Boolean {
        return startAppAd?.isReady == true
    }

    fun showAd(context: Context, onAdClosed: () -> Unit) {
        if (!isInitialized) {
            initialize(context)
            onAdClosed()
            return
        }
        val ad = startAppAd
        if (ad != null && ad.isReady) {
            Log.d("PaisaAds", "ADS_SHOW")
            try {
                ad.showAd(object : AdDisplayListener {
                    override fun adDisplayed(displayedAd: Ad) {
                        Log.d("PaisaAds", "ADS_SHOW: Displayed")
                    }

                    override fun adNotDisplayed(displayedAd: Ad) {
                        Log.e("PaisaAds", "ADS_SHOW: Not displayed")
                        onAdClosed()
                    }

                    override fun adClicked(displayedAd: Ad) {}

                    override fun adHidden(displayedAd: Ad) {
                        Log.d("PaisaAds", "ADS_CLOSED")
                        onAdClosed()
                        // Preload next ad immediately after closing
                        preloadAd(context)
                    }
                })
            } catch (e: Exception) {
                Log.e("PaisaAds", "Failed to show ad: ${e.message}")
                onAdClosed()
            }
        } else {
            Log.d("PaisaAds", "Ad not ready on show request, preloading on-demand")
            preloadAd(context)
            onAdClosed()
        }
    }
}
