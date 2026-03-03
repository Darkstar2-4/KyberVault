package com.kybervault

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kybervault.data.EphemeralKeyVault
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class KyberVaultApp : Application() {

    val keyVault = EphemeralKeyVault()

    override fun onCreate() {
        super.onCreate()
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        // Auto-save on background, auto-restore on foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App went to background — save current state to temp encrypted storage
                if (keyVault.size > 0 || keyVault.hasAnySessionKey()) {
                    try { keyVault.persistTemp(this@KyberVaultApp) } catch (_: Exception) {}
                }
            }

            override fun onStart(owner: LifecycleOwner) {
                // App came to foreground — restore from temp if RAM is empty
                if (keyVault.size == 0) {
                    try { keyVault.restoreTemp(this@KyberVaultApp) } catch (_: Exception) {}
                }
            }
        })
    }
}
