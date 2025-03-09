package AlgoView_Server.global.analysis.dto;

public class AnalysisRequestDto {
    private String analysisId;
    private String historyJson;
    private String subscriptionsJson;

    // Getters and Setters
    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getHistoryJson() {
        return historyJson;
    }

    public void setHistoryJson(String historyJson) {
        this.historyJson = historyJson;
    }

    public String getSubscriptionsJson() {
        return subscriptionsJson;
    }

    public void setSubscriptionsJson(String subscriptionsJson) {
        this.subscriptionsJson = subscriptionsJson;
    }
}
