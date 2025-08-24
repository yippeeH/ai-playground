package com.tradeflow.agg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class AggStateSerde implements Serde<AggState> {
    ObjectMapper om = new ObjectMapper();
    @Override public Serializer<AggState> serializer() {
        return new Serializer<AggState>() {
            @Override public byte[] serialize(String topic, AggState data) {
                try { return om.writeValueAsBytes(data); } catch (Exception e) { return null; }
            }
        };
    }
    @Override public Deserializer<AggState> deserializer() {
        return new Deserializer<AggState>() {
            @Override public AggState deserialize(String topic, byte[] data) {
                try { return om.readValue(data, AggState.class); } catch (Exception e) { return null; }
            }
        };
    }
}
