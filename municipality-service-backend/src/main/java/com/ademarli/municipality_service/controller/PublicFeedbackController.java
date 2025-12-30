package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/complaints")
public class PublicFeedbackController {

    private final FeedbackService feedbackService;

    public PublicFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/{complaintId}/rating/stats")
    public ResponseEntity<FeedbackStatsResponse> stats(@PathVariable Long complaintId) {
        return ResponseEntity.ok(feedbackService.getStats(complaintId, null));
    }
}
