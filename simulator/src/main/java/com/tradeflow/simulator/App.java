package com.tradeflow.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.common.model.TradeEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class App {
    public static void main(String[] args) throws Exception {
        String brokers = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:19092");
        String topic = "trades.raw";
        List<String> symbols = List.of("AAPL", "MSFT", "NVDA", "AMD", "TSLA", "META");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ObjectMapper om = new ObjectMapper();
            Random rnd = new Random();
            System.out.println("Simulator producing to " + brokers + " topic " + topic + " ... (Ctrl+C to stop)");
            while (true) {
                String sym = symbols.get(rnd.nextInt(symbols.size()));
                String side = rnd.nextDouble() < 0.5 ? "buy" : "sell";
                long qty = (long) (rnd.nextDouble() < 0.05 ? rnd.nextInt(50000) + 20000 : rnd.nextInt(4000) + 100);
                double base;
                switch (sym) {
                    case "AAPL":
                        base = 200.0;
                        break;
                    case "MSFT":
                        base = 430.0;
                        break;
                    case "NVDA":
                        base = 950.0;
                        break;
                    case "AMD":
                        base = 170.0;
                        break;
                    case "TSLA":
                        base = 250.0;
                        break;
                    case "META":
                        base = 520.0;
                        break;
                    default:
                        base = 100.0;
                        break;
                }
                double price = base + rnd.nextGaussian() * (base * 0.002);
                boolean aggressor = rnd.nextBoolean();
                TradeEvent ev = new TradeEvent(Instant.now().toEpochMilli(), sym, side, qty, price, aggressor);
                String json = om.writeValueAsString(ev);
                producer.send(new ProducerRecord<>(topic, sym, json));
                Thread.sleep(50);
            }
        }
    }
}
