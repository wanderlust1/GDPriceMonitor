package com.wanderlust.gdpricemonitor.data

import com.wanderlust.gdpricemonitor.data.model.Quote
import com.wanderlust.gdpricemonitor.data.remote.QuoteParser
import com.wanderlust.gdpricemonitor.data.remote.SinaQuoteApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 行情仓库：封装品种清单与网络请求，向 ViewModel 提供 [Quote] 列表。
 * 网络异常时返回空列表（容错，不向上抛崩溃）。
 */
class GoldPriceRepository {

    private val api = SinaQuoteApi()

    private val symbols = listOf("hf_XAU", "nf_AU0")

    private val names = mapOf(
        "hf_XAU" to "伦敦金 XAU/USD",
        "nf_AU0" to "沪金连续"
    )

    suspend fun getQuotes(): List<Quote> = withContext(Dispatchers.IO) {
        try {
            val raw = api.fetchRaw(symbols)
            QuoteParser.parse(raw, names)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
