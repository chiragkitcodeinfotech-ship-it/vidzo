package com.walyo.browser

import android.app.Application
import com.walyo.browser.ads.AppOpenAdManager
import com.walyo.browser.ads.MobileAdsGate
import com.walyo.browser.ads.RewardedAdManager
import com.walyo.browser.data.HistoryRepository
import com.walyo.browser.data.SettingsRepository
import com.walyo.browser.referral.InstallReferrerReader
import com.walyo.browser.referral.ReferralManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class WalyoApp : Application() {

    /** Lives as long as the process — for writes that must not be cancelled by screen changes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var appOpenAdManager: AppOpenAdManager
        private set
    lateinit var rewardedAdManager: RewardedAdManager
        private set
    lateinit var referralManager: ReferralManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        historyRepository = HistoryRepository(this)
        appOpenAdManager = AppOpenAdManager(this, BuildConfig.AD_APP_OPEN, appScope)
        rewardedAdManager = RewardedAdManager(this, BuildConfig.AD_REWARDED)
        referralManager = ReferralManager(InstallReferrerReader(this), settingsRepository, BuildConfig.LINK_CAMPAIGN)

        // Initialised off the main thread; ads wait for it (see MobileAdsGate).
        MobileAdsGate.initialize(this, appScope)
        appOpenAdManager.load()
    }
}
