package com.ademarli.municipality_service.model.dto.complaint;

import com.ademarli.municipality_service.model.enums.ComplaintStatus;

public class PublicTrackingResponse {
    private String trackingCode;
    private ComplaintStatus status;
    private String departmentName;

    public PublicTrackingResponse() {
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}

