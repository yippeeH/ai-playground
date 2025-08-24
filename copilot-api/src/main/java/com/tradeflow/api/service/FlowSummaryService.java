package com.tradeflow.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class FlowSummaryService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    public FlowSummaryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String summarize(String symbol, String interval) throws Exception {
        String sql = "SELECT window_start, trade_count, total_qty, buy_qty, sell_qty, vwap, block_trades, imbalance "
                + "FROM trades_agg_1m WHERE symbol = ? ORDER BY window_start DESC LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, symbol);
        if (rows.isEmpty()) {
            return om.writeValueAsString(Map.of(
                    "symbol", symbol,
                    "interval", interval,
                    "flow_color", "neutral",
                    "evidence", List.of("no_recent_data"),
                    "metrics", Map.of("imbalance", 0.0, "block_ratio", 0.0, "vwap_slippage_bps", 0.0),
                    "confidence", 0.3));
        }
        Map<String, Object> r = rows.get(0);

        double imbalance = ((Number) r.get("imbalance")).doubleValue();
        long tradeCount = ((Number) r.get("trade_count")).longValue();
        long block = ((Number) r.get("block_trades")).longValue();
        double blockRatio = tradeCount > 0 ? (double) block / (double) tradeCount : 0.0;

        String color = "neutral";
        if (imbalance > 0.05)
            color = "buy";
        else if (imbalance < -0.05)
            color = "sell";
        else if (Math.abs(imbalance) > 0.01)
            color = "mixed";

        String narrative = String.format("Latest 1m for %s: imbalance=%.3f, block_ratio=%.3f (trade_count=%d).",
                symbol, imbalance, blockRatio, tradeCount);

        Map<String, Object> payload = Map.of(
                "symbol", symbol,
                "interval", interval,
                "flow_color", color,
                "evidence", List.of(narrative),
                "metrics", Map.of(
                        "imbalance", imbalance,
                        "block_ratio", blockRatio,
                        "vwap_slippage_bps", 0.0),
                "confidence", Math.min(1.0, 0.5 + Math.min(0.4, Math.abs(imbalance))));

        return om.writeValueAsString(payload);
    }
}
