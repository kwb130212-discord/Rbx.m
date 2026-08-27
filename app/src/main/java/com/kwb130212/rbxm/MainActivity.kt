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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }
    private lateinit var status: TextView
    private lateinit var intervalLabel: TextView

    private val games = listOf(GameProfile("Brawl Stars", "com.supercell.brawlstars"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        requestNotificationPermission()
        refreshStatus()
    }

    override fun onResume() { super.onResume(); refreshStatus() }
    override fun onDestroy() { OverlayEditor.hide(); super.onDestroy() }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 44, 32, 32)
            setBackgroundColor(0xFFF7F8FA.toInt())
        }
        root.addView(TextView(this).apply { text = "Rbx.m AI"; textSize = 34f; setTypeface(null, android.graphics.Typeface.BOLD) })
        root.addView(TextView(this).apply {
            text = "Brawl Stars · AI Macro"
            textSize = 15f; setTextColor(0xFF667085.toInt()); setPadding(0, 4, 0, 20)
        })
        status = TextView(this).apply { textSize = 15f; setPadding(20, 18, 20, 18); setBackgroundColor(0xFFE9F7EF.toInt()) }
        root.addView(status)

        section(root, "🎮 매크로")
        root.addView(button("📍 클릭 아이콘 위치 설정") {
            if (!Settings.canDrawOverlays(this)) openOverlaySettings() else OverlayEditor.show(this)
        })
        root.addView(button("▶ 자동 실행") { startMacro() })
        root.addView(button("■ 정지") { stopMacro() })
        root.addView(button("🎮 Brawl Stars 실행") { launchGame() })

        section(root, "🤖 오토팜")
        val farmSwitch = Switch(this).apply {
            text = "오토팜 활성화"
            textSize = 16f
            isChecked = AutoFarmPrefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> AutoFarmPrefs.enabled(this@MainActivity, checked) }
        }
        root.addView(farmSwitch)
        root.addView(TextView(this).apply { text = "모드"; textSize = 14f; setPadding(0, 12, 0, 4) })
        val modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, AutoFarmPrefs.Mode.values().map { it.label })
            setSelection(AutoFarmPrefs.Mode.values().indexOf(AutoFarmPrefs.mode(this@MainActivity)))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    AutoFarmPrefs.mode(this@MainActivity, AutoFarmPrefs.Mode.values()[position])
                }
            }
        }
        root.addView(modeSpinner)
        root.addView(TextView(this).apply { text = "AI 판단 주기"; textSize = 14f; setPadding(0, 12, 0, 2) })
        root.addView(SeekBar(this).apply {
            max = 19
            progress = ((AutoFarmPrefs.intervalMs(this@MainActivity) - 500L) / 500L).toInt().coerceIn(0, 19)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) AutoFarmPrefs.intervalMs(this@MainActivity, (value + 1) * 500L) }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })
        root.addView(Switch(this).apply {
            text = "화면이 꺼져도 서비스 유지 시도"
            isChecked = AutoFarmPrefs.keepAwake(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> AutoFarmPrefs.keepAwake(this@MainActivity, checked) }
        })
        root.addView(TextView(this).apply {
            text = "※ Android는 화면이 꺼진 상태에서 다른 앱의 화면 캡처/터치 입력을 제한할 수 있습니다. 이 옵션은 서비스와 CPU 유지용이며, 게임이 실제로 백그라운드에서 계속 렌더링/입력을 허용한다는 보장은 없습니다."
            textSize = 12f; setTextColor(0xFF667085.toInt()); setPadding(0, 8, 0, 0)
        })

        section(root, "⚙ 권한")
        root.addView(button("🪟 다른 앱 위에 표시") { openOverlaySettings() })
        root.addView(button("🔐 접근성 서비스") { openAccessibilitySettings() })
        root.addView(button("🔋 배터리 최적화") { openBatterySettings() })
        return root
    }

    private fun section(root: LinearLayout, title: String) {
        root.addView(TextView(this).apply { text = title; textSize = 20f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, 24, 0, 8) })
    }

    private fun button(textValue: String, action: () -> Unit): Button = Button(this).apply {
        text = textValue; textSize = 15f; gravity = Gravity.CENTER; setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, 56).apply { topMargin = 8 }
    }

    private fun startMacro() {
        if (!isAccessibilityEnabled()) { Toast.makeText(this, "접근성 서비스를 먼저 허용하세요.", Toast.LENGTH_LONG).show(); openAccessibilitySettings(); return }
        if (!Settings.canDrawOverlays(this)) { Toast.makeText(this, "오버레이 권한을 먼저 허용하세요.", Toast.LENGTH_LONG).show(); openOverlaySettings(); return }
        ContextCompat.startForegroundService(this, Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_START))
        refreshStatus()
    }

    private fun stopMacro() {
        startService(Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_STOP))
        refreshStatus()
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        val accessibility = if (isAccessibilityEnabled()) "접근성 연결됨" else "접근성 필요"
        val overlay = if (Settings.canDrawOverlays(this)) "오버레이 허용" else "오버레이 필요"
        val game = if (packageManager.getLaunchIntentForPackage("com.supercell.brawlstars") != null) "게임 설치됨" else "게임 미설치"
        val running = if (MacroPrefs.isRunning(this)) "실행 중" else "정지"
        val farm = if (AutoFarmPrefs.enabled(this)) "오토팜 ON · ${AutoFarmPrefs.mode(this).label}" else "오토팜 OFF"
        status.text = "$accessibility · $overlay\n$game · $running\n$farm"
    }

    private fun launchGame() { packageManager.getLaunchIntentForPackage("com.supercell.brawlstars")?.let { startActivity(it) } ?: Toast.makeText(this, "Brawl Stars가 설치되어 있지 않습니다.", Toast.LENGTH_LONG).show() }
    private fun isAccessibilityEnabled() = RbxAccessibilityService.isConnected
    private fun openAccessibilitySettings() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    private fun openOverlaySettings() = startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
    }
    private fun requestNotificationPermission() { if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

data class GameProfile(val name: String, val packageName: String)
