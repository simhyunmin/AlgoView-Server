package AlgoView_Server.global.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class DataPreprocessor {
    public JsonNode preprocessHistory(JsonNode data) {
        return data;
    }
    public JsonNode preprocessLikes(JsonNode data) {
        return data;
    }
    public JsonNode preprocessSubscriptions(JsonNode data) {
        return data;
    }
}
