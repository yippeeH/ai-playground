package com.tradeflow.api.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowSummaryServiceTest {
    @Test
    void summarize_neutral_when_no_rows() throws Exception {
        var jdbc = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbc.queryForList(Mockito.anyString(), (Object[]) Mockito.any()))
                .thenReturn(List.of()); // no data
        var svc = new FlowSummaryService(jdbc);
        var json = svc.summarize("AAPL", "1m");
        assertThat(json).contains("\"flow_color\":\"neutral\"");
    }

    @Test
    void summarize_uses_latest_row() throws Exception {
        var jdbc = Mockito.mock(JdbcTemplate.class);
        Mockito.when(jdbc.queryForList(Mockito.anyString(), (Object[]) Mockito.any()))
                .thenReturn(List.of(Map.of(
                        "imbalance", 0.12, "trade_count", 100L, "block_trades", 5L)));
        var svc = new FlowSummaryService(jdbc);
        var json = svc.summarize("AAPL", "1m");
        assertThat(json).contains("\"flow_color\":\"buy\"");
    }
}
