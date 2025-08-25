package com.tradeflow.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/* === LEARN-BY-BUILDING (LIGHT) =======================================
Why:
- Defines the event contract. Changes ripple through producer/streams/api.

What to tweak:
- Add fields you genuinely need (e.g., venue, orderId), then update serde/topology.
======================================================================= */
public class TradeEvent {
    @JsonProperty
    public long tsEpochMillis;
    @JsonProperty
    public String symbol;
    @JsonProperty
    public String side;
    @JsonProperty
    public long qty;
    @JsonProperty
    public double price;
    @JsonProperty
    public boolean aggressor;

    public TradeEvent() {
    }

    public TradeEvent(long tsEpochMillis, String symbol, String side, long qty, double price, boolean aggressor) {
        this.tsEpochMillis = tsEpochMillis;
        this.symbol = symbol;
        this.side = side;
        this.qty = qty;
        this.price = price;
        this.aggressor = aggressor;
    }
}
