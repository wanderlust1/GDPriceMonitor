package com.wanderlust.gdpricemonitor.data.remote

import com.wanderlust.gdpricemonitor.data.model.Quote

/**
 * 解析新浪行情文本为 [Quote] 列表。
 *
 * 字段语义（用户确认 + 原始数据核对）：
 * - 伦敦金(hf_XAU) 外汇贵金属格式：index0=最新价，index4=最高，index5=最低，index8=开盘；
 *   昨收取 index1（≈4105.20，index1 与 index7 同值），涨跌额 = 最新价 - 昨收。
 * - 沪金连续(nf_AU0) 期货格式：index6=最新价，index2=开盘，index3=最高，index4=最低，index10=昨收；
 *   该品种字段表无更新时间，统一沿用伦敦金(hf_XAU)的更新时间。
 */
object QuoteParser {

    private val LINE_RE = Regex("""var hq_str_(\w+)="([^"]*)";""")

    fun parse(raw: String, names: Map<String, String>): List<Quote> {
        val quotes = raw.lineSequence().mapNotNull { line ->
            val m = LINE_RE.find(line) ?: return@mapNotNull null
            val symbol = m.groupValues[1]
            val inner = m.groupValues[2]
            val name = names[symbol] ?: symbol
            if (inner.isBlank()) {
                // 休市 / 暂无数据
                Quote(symbol, name, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "", true)
            } else {
                parseQuote(symbol, name, inner.split(","))
            }
        }.toList()
        // 沪金连续(nf_AU0)字段无更新时间，沿用伦敦金(hf_XAU)的更新时间
        val refTime = quotes.firstOrNull { it.symbol == "hf_XAU" && it.updateTime.isNotEmpty() }?.updateTime
            ?: ""
        return quotes.map { q ->
            if (q.updateTime.isEmpty() && refTime.isNotEmpty()) q.copy(updateTime = refTime) else q
        }
    }

    private fun parseQuote(symbol: String, name: String, f: List<String>): Quote {
        return when (symbol) {
            "nf_AU0" -> parseShGold(f, name, symbol)
            else -> parseSpotGold(f, name, symbol) // hf_XAU 等外汇贵金属格式
        }
    }

    private fun safe(idx: Int, f: List<String>): Double =
        f.getOrNull(idx)?.toDoubleOrNull() ?: 0.0

    private fun parseSpotGold(f: List<String>, name: String, symbol: String): Quote {
        val latest = safe(0, f)       // 最新价
        val prevClose = safe(1, f)    // 昨收（≈4105.20；index1 与 index7 同值）
        val open = safe(8, f)         // 开盘价
        val change = latest - prevClose
        val pct = if (prevClose != 0.0) change / prevClose * 100 else 0.0
        val high = safe(4, f)         // 最高
        val low = safe(5, f)          // 最低
        val time = f.getOrNull(6) ?: ""
        return Quote(symbol, name, latest, change, pct, high, low, open, prevClose, time, false)
    }

    private fun parseShGold(f: List<String>, name: String, symbol: String): Quote {
        val latest = safe(6, f)       // 最新价
        val prevClose = safe(10, f)    // 昨收
        val open = safe(2, f)         // 开盘价
        val change = latest - prevClose
        val pct = if (prevClose != 0.0) change / prevClose * 100 else 0.0
        val high = safe(3, f)        // 最高
        val low = safe(4, f)          // 最低
        // 沪金连续字段表无更新时间，置空，由 parse() 统一回填伦敦金时间
        val time = ""
        return Quote(symbol, name, latest, change, pct, high, low, open, prevClose, time, false)
    }
}
