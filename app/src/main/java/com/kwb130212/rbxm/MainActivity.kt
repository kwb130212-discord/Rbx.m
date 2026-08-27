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
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }
    private lateinit var status: TextView
    private lateinit var intervalLabel: TextView
    private lateinit var gameSpinner: Spinner

    private val games = listOf(GameProfile("Brawl Stars", "com.supercell.brawlstars"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
        refreshStatus()
    }

    override fun onResume() { super.onResume(); refreshStatus() }

    override fun onDestroy() {
        OverlayEditor.hide()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 44, 32, 32)
            setBackgroundColor(0xFFF7F8FA.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Rbx.m AI"
            textSize = 34f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Brawl Stars · 온디바이스 컨트롤러"
            textSize = 15f
            setTextColor(0xFF667085.toInt())
            setPadding(0, 4, 0, 20)
        })
        status = TextView(this).apply { textSize = 15f; setPadding(20, 18, 20, 18); setBackgroundColor(0xFFE9F7EF.toInt()) }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "프로필"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        })
        gameSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, games.map { it.name })
        }
        root.addView(gameSpinner)

        intervalLabel = TextView(this).apply { textSize = 16f; setPadding(0, 22, 0, 2) }
        root.addView(intervalLabel)
        root.addView(SeekBar(this).apply {
            max = 39
            progress = ((MacroPrefs.intervalMs(this@MainActivity) / 1000L).toInt() - 1).coerceIn(0, 39)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    val seconds = value + 1
                    intervalLabel.text = "자동 클릭 간격 · ${seconds}초"
                    if (fromUser) MacroPrefs.intervalMs(this@MainActivity, seconds * 1000L)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })

        root.addView(button("📍  클릭 아이콘 위치 설정") {
            if (!Settings.canDrawOverlays(this)) openOverlaySettings() else OverlayEditor.show(this)
        })
        root.addView(button("▶  자동 실행") {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "접근성 서비스를 먼저 허용하세요.", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@button
            }
            ContextCompat.startForegroundService(this, Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_START))
            refreshStatus()
        })
        root.addView(button("■  정지") {
            startService(Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_STOP))
            refreshStatus()
        })
        root.addView(button("🎮  Brawl Stars 실행") { launchGame() })
        root.addView(button("🪟  다른 앱 위에 표시 권한") { openOverlaySettings() })
        root.addView(button("🔐  접근성 서비스 설정") { openAccessibilitySettings() })
        root.addView(button("🔋  배터리 최적화 설정") { openBatterySettings() })
        root.addView(TextView(this).apply {
            text = "안내\n• 위치 설정에서 ⚔ 공격 / ★ 특수 / ✚ 이동 마커를 드래그하세요.\n• 위치는 화면 비율로 저장되어 해상도가 바뀌어도 보정됩니다.\n• 자동 입력은 Brawl Stars가 전면에 있을 때만 허용됩니다.\n• 오버레이 권한은 Android 설정에서 직접 허용해야 합니다."
            textSize = 13f
            setTextColor(0xFF667085.toInt())
            setPadding(0, 24, 0, 0)
        })
        return root
    }

    private fun button(textValue: String, action: () -> Unit): Button = Button(this).apply {
        text = textValue; textSize = 15f; gravity = Gravity.CENTER; setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, 56).apply { topMargin = 10 }
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        val accessibility = if (isAccessibilityEnabled()) "접근성 연결됨" else "접근성 연결 필요"
        val overlay = if (Settings.canDrawOverlays(this)) "오버레이 허용" else "오버레이 권한 필요"
        val game = if (packageManager.getLaunchIntentForPackage("com.supercell.brawlstars") != null) "Brawl Stars 설치됨" else "Brawl Stars 미설치"
        val running = if (MacroPrefs.isRunning(this)) "자동 실행 중" else "정지"
        status.text = "$accessibility · $overlay\n$game · $running"
        intervalLabel.text = "자동 클릭 간격 · ${MacroPrefs.intervalMs(this) / 1000L}초"
    }

    private fun launchGame() {
        packageManager.getLaunchIntentForPackage("com.supercell.brawlstars")?.let { startActivity(it) }
            ?: Toast.makeText(this, "Brawl Stars가 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show()
    }

    private fun isAccessibilityEnabled() = RbxAccessibilityService.isConnected
    private fun openAccessibilitySettings() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    private fun openOverlaySettings() = startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))

    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            else Toast.makeText(this, "이미 배터리 최적화 제외 상태입니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

data class GameProfile(val name: String, val packageName: String)
