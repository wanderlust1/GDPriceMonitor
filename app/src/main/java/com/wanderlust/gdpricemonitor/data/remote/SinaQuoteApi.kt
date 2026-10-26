package com.wanderlust.gdpricemonitor.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 新浪行情接口客户端。
 *
 * 接口返回非 JSON，形如 `var hq_str_hf_XAU="a,b,c,...";`。
 * 必须携带 Referer / User-Agent，否则返回空串（已验证）。
 */
class SinaQuoteApi {

    private val TAG = "SinaQuoteApi"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Referer", "https://finance.sina.com.cn")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** 一次性拉取多个品种的原文，symbols 用逗号拼接。 */
    fun fetchRaw(symbols: List<String>): String {
        val url = "https://hq.sinajs.cn/list=" + symbols.joinToString(",")
        Log.d(TAG, "fetchRaw url=$url")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code}")
            }
            val bytes = response.body?.bytes() ?: ByteArray(0)
            // 新浪行情接口返回 GBK 编码，按字节读取后以 GBK 解码（默认 string() 按 UTF-8 会乱码）
            val raw = String(bytes, Charsets.ISO_8859_1)
            Log.d(TAG, "fetchRaw response code=${response.code} raw=\n$raw")
            return raw
        }
    }
}
