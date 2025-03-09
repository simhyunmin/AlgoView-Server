package AlgoView_Server.global.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class DataAnalyzer {
    public JsonNode analyzeData(JsonNode history, JsonNode likes, JsonNode subscriptions) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("message", "Analysis complete");
        return result;
    }
}
