package com.ademarli.municipality_service.model.dto.complaint;

import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicFeedItem {
    private Long id;
    private String trackingCode;
    private String title;
    private String categoryName;
    private String departmentName;
    private ComplaintStatus status;
    private OffsetDateTime answeredAt;
    private String publicAnswer;
    private Double avgRating;
    private Long ratingCount;

}

