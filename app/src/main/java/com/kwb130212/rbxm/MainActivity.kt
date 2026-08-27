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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }
    private lateinit var status: TextView
    private lateinit var intervalLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
        refreshStatus()
    }

    override fun onResume() { super.onResume(); refreshStatus() }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 44, 32, 32)
            setBackgroundColor(0xFFF7F8FA.toInt())
        }
        root.addView(TextView(this).apply { text = "Rbx.m AI"; textSize = 34f; setTypeface(null, android.graphics.Typeface.BOLD) })
        root.addView(TextView(this).apply {
            text = "On-device combat controller · adaptive policy"
            textSize = 15f; setTextColor(0xFF667085.toInt()); setPadding(0, 4, 0, 20)
        })
        status = TextView(this).apply { textSize = 15f; setPadding(20, 18, 20, 18); setBackgroundColor(0xFFE9F7EF.toInt()) }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "분석 주기"; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 24, 0, 8)
        })
        intervalLabel = TextView(this).apply { textSize = 16f; setPadding(0, 8, 0, 2) }
        root.addView(intervalLabel)
        root.addView(SeekBar(this).apply {
            max = 15
            progress = ((MacroPrefs.intervalMs(this@MainActivity) / 100L).toInt() - 5).coerceIn(0, 15)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    val ms = (value + 5) * 100L
                    intervalLabel.text = "화면 분석 간격 · ${ms}ms"
                    if (fromUser) MacroPrefs.intervalMs(this@MainActivity, ms)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })

        root.addView(button("▶  AI 시작") {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "접근성 서비스를 먼저 허용하세요.", Toast.LENGTH_LONG).show(); openAccessibilitySettings(); return@button
            }
            ContextCompat.startForegroundService(this, Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_START))
            refreshStatus()
        })
        root.addView(button("■  AI 정지") {
            startService(Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_STOP)); refreshStatus()
        })
        root.addView(button("🎮  Brawl Stars 실행") { launchGame() })
        root.addView(button("🔐  접근성 설정") { openAccessibilitySettings() })
        root.addView(button("🔋  배터리 최적화 제외") { openBatterySettings() })
        root.addView(button("☀  화면 꺼짐 방지") {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Toast.makeText(this, "화면 꺼짐을 방지합니다.", Toast.LENGTH_SHORT).show()
        })
        root.addView(TextView(this).apply {
            text = "동작 방식\n• 화면을 로컬에서 분석합니다.\n• 현재 상태를 추정하고 회피/이동/공격 행동을 선택합니다.\n• 정책 점수는 기기 내부에 저장되어 반복 사용에 따라 조정됩니다.\n• 네트워크 패킷, 계정 비밀번호, 인증 토큰을 수집하지 않습니다.\n• 자동화 동작은 Brawl Stars가 전면에 있을 때만 수행합니다."
            textSize = 13f; setTextColor(0xFF667085.toInt()); setPadding(0, 24, 0, 0)
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
        val game = if (packageManager.getLaunchIntentForPackage(RbxAccessibilityService.ROBLOX_PACKAGE) != null) "Brawl Stars 설치됨" else "Brawl Stars 미설치"
        val running = if (MacroPrefs.isRunning(this)) "AI 실행 중" else "AI 정지"
        status.text = "$accessibility  ·  $game\n$running"
        intervalLabel.text = "화면 분석 간격 · ${MacroPrefs.intervalMs(this)}ms"
    }

    private fun launchGame() {
        val launch = packageManager.getLaunchIntentForPackage(RbxAccessibilityService.ROBLOX_PACKAGE)
        if (launch != null) startActivity(launch) else Toast.makeText(this, "Brawl Stars가 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show()
    }
    private fun isAccessibilityEnabled(): Boolean = RbxAccessibilityService.isConnected
    private fun openAccessibilitySettings() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") })
            else Toast.makeText(this, "이미 배터리 최적화 제외 상태입니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
