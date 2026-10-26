package com.wanderlust.gdpricemonitor.data.model

import android.view.ViewGroup

/**
 * 单个黄金品种的实时行情。
 *
 * @param symbol       新浪行情代码，如 hf_XAU / AU0
 * @param name         展示名称，如 "伦敦金 XAU/USD"
 * @param latestPrice 最新价
 * @param change       涨跌额（最新价 - 昨收）
 * @param changePercent 涨跌幅（%）
 * @param high         最高价
 * @param low          最低价
 * @param open         开盘价（今开）
 * @param prevClose    昨收价
 * @param updateTime   行情更新时间（HH:mm:ss）
 * @param isClosed     是否休市 / 暂无数据（如 COMEX 在非美盘时段返回空）
 */
data class Quote(
    val symbol: String,
    val name: String,
    val latestPrice: Double,
    val change: Double,
    val changePercent: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val prevClose: Double,
    val updateTime: String,
    val isClosed: Boolean
)
