package com.bigbang_tournaments.model;

public class TournamentRuleViolation {
    private TournamentRuleViolationType type;
    private String message;
    private String detail;

    public TournamentRuleViolation() {
    }

    public TournamentRuleViolation(TournamentRuleViolationType type, String message, String detail) {
        this.type = type;
        this.message = message;
        this.detail = detail;
    }

    public TournamentRuleViolationType getType() {
        return type;
    }

    public void setType(TournamentRuleViolationType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
