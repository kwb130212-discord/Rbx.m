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
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { advanceSetup() }
    private lateinit var status: TextView
    private var setupStarted = false
    private val games = listOf(GameProfile("Brawl Stars", "com.supercell.brawlstars"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        if (savedInstanceState == null) {
            setupStarted = true
            advanceSetup()
        }
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        if (setupStarted) advanceSetup()
    }

    override fun onDestroy() { OverlayEditor.hide(); super.onDestroy() }

    /** Opens required Android settings one at a time and continues after returning. */
    private fun advanceSetup() {
        if (!setupStarted) return
        when {
            Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            !Settings.canDrawOverlays(this) ->
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            !isAccessibilityEnabled() ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName) ->
                runCatching { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) }
                    .onFailure { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            else -> setupStarted = false
        }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 38, 28, 36)
            setBackgroundColor(0xFFF6F7FB.toInt())
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "dino-brawl-tool"
            textSize = 30f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Automation Center"
            textSize = 15f
            setTextColor(0xFF667085.toInt())
            setPadding(0, 2, 0, 18)
        })
        status = TextView(this).apply {
            textSize = 14f
            setPadding(20, 18, 20, 18)
            setBackgroundColor(0xFFE9F7EF.toInt())
        }
        root.addView(status)

        section(root, "🎮 게임")
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, games.map { it.name })
        })
        root.addView(button("▶ Brawl Stars 열기") { launchGame() })

        section(root, "⚡ 오토메이션")
        root.addView(switch("오토팜 활성화", AutoFarmPrefs.enabled(this)) { AutoFarmPrefs.enabled(this, it) })
        root.addView(TextView(this).apply { text = "동작 모드"; textSize = 13f; setPadding(0, 10, 0, 3) })
        root.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, AutoFarmPrefs.Mode.values().map { it.label })
            setSelection(AutoFarmPrefs.Mode.values().indexOf(AutoFarmPrefs.mode(this@MainActivity)))
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    AutoFarmPrefs.mode(this@MainActivity, AutoFarmPrefs.Mode.values()[position])
                }
            }
        })
        root.addView(TextView(this).apply { text = "판단 주기: ${AutoFarmPrefs.intervalMs(this@MainActivity)} ms"; textSize = 13f; setPadding(0, 10, 0, 2) })
        root.addView(SeekBar(this).apply {
            max = 19
            progress = ((AutoFarmPrefs.intervalMs(this@MainActivity) - 500L) / 500L).toInt().coerceIn(0, 19)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) AutoFarmPrefs.intervalMs(this@MainActivity, (value + 1) * 500L) }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })
        root.addView(button("▶ 자동화 시작") { startMacro() })
        root.addView(button("■ 자동화 정지") { stopMacro() })

        section(root, "📍 컨트롤 위치")
        root.addView(button("위치 편집기 열기") {
            if (!Settings.canDrawOverlays(this)) openOverlaySettings() else OverlayEditor.show(this)
        })

        section(root, "🧠 AI / 비전")
        root.addView(switch("화면 분석", true) { })
        root.addView(switch("학습 데이터 기록", true) { })
        root.addView(switch("화면 꺼짐 시 서비스 유지 시도", AutoFarmPrefs.keepAwake(this)) { AutoFarmPrefs.keepAwake(this, it) })

        section(root, "🔐 권한 및 안정성")
        root.addView(button("🪟 다른 앱 위에 표시") { openOverlaySettings() })
        root.addView(button("♿ 접근성 서비스") { openAccessibilitySettings() })
        root.addView(button("🔔 알림 권한") { requestNotificationPermission() })
        root.addView(button("🔋 배터리 최적화") { openBatterySettings() })
        root.addView(button("⚡ 전체 권한 다시 확인") { setupStarted = true; advanceSetup() })
        root.addView(TextView(this).apply {
            text = "처음 실행하면 필요한 권한을 순서대로 안내합니다. 갤럭시는 백그라운드 제한 설정에 따라 장시간 실행 여부가 달라질 수 있습니다."
            textSize = 12f
            setTextColor(0xFF667085.toInt())
            setPadding(0, 8, 0, 0)
        })
        return scroll
    }

    private fun section(root: LinearLayout, title: String) {
        root.addView(TextView(this).apply {
            text = title
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 24, 0, 8)
        })
    }

    private fun switch(label: String, checked: Boolean, action: (Boolean) -> Unit): Switch = Switch(this).apply {
        text = label
        textSize = 15f
        isChecked = checked
        setPadding(0, 5, 0, 5)
        setOnCheckedChangeListener { _, value -> action(value) }
    }

    private fun button(textValue: String, action: () -> Unit): Button = Button(this).apply {
        text = textValue
        textSize = 14f
        gravity = Gravity.CENTER
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, 54).apply { topMargin = 7 }
    }

    private fun startMacro() {
        if (!isAccessibilityEnabled()) { openAccessibilitySettings(); return }
        if (!Settings.canDrawOverlays(this)) { openOverlaySettings(); return }
        ContextCompat.startForegroundService(this, Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_START))
        refreshStatus()
    }

    private fun stopMacro() {
        startService(Intent(this, MacroForegroundService::class.java).setAction(MacroForegroundService.ACTION_STOP))
        refreshStatus()
    }

    private fun refreshStatus() {
        if (!::status.isInitialized) return
        val accessibility = if (isAccessibilityEnabled()) "접근성 ✓" else "접근성 필요"
        val overlay = if (Settings.canDrawOverlays(this)) "오버레이 ✓" else "오버레이 필요"
        val game = if (packageManager.getLaunchIntentForPackage(games[0].packageName) != null) "게임 ✓" else "게임 미설치"
        val running = if (MacroPrefs.isRunning(this)) "실행 중" else "정지"
        status.text = "$accessibility  ·  $overlay\n$game  ·  $running\n오토팜: ${if (AutoFarmPrefs.enabled(this)) AutoFarmPrefs.mode(this).label else "OFF"}"
    }

    private fun launchGame() {
        if (!Settings.canDrawOverlays(this) || !isAccessibilityEnabled()) {
            setupStarted = true
            advanceSetup()
            return
        }
        packageManager.getLaunchIntentForPackage(games[0].packageName)?.let { startActivity(it) }
    }

    private fun isAccessibilityEnabled() = RbxAccessibilityService.isConnected
    private fun openAccessibilitySettings() = startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    private fun openOverlaySettings() = startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                runCatching { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) }
                    .onFailure { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }
        }
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

data class GameProfile(val name: String, val packageName: String)
