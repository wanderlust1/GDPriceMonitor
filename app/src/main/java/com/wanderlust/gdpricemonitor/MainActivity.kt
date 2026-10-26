package com.wanderlust.gdpricemonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.wanderlust.gdpricemonitor.databinding.ActivityMainBinding
import com.wanderlust.gdpricemonitor.ui.PriceViewModel
import com.wanderlust.gdpricemonitor.ui.QuoteAdapter
import com.wanderlust.gdpricemonitor.ui.SettingsActivity
import com.wanderlust.gdpricemonitor.ui.alert.PriceAlertChecker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PriceViewModel
    private val adapter = QuoteAdapter()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    // Android 13+ 通知权限申请（后台通知依赖该权限）
    private val requestPostNotification =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    // Android 12+ 精确闹钟权限申请（后台检测间隔 < 15 分钟依赖它；无权限时退化为非精确）
    private val requestExactAlarm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 状态栏统一为金色，与顶部工具栏一致；enableEdgeToEdge 会把状态栏设为透明，这里显式覆盖
        window.statusBarColor = ContextCompat.getColor(this, R.color.gold_primary)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[PriceViewModel::class.java]

        binding.recyclerQuotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerQuotes.adapter = adapter

        binding.btnRefresh.setOnClickListener { viewModel.refresh() }
        binding.fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Android 13+ 请求通知权限（低于该版本无需申请）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPostNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12+ 请求精确闹钟权限（后台检测 < 15 分钟需要；用户拒绝则退化为非精确）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SCHEDULE_EXACT_ALARM
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestExactAlarm.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.quotes)
                    binding.btnRefresh.isEnabled = !state.isLoading
                    binding.tvLastUpdated.text = if (state.lastUpdated == 0L) {
                        ""
                    } else {
                        getString(
                            R.string.hint_last_updated,
                            timeFmt.format(Date(state.lastUpdated))
                        )
                    }
                    if (state.error != null) {
                        binding.tvError.text = state.error
                        binding.tvError.visibility = View.VISIBLE
                    } else {
                        binding.tvError.visibility = View.GONE
                    }
                    // 前台轮询拿到行情后同步评估预警（App 打开时即时通知）
                    if (state.quotes.isNotEmpty()) {
                        PriceAlertChecker.evaluate(this@MainActivity, state.quotes)
                    }
                }
            }
        }

        // 前台自动轮询（固定 10 秒，提供 App 打开时的即时检测；后台间隔由设置页单独控制）
        viewModel.startPolling(10_000)
    }
}
