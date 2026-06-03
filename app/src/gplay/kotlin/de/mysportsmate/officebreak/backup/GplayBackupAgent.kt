package de.mysportsmate.officebreak.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class GplayBackupAgent : BackupAgent() {

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) {
        // Key/value backup is not used; Auto Backup goes through onFullBackup.
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) {
        // Key/value restore is not used.
    }

    override fun onFullBackup(data: FullBackupDataOutput?) {
        val enabled = try {
            runBlocking {
                applicationContext.dataStore.data.first()[SettingsRepository.KEY_CLOUD_BACKUP_ENABLED]
                    ?: SettingsRepository.DEFAULT_CLOUD_BACKUP_ENABLED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read cloud backup preference; defaulting to enabled", e)
            SettingsRepository.DEFAULT_CLOUD_BACKUP_ENABLED
        }

        if (!enabled) {
            Log.i(TAG, "Auto backup skipped: user disabled cloud backup in settings")
            return
        }

        super.onFullBackup(data)
    }

    companion object {
        private const val TAG = "GplayBackupAgent"
    }
}
