package com.ademarli.municipality_service.model.dto.complaint;

import jakarta.validation.constraints.NotBlank;

public class ChangeStatusRequest {

    @NotBlank
    private String toStatus;

    private String note;

    private String publicAnswer;

    public ChangeStatusRequest() {
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPublicAnswer() {
        return publicAnswer;
    }

    public void setPublicAnswer(String publicAnswer) {
        this.publicAnswer = publicAnswer;
    }
}
