package com.editpdf.online.ads

import com.editpdf.online.config.RemoteConfigManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFrequencyManagerTest {
    private var now = 100_000L

    private fun manager(
        enabled: Boolean = true,
        frequency: Long = 3L,
        minSeconds: Long = 90L
    ) = AdFrequencyManager(
        remoteConfig = RemoteConfigManager(
            mapOf(
                RemoteConfigManager.ADS_INTERSTITIAL_AFTER_EXPORT_ENABLED to enabled,
                RemoteConfigManager.ADS_INTERSTITIAL_FREQUENCY to frequency,
                RemoteConfigManager.ADS_INTERSTITIAL_MIN_SECONDS to minSeconds
            )
        ),
        nowMillis = { now }
    )

    @Test
    fun `interstitial waits for successful action threshold`() {
        val manager = manager(minSeconds = 0)
        repeat(2) { manager.recordSuccessfulAction() }
        assertFalse(manager.canShowInterstitial())
        manager.recordSuccessfulAction()
        assertTrue(manager.canShowInterstitial())
    }

    @Test
    fun `interstitial respects cooldown after being shown`() {
        val manager = manager()
        repeat(3) { manager.recordSuccessfulAction() }
        manager.recordInterstitialShown()
        repeat(3) { manager.recordSuccessfulAction() }
        assertFalse(manager.canShowInterstitial())
        now += 90_000L
        assertTrue(manager.canShowInterstitial())
    }

    @Test
    fun `disabled remote config always blocks interstitial`() {
        val manager = manager(enabled = false, minSeconds = 0)
        repeat(10) { manager.recordSuccessfulAction() }
        assertFalse(manager.canShowInterstitial())
    }

    @Test
    fun `reset clears accumulated actions`() {
        val manager = manager(minSeconds = 0)
        repeat(3) { manager.recordSuccessfulAction() }
        assertTrue(manager.canShowInterstitial())
        manager.reset()
        assertFalse(manager.canShowInterstitial())
    }
}
