package com.ademarli.municipality_service.model.dto.feedback;

public class FeedbackStatsResponse {
    private Long complaintId;
    private double avgRating;
    private Long ratingCount;
    private Integer myRating;

    public FeedbackStatsResponse() {}

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double averageRating) { this.avgRating = averageRating; }

    public Long getRatingCount() { return ratingCount; }
    public void setRatingCount(Long ratingCount) { this.ratingCount = ratingCount; }

    public Integer getMyRating() { return myRating; }
    public void setMyRating(Integer myRating) { this.myRating = myRating; }
}
