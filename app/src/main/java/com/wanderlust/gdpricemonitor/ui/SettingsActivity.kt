package com.wanderlust.gdpricemonitor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.wanderlust.gdpricemonitor.R
import com.wanderlust.gdpricemonitor.data.GoldPriceRepository
import com.wanderlust.gdpricemonitor.data.alert.AlertRuleStore
import com.wanderlust.gdpricemonitor.databinding.ActivitySettingsBinding
import com.wanderlust.gdpricemonitor.ui.alert.NotificationSender
import com.wanderlust.gdpricemonitor.worker.PriceAlertScheduler

/**
 * 预警设置页：
 * - 全局：后台检测间隔（分钟，默认 5）。
 * - 每品种：跌破位(≤) 与 涨破位(≥) 阈值，留空=不监控。
 * 保存时校验输入，成功/失败均用 Toast 提示。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var store: AlertRuleStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        store = AlertRuleStore(this)

        // 后台检测间隔预填当前值（分钟）
        binding.etInterval.setText(store.getBackgroundIntervalMinutes().toString())

        val inflater = LayoutInflater.from(this)
        for ((symbol, name) in GoldPriceRepository().getMonitoredSymbols()) {
            val row = inflater.inflate(R.layout.item_alert_rule, binding.container, false)
            row.findViewById<TextView>(R.id.tvSymbolName).text = name
            val etBelow = row.findViewById<EditText>(R.id.etBelow)
            val etAbove = row.findViewById<EditText>(R.id.etAbove)
            val below = store.getBelow(symbol)
            val above = store.getAbove(symbol)
            below?.let { etBelow.setText(it.toString()) }
            above?.let { etAbove.setText(it.toString()) }
            row.findViewById<TextView>(R.id.tvCurrentRule).text = currentRuleText(below, above)
            row.tag = symbol
            binding.container.addView(row)
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnTestAlert.setOnClickListener { sendTestAlert() }
        binding.btnClearRules.setOnClickListener { clearRules() }
    }

    /** 当前已保存预警值的只读展示文案。 */
    private fun currentRuleText(below: Double?, above: Double?): String {
        val parts = mutableListOf<String>()
        below?.let { parts.add("跌破 ≤ ${fmt(it)}") }
        above?.let { parts.add("涨破 ≥ ${fmt(it)}") }
        return if (parts.isEmpty()) "当前：未设置（留空表示不监控）"
        else "当前：${parts.joinToString(" · ")}"
    }

    /** 4000.0 -> "4000"，4105.5 -> "4105.5"，去掉多余的 .0。 */
    private fun fmt(v: Double): String =
        if (v.toLong() == v.toLong()) v.toLong().toString() else v.toString()

    /** 清空所有品种的预警策略（阈值与穿越状态），并刷新界面显示。 */
    private fun clearRules() {
        for (i in 0 until binding.container.childCount) {
            val row = binding.container.getChildAt(i)
            val symbol = row.tag as? String ?: continue
            store.clearRule(symbol)
            row.findViewById<EditText>(R.id.etBelow).text.clear()
            row.findViewById<EditText>(R.id.etAbove).text.clear()
            row.findViewById<TextView>(R.id.tvCurrentRule).text = currentRuleText(null, null)
        }
        Toast.makeText(this, "已清空所有预警策略", Toast.LENGTH_SHORT).show()
    }

    /** 立即发送一条模拟的跌破阈值通知，用于验证通知链路（渠道/权限/样式）。 */
    private fun sendTestAlert() {
        // Android 13+ 需通知权限；未授权直接提示，避免"发了但看不到"的困惑
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "未授予通知权限，请在系统设置中开启后重试", Toast.LENGTH_SHORT).show()
            return
        }
        // 模拟数据：用伦敦金已保存的跌破位（未设置则用 4000），现价取跌破位下方 5 美元
        val symbol = "hf_XAU"
        val below = store.getBelow(symbol) ?: 4000.0
        val price = below - 5.0
        NotificationSender.send(
            this,
            "test_$symbol",
            "【测试】伦敦金 XAU/USD",
            String.format("现价 %.2f 已跌破 %.2f（模拟数据）", price, below)
        )
        Toast.makeText(this, "测试通知已发送", Toast.LENGTH_SHORT).show()
    }

    private fun save() {
        // 1) 后台检测间隔校验（分钟）
        val interval = binding.etInterval.text.toString().trim().toIntOrNull()
        if (interval == null || interval <= 0) {
            Toast.makeText(this, "后台检测间隔需为正整数（分钟）", Toast.LENGTH_SHORT).show()
            return
        }

        // 2) 各品种阈值校验：跌破位需 < 涨破位
        val rules = mutableListOf<Pair<String, Pair<Double?, Double?>>>()
        for (i in 0 until binding.container.childCount) {
            val row = binding.container.getChildAt(i)
            val symbol = row.tag as? String ?: continue
            val name = row.findViewById<TextView>(R.id.tvSymbolName).text.toString()
            val below = row.findViewById<EditText>(R.id.etBelow).text.toString().trim().toDoubleOrNull()
            val above = row.findViewById<EditText>(R.id.etAbove).text.toString().trim().toDoubleOrNull()
            if (below != null && above != null && below >= above) {
                Toast.makeText(this, "$name：跌破位需小于涨破位", Toast.LENGTH_SHORT).show()
                return
            }
            rules.add(symbol to (below to above))
        }

        // 3) 除检测间隔外未输入任何阈值：不允许保存
        if (rules.all { it.second.first == null && it.second.second == null }) {
            Toast.makeText(
                this,
                "未输入任何预警阈值，不能保存；如需清空请点「清空预警」",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 4) 写入并提示
        store.setBackgroundIntervalMinutes(interval)
        for ((symbol, belowAbove) in rules) {
            store.setBelow(symbol, belowAbove.first)
            store.setAbove(symbol, belowAbove.second)
            // 阈值被清掉时同步重置穿越状态，便于日后重新设置时正常触发
            if (belowAbove.first == null) store.setWasBelow(symbol, false)
            if (belowAbove.second == null) store.setWasAbove(symbol, false)
        }
        // 立即按新间隔重新排程后台检测
        PriceAlertScheduler.schedule(this, interval)
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        finish()
    }
}
