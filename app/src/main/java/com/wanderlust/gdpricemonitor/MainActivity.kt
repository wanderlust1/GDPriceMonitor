package com.wanderlust.gdpricemonitor

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PriceViewModel
    private val adapter = QuoteAdapter()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

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
                }
            }
        }

        // 前台每 10 秒自动轮询；退到后台自动暂停
        viewModel.startPolling(10_000)
    }
}
