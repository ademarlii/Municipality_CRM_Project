package com.ademarli.municipality_service.model.dto.complaint;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public class AssignRequest {

    @NotNull
    private Long assignedToUserId;

    @Size(max = 500)
    private String note;

    public AssignRequest() {
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

