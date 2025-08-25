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

/* === COPY-THIS BOILERPLATE ============================================
Why:
- Thin HTTP veneer + schema validation.

You may tweak:
- Request params (@RequestParam vs @PathVariable), input validation.
Tip:
- Keep logic out of controllers; put it in services for testability.
======================================================================= */
@RestController
public class SummarizeController {
    private final FlowSummaryService service;
    private final ObjectMapper om = new ObjectMapper();
    private final JsonSchema schema;

    public SummarizeController(FlowSummaryService service) throws Exception {
        this.service = service;
        try (InputStream is = getClass().getResourceAsStream("/schema/flow_summary.schema.json")) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(is);
        }
    }

    @PostMapping("/summarize")
    public Object summarize(@RequestBody Map<String, Object> body) throws Exception {
        String symbol = ((String) body.getOrDefault("symbol", "AAPL")).toUpperCase();
        String interval = (String) body.getOrDefault("interval", "1m");
        String json = service.summarize(symbol, interval);
        Set<ValidationMessage> errors = schema.validate(om.readTree(json));
        if (!errors.isEmpty()) {
            return Map.of("error", "SchemaValidationError", "messages", errors.toString(), "raw", json);
        }
        return om.readTree(json);
    }
}
