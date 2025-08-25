package com.tradeflow.agg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.common.model.TradeEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/* === COPY-THIS BOILERPLATE ============================================
Why:
- Reusable JSON serde for TradeEvent. Low-change glue.

You may tweak:
- ObjectMapper settings (fail-fast on unknowns, timestamps).
Avoid:
- Expensive per-message allocations in hot paths.
======================================================================= */
public class TradeSerde implements Serde<TradeEvent> {
    ObjectMapper om = new ObjectMapper();

    @Override
    public Serializer<TradeEvent> serializer() {
        return new Serializer<TradeEvent>() {
            @Override
            public byte[] serialize(String topic, TradeEvent data) {
                try {
                    return om.writeValueAsBytes(data);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

    @Override
    public Deserializer<TradeEvent> deserializer() {
        return new Deserializer<TradeEvent>() {
            @Override
            public TradeEvent deserialize(String topic, byte[] data) {
                try {
                    return om.readValue(data, TradeEvent.class);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }
}
