package com.tradeflow.common.model;

public class AggregatedInterval {
    public long windowStartEpochMillis;
    public String symbol;
    public long tradeCount;
    public long totalQty;
    public long buyQty;
    public long sellQty;
    public double vwap;
    public long blockTrades;
    public double imbalance;
}
