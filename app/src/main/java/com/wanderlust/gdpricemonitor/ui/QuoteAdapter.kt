package com.wanderlust.gdpricemonitor.ui

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wanderlust.gdpricemonitor.R
import com.wanderlust.gdpricemonitor.data.model.Quote
import com.wanderlust.gdpricemonitor.databinding.ItemQuoteCardBinding

class QuoteAdapter : ListAdapter<Quote, QuoteAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Quote>() {
            override fun areItemsTheSame(a: Quote, b: Quote) = a.symbol == b.symbol
            override fun areContentsTheSame(a: Quote, b: Quote) = a == b
        }
    }

    inner class VH(val binding: ItemQuoteCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemQuoteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val q = getItem(position)
        val ctx: Context = holder.binding.root.context
        with(holder.binding) {
            tvName.text = q.name
            if (q.isClosed) {
                tvLatest.text = "暂无数据"
                tvLatest.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                tvChange.text = "休市 / 非交易时段"
                tvChange.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                tvHighLow.text = ""
                tvOpen.text = ""
                tvTime.text = ""
            } else {
                val up = q.change >= 0
                val color = ContextCompat.getColor(ctx, if (up) R.color.up_red else R.color.down_green)
                tvLatest.text = String.format("%.2f", q.latestPrice)
                tvLatest.setTextColor(color)
                val sign = if (up) "+" else ""
                tvChange.text = String.format(
                    "%s%.2f  (%s%.2f%%)",
                    sign, q.change, sign, q.changePercent
                )
                tvChange.setTextColor(color)
                tvHighLow.text = String.format("最高 %.2f · 最低 %.2f", q.high, q.low)
                tvOpen.text = String.format("今开 %.2f · 昨收 %.2f", q.open, q.prevClose)
                tvTime.text = if (q.updateTime.isNotEmpty()) "更新 ${q.updateTime}" else ""
            }
        }
    }
}