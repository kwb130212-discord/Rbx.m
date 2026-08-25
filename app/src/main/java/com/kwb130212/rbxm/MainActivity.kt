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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshStatus() }

    private lateinit var status: TextView
    private lateinit var intervalLabel: TextView
    private lateinit var gameSpinner: Spinner

    private val games = listOf(
        GameProfile("Roblox 기본", null),
        GameProfile("내 게임 1", null),
        GameProfile("내 게임 2", null)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 44, 32, 32)
            setBackgroundColor(0xFFF7F8FA.toInt())
        }

        root.addView(TextView(this).apply {
            text = "Rbx.m"
            textSize = 34f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Roblox 매크로 · 개인/가족용"
            textSize = 15f
            setTextColor(0xFF667085.toInt())
            setPadding(0, 4, 0, 20)
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(20, 18, 20, 18)
            setBackgroundColor(0xFFE9F7EF.toInt())
        }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "게임 선택"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        })

        gameSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                games.map { it.name }
            )
        }
        root.addView(gameSpinner)

        intervalLabel = TextView(this).apply {
            textSize = 16f
            setPadding(0, 22, 0, 2)
        }
        root.addView(intervalLabel)

        val seek = SeekBar(this).apply {
            max = 39
            progress = ((MacroPrefs.intervalMs(this@MainActivity) / 1000L).toInt() - 1).coerceIn(0, 39)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    val seconds = value + 1
                    intervalLabel.text = "터치 간격  ·  ${seconds}초"
                    if (fromUser) MacroPrefs.intervalMs(this@MainActivity, seconds * 1000L)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        root.addView(seek)

        root.addView(button("▶  매크로 시작") {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "접근성 서비스를 먼저 허용하세요.", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@button
            }
            ContextCompat.startForegroundService(
                this,
                Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_START)
            )
            Toast.makeText(this, "Rbx.m 서비스가 백그라운드에서 유지됩니다.", Toast.LENGTH_SHORT).show()
            refreshStatus()
        })
        root.addView(button("■  매크로 정지") {
            startService(Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_STOP))
            refreshStatus()
        })
        root.addView(button("🎮  Roblox 실행 / 로그인") { launchRoblox() })
        root.addView(button("🔐  필수 권한 설정") { openAccessibilitySettings() })
        root.addView(button("🔋  배터리 최적화 설정") { openBatterySettings() })
        root.addView(button("☀  화면 꺼짐 방지") {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Toast.makeText(this, "이 화면에서는 화면 꺼짐을 방지합니다.", Toast.LENGTH_SHORT).show()
        })

        root.addView(TextView(this).apply {
            text = "안내\n• Rbx.m 서비스는 앱 화면을 닫거나 최근 앱에서 제거해도 유지되도록 설계됩니다.\n• 서비스가 실행 중이어도 실제 자동 터치는 Roblox가 현재 화면에 있을 때만 수행합니다.\n• 화면이 꺼지면 Android의 화면 입력 자체가 중단될 수 있습니다.\n• Roblox 계정 비밀번호는 Rbx.m에서 받거나 저장하지 않습니다."
            textSize = 13f
            setTextColor(0xFF667085.toInt())
            setPadding(0, 24, 0, 0)
        })
        return root
    }

    private fun button(textValue: String, action: () -> Unit): Button = Button(this).apply {
        text = textValue
        textSize = 15f
        gravity = Gravity.CENTER
        setOnClickListener { action() }
        val p = LinearLayout.LayoutParams(-1, 56)
        p.topMargin = 10
        layoutParams = p
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        val accessibility = if (isAccessibilityEnabled()) "접근성 연결됨" else "접근성 연결 필요"
        val roblox = if (packageManager.getLaunchIntentForPackage("com.roblox.client") != null) "Roblox 설치됨" else "Roblox 미설치"
        val running = if (MacroPrefs.isRunning(this)) "매크로 서비스 실행 중" else "매크로 정지"
        status.text = "$accessibility  ·  $roblox\n$running\n선택 게임: ${games[gameSpinner.selectedItemPosition.coerceIn(0, games.lastIndex)].name}"
        intervalLabel.text = "터치 간격  ·  ${MacroPrefs.intervalMs(this) / 1000L}초"
    }

    private fun launchRoblox() {
        val launch = packageManager.getLaunchIntentForPackage("com.roblox.client")
        if (launch != null) startActivity(launch)
        else Toast.makeText(this, "Roblox 앱이 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityEnabled(): Boolean = RbxAccessibilityService.isConnected

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
            } else Toast.makeText(this, "이미 배터리 최적화 제외 상태입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

data class GameProfile(val name: String, val placeId: Long?)