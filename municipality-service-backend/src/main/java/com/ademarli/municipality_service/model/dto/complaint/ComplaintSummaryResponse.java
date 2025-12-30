package com.ademarli.municipality_service.model.dto.complaint;

import com.ademarli.municipality_service.model.enums.ComplaintStatus;

import java.time.OffsetDateTime;

public class ComplaintSummaryResponse {

    private Long id;
    private String trackingCode;
    private String title;
    private ComplaintStatus status;
    private OffsetDateTime createdAt;

    public ComplaintSummaryResponse() {
    }

    // getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

