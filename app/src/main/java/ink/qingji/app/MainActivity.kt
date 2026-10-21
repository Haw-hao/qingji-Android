package ink.qingji.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ink.qingji.app.data.TransactionDb

class MainActivity : ComponentActivity() {
    private lateinit var db: TransactionDb

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = TransactionDb(this)
        val prefs = getSharedPreferences("qingji_settings", MODE_PRIVATE)
        val migrations = getSharedPreferences("qingji_migrations", MODE_PRIVATE)
        if (!migrations.getBoolean("legacy_demo_removed", false)) {
            db.removeLegacyDemoData()
            migrations.edit().putBoolean("legacy_demo_removed", true).apply()
        }
        setContent { QingjiApp(db, prefs) }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }
}
