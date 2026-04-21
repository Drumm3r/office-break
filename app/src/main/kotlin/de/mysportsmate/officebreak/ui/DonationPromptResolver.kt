package de.mysportsmate.officebreak.ui

import de.mysportsmate.officebreak.data.SettingsRepository
import java.util.concurrent.TimeUnit

object DonationPromptResolver {

    fun shouldShow(
        installTimestamp: Long,
        lastShown: Long,
        dismissed: Boolean,
        timerActive: Boolean,
        nowMillis: Long,
        initialThresholdDays: Long = SettingsRepository.DONATION_PROMPT_INITIAL_DAYS,
        snoozeDays: Long = SettingsRepository.DONATION_PROMPT_SNOOZE_DAYS,
    ): Boolean {
        if (dismissed) return false
        if (timerActive) return false
        if (installTimestamp <= 0L) return false
        if (nowMillis < installTimestamp) return false

        val msSinceInstall = nowMillis - installTimestamp
        if (msSinceInstall < TimeUnit.DAYS.toMillis(initialThresholdDays)) return false

        if (lastShown > 0L) {
            val msSinceShown = nowMillis - lastShown
            if (msSinceShown < TimeUnit.DAYS.toMillis(snoozeDays)) return false
        }

        return true
    }
}
