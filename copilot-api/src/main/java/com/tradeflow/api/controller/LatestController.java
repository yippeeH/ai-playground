package com.tradeflow.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.tradeflow.api.service.FlowSummaryService;
import org.springframework.web.bind.annotation.*;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

@RestController
public class LatestController {
    private final FlowSummaryService service;
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchema schema;

    public LatestController(FlowSummaryService service) throws Exception {
        this.service = service;
        try (InputStream is = getClass().getResourceAsStream("/schema/flow_summary.schema.json")) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(is);
        }
    }

    @GetMapping("/latest")
    public Object latest(@RequestParam(name = "symbol") String symbol,
            @RequestParam(name = "interval", defaultValue = "1m") String interval) throws Exception {
        symbol = symbol.toUpperCase();
        String json = service.summarize(symbol, interval);
        Set<ValidationMessage> errors = schema.validate(om.readTree(json));
        if (!errors.isEmpty()) {
            return Map.of("error", "SchemaValidationError", "messages", errors.toString(), "raw", json);
        }
        return om.readTree(json);
    }
}
