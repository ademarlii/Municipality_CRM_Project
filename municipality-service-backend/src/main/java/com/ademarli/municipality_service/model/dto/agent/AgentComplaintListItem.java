package com.ademarli.municipality_service.model.dto.agent;

import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class AgentComplaintListItem {
    private Long id;
    private String trackingCode;
    private String title;
    private ComplaintStatus status;
    private OffsetDateTime createdAt;
    private String categoryName;
    private String departmentName;
    private String citizenEmail;

}

