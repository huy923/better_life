package com.health.heartrate.phone.healthconnect

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.lifecycleScope
import com.health.heartrate.phone.R
import kotlinx.coroutines.launch

/**
 * Shown by Health Connect to explain why permissions are needed.
 * Also used to re-request permissions if previously denied.
 */
class HealthConnectPermissionActivity : AppCompatActivity() {

    private val hcManager by lazy { HealthConnectManager(this) }

    private val permissionLauncher = registerForActivityResult(
        HealthConnectClient.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(hcManager.requiredPermissions)) {
            Toast.makeText(this, "Health Connect đã được kết nối!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Cần quyền để đọc dữ liệu nhịp tim", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect_permission)

        findViewById<Button>(R.id.btnGrantHc).setOnClickListener {
            permissionLauncher.launch(hcManager.requiredPermissions)
        }

        lifecycleScope.launch {
            val hasPerms = hcManager.hasPermissions()
            findViewById<TextView>(R.id.tvHcStatus).text =
                if (hasPerms) "Đã kết nối Health Connect" else "Chưa cấp quyền"
        }
    }
}
