package com.ademarli.municipality_service.model.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class NotificationItemResponse {
    private Long id;
    private Long complaintId;
    private String title;
    private String body;
    private String link;
    @JsonProperty("isRead")
    private boolean isRead;
    private OffsetDateTime createdAt;
}
