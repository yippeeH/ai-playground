package com.tradeflow.agg;

import com.tradeflow.common.model.TradeEvent;

/* === LEARN-BY-BUILDING ================================================
Why:
- Encodes business math (count/qty/buy/sell/VWAP/block/imbalance) that drives signals.

What to tweak:
- Add new features (e.g., participation rate, median trade size).
- Guardrails for outliers (clip absurd qty/price).
- Precision vs performance (double vs BigDecimal for VWAP if needed).

Mini TODO:
- Add rolling max trade size and expose it to API/Grafana.
======================================================================= */
public class AggState {
    long tradeCount = 0;
    long totalQty = 0;
    long buyQty = 0;
    long sellQty = 0;
    double vwapNumerator = 0.0;
    long blockTrades = 0;

    public AggState add(TradeEvent ev) {
        tradeCount++;
        totalQty += ev.qty;
        if ("buy".equalsIgnoreCase(ev.side))
            buyQty += ev.qty;
        else
            sellQty += ev.qty;
        vwapNumerator += ev.price * ev.qty;
        if (ev.qty >= 20000)
            blockTrades++;
        return this;
    }

    public double vwap() {
        return totalQty > 0 ? vwapNumerator / totalQty : 0.0;
    }

    public double imbalance() {
        long net = buyQty - sellQty;
        return totalQty > 0 ? (double) net / (double) totalQty : 0.0;
    }
}
