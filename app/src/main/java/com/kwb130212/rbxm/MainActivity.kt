package com.kwb130212.rbxm

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
    }

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 32)
        }

        val title = TextView(this).apply {
            text = "Rbx.m"
            textSize = 30f
        }
        val subtitle = TextView(this).apply {
            text = "Roblox 매크로 도우미 · 개인/가족용"
            textSize = 15f
        }
        root.addView(title)
        root.addView(subtitle)

        val label = TextView(this).apply {
            text = "게임 선택"
            textSize = 18f
            setPadding(0, 40, 0, 8)
        }
        root.addView(label)

        val games = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("Roblox 기본 프로필", "내 게임 프로필 1", "내 게임 프로필 2")
            )
        }
        root.addView(games)

        val interval = TextView(this).apply {
            text = "터치 간격: 10초"
            textSize = 17f
            setPadding(0, 24, 0, 8)
        }
        root.addView(interval)

        val start = Button(this).apply {
            text = "매크로 시작"
            setOnClickListener {
                if (!isAccessibilityEnabled()) {
                    Toast.makeText(context, "먼저 접근성 서비스를 허용하세요.", Toast.LENGTH_LONG).show()
                    openAccessibilitySettings()
                    return@setOnClickListener
                }
                MacroPrefs.intervalMs(this@MainActivity, 10_000L)
                ContextCompat.startForegroundService(
                    this@MainActivity,
                    Intent(this@MainActivity, MacroForegroundService::class.java).apply {
                        action = MacroForegroundService.ACTION_START
                    }
                )
                Toast.makeText(context, "매크로 서비스를 시작했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(start)

        val stop = Button(this).apply {
            text = "매크로 정지"
            setOnClickListener {
                startService(Intent(this@MainActivity, MacroForegroundService::class.java).apply {
                    action = MacroForegroundService.ACTION_STOP
                })
            }
        }
        root.addView(stop)

        val launch = Button(this).apply {
            text = "Roblox 실행 / 로그인"
            setOnClickListener { launchRoblox() }
        }
        root.addView(launch)

        val permissions = Button(this).apply {
            text = "필수 권한 설정"
            setOnClickListener { openAccessibilitySettings() }
        }
        root.addView(permissions)

        val keepScreen = Button(this).apply {
            text = "이 화면에서는 화면 꺼짐 방지"
            setOnClickListener {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Toast.makeText(context, "화면 꺼짐 방지를 켰습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(keepScreen)

        val battery = Button(this).apply {
            text = "배터리 최적화 설정 열기"
            setOnClickListener { openBatterySettings() }
        }
        root.addView(battery)

        return root
    }

    private fun launchRoblox() {
        val launch = packageManager.getLaunchIntentForPackage("com.roblox.client")
        if (launch != null) startActivity(launch)
        else Toast.makeText(this, "Roblox 앱이 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityEnabled(): Boolean =
        RbxAccessibilityService.isConnected

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                })
            } else {
                Toast.makeText(this, "이미 배터리 최적화 제외 상태입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

object MacroPrefs {
    private const val PREFS = "macro"
    private const val INTERVAL = "interval_ms"

    fun intervalMs(context: Context, value: Long? = null): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (value != null) prefs.edit().putLong(INTERVAL, value).apply()
        return prefs.getLong(INTERVAL, 10_000L).coerceAtLeast(250L)
    }
}
