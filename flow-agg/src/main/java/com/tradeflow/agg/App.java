package com.tradeflow.agg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeflow.common.model.TradeEvent;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Properties;

/* === LEARN-BY-BUILDING ================================================
Why this file matters:
- It defines the Kafka Streams topology: parse -> groupBy(symbol) -> windowed aggregate -> JDBC upsert.
- Your choices here control correctness, latency, scalability.

What to tweak as you learn:
- Windowing: size/grace (e.g., 5s/1m/5m), event-time vs processing-time tradeoffs.
- State & semantics: WindowStore materialization, commit interval, caching.
- Failure handling: DLQ rules, idempotent upserts, backoff.

Safe to copy (rarely changes):
- Basic Streams bootstrap & serde wiring.
- The JDBC UPSERT template (keep ON CONFLICT for idempotency).

Mini TODOs:
- Try CACHE_MAX_BYTES_BUFFERING_CONFIG=10_000_000 and compare DB write cadence.
- Switch AUTO_OFFSET_RESET between earliest/latest and explain first-run behavior.
======================================================================= */
public class App {

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        String brokers = System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:19092");
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "flow-agg");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0); // flush each update
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000); // 1s commits
        // Optional: start from earliest for first run
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        StreamsBuilder builder = new StreamsBuilder();
        ObjectMapper om = new ObjectMapper();

        KStream<String, String> raw = builder.stream("trades.raw", Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, TradeEvent> events = raw
                .mapValues(v -> {
                    try {
                        return om.readValue(v, TradeEvent.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter((k, ev) -> ev != null);

        aggregateToTable(events, TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)), "1m");
        aggregateToTable(events, TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(5)), "5s");
        aggregateToTable(events, TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)), "5m");

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        streams.start();
        System.out.println("flow-agg started. Writing 1m aggregates to Postgres.");
    }

    static Connection connect() throws Exception {
        String url = "jdbc:postgresql://localhost:5432/" + System.getenv().getOrDefault("POSTGRES_DB", "flowdb");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "app");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "app");
        return DriverManager.getConnection(url, user, pass);
    }

    private static void aggregateToTable(KStream<String, TradeEvent> events, TimeWindows windows, String suffix) {
        KTable<Windowed<String>, AggState> aggregated = events
                .groupBy((k, ev) -> ev.symbol, Grouped.<String, TradeEvent>with(Serdes.String(), new TradeSerde()))
                .windowedBy(windows)
                .aggregate(
                        AggState::new,
                        (sym, ev, acc) -> acc.add(ev),
                        Materialized.<String, AggState, WindowStore<Bytes, byte[]>>as("agg-store-"+ suffix)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new AggStateSerde()));

        aggregated.toStream().foreach((windowedKey, state) -> {
            try (Connection conn = connect()) {
                String sql = "INSERT INTO trades_agg_"+suffix+"(window_start, symbol, trade_count, total_qty, buy_qty, sell_qty, vwap, block_trades, imbalance) "
                        + "VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (window_start, symbol) DO UPDATE SET "
                        + "trade_count=EXCLUDED.trade_count, total_qty=EXCLUDED.total_qty, buy_qty=EXCLUDED.buy_qty, "
                        + "sell_qty=EXCLUDED.sell_qty, vwap=EXCLUDED.vwap, block_trades=EXCLUDED.block_trades, imbalance=EXCLUDED.imbalance";
                PreparedStatement ps = conn.prepareStatement(sql);
                long windowStart = windowedKey.window().start();
                ps.setTimestamp(1, new Timestamp(windowStart));
                ps.setString(2, windowedKey.key());
                ps.setLong(3, state.tradeCount);
                ps.setLong(4, state.totalQty);
                ps.setLong(5, state.buyQty);
                ps.setLong(6, state.sellQty);
                ps.setDouble(7, state.vwap());
                ps.setLong(8, state.blockTrades);
                ps.setDouble(9, state.imbalance());
                ps.executeUpdate();
                System.out.printf("UPSERT %s @ %d -> trades=%d qty=%d buy=%d sell=%d vwap=%.4f block=%d imb=%.4f%n",
                        windowedKey.key(), windowedKey.window().start(), state.tradeCount, state.totalQty,
                        state.buyQty, state.sellQty, state.vwap(), state.blockTrades, state.imbalance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
