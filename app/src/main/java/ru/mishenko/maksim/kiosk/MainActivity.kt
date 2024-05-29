package ru.mishenko.maksim.kiosk

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.w3c.dom.Text
import ru.mishenko.maksim.kiosk.ui.theme.KioskDemoTheme

class MainActivity : ComponentActivity() {
    private val mutableFeatureHome = MutableStateFlow(false)
    private val mutableFeatureOverview = MutableStateFlow(false)
    private val mutableFeatureGlobalAction = MutableStateFlow(false)
    private val mutableFeatureNotification = MutableStateFlow(false)
    private val mutableFeatureSystemInfo = MutableStateFlow(false)
    private val mutableFeatureKeyguard = MutableStateFlow(false)

    private val dpm by lazy { getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val activityManager by lazy { getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    private val adminName by lazy { AdminReceiver.getComponentName(this) }
    private val KIOSK_PACKAGE = "ru.mishenko.maksim.kiosk"
    private val APP_PACKAGES = arrayOf(KIOSK_PACKAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val featureHome by mutableFeatureHome.collectAsState()
            val featureOverview by mutableFeatureOverview.collectAsState()
            val featureGlobalAction by mutableFeatureGlobalAction.collectAsState()
            val featureNotification by mutableFeatureNotification.collectAsState()
            val featureSystemInfo by mutableFeatureSystemInfo.collectAsState()
            val featureKeyguard by mutableFeatureKeyguard.collectAsState()
            KioskDemoTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column {
                        CheckboxWithText(
                            checked = featureHome,
                            onCheckedChange = { newValue -> mutableFeatureHome.update { newValue } },
                            text = "LOCK_TASK_FEATURE_HOME"
                        )
                        CheckboxWithText(
                            checked = featureOverview,
                            onCheckedChange = { newValue ->
                                mutableFeatureOverview.update { newValue }
                                mutableFeatureHome.update { newValue }
                            },
                            text = "LOCK_TASK_FEATURE_OVERVIEW"
                        )
                        CheckboxWithText(
                            checked = featureGlobalAction,
                            onCheckedChange = { newValue -> mutableFeatureGlobalAction.update { newValue } },
                            text = "LOCK_TASK_FEATURE_GLOBAL_ACTIONS"
                        )
                        CheckboxWithText(
                            checked = featureNotification,
                            onCheckedChange = { newValue ->
                                mutableFeatureNotification.update { newValue }
                                mutableFeatureHome.update { newValue }
                            },
                            text = "LOCK_TASK_FEATURE_NOTIFICATIONS"
                        )
                        CheckboxWithText(
                            checked = featureSystemInfo,
                            onCheckedChange = { newValue -> mutableFeatureSystemInfo.update { newValue } },
                            text = "LOCK_TASK_FEATURE_SYSTEM_INFO"
                        )
                        CheckboxWithText(
                            checked = featureKeyguard,
                            onCheckedChange = { newValue -> mutableFeatureKeyguard.update { newValue } },
                            text = "LOCK_TASK_FEATURE_KEYGUARD"
                        )
                    }
                    Button(onClick = { runKiosk() }) {
                        Text("Enable LTM")
                    }
                    Button(onClick = { disableKiosk() }) {
                        Text("Disable LTM")
                    }
                    Button(onClick = { disableDeviceOwner() }) {
                        Text("Disable device admin")
                    }
                }
            }
        }
    }

    private fun Int.addFlag(flag: Int, enable: Boolean) =
        if (enable)
            this or flag
        else
            this

    private fun runKiosk() {
        val isKiosk = activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED
        val isNotHavePermissions = !dpm.isDeviceOwnerApp(adminName.packageName)

        if (isKiosk || isNotHavePermissions) return

        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "")
        }
        startActivityForResult(intent, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val flags = DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                .addFlag(
                    flag = DevicePolicyManager.LOCK_TASK_FEATURE_HOME,
                    enable = mutableFeatureHome.value
                )
                .addFlag(
                    flag = DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW,
                    enable = mutableFeatureOverview.value
                )
                .addFlag(
                    flag = DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS,
                    enable = mutableFeatureGlobalAction.value
                )
                .addFlag(
                    flag = DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS,
                    enable = mutableFeatureNotification.value
                )
                .addFlag(
                    flag = DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO,
                    enable = mutableFeatureSystemInfo.value
                )
                .addFlag(
                    flag = DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD,
                    enable = mutableFeatureKeyguard.value
                )
            dpm.setLockTaskFeatures(
                /* admin = */ adminName,
                /* flags = */ flags
            )
        }

        dpm.setLockTaskPackages(adminName, APP_PACKAGES)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Set an option to turn on lock task mode when starting the activity.
            val options = ActivityOptions.makeBasic()
            options.setLockTaskEnabled(true)

            // Start our kiosk app's main activity with our lock task mode option.
            val packageManager = packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(KIOSK_PACKAGE)
            if (launchIntent != null) {
                startActivity(launchIntent, options.toBundle())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isLockTaskPermitted(packageName) && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            startLockTask()
        }
    }

    private fun disableKiosk() {
        val adminName = AdminReceiver.getComponentName(this)
        dpm.setLockTaskPackages(adminName, emptyArray())
        val launchIntent = packageManager.getLaunchIntentForPackage(KIOSK_PACKAGE)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }

    private fun disableDeviceOwner() {
        dpm.clearDeviceOwnerApp(packageName)
        val launchIntent = packageManager.getLaunchIntentForPackage(KIOSK_PACKAGE)
        if (launchIntent != null) {
            startActivity(launchIntent)
        }
    }
}

@Composable
fun CheckboxWithText(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text)
    }
}