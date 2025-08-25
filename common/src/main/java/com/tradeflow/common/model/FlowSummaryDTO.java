package com.tradeflow.common.model;

/* === COPY-THIS BOILERPLATE ============================================
Plain DTO for API payloads. Keep lean; enrich via service logic.
======================================================================= */
public class FlowSummaryDTO {
    public String symbol;
    public String interval;
    public String flow_color;
    public java.util.List<String> evidence;
    public java.util.Map<String, Object> metrics;
    public double confidence;
}
