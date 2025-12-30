package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.model.dto.feedback.UpsertFeedbackRequest;
import com.ademarli.municipality_service.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/citizen/feedback")
public class CitizenFeedbackController {

    private final FeedbackService feedbackService;

    public CitizenFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PutMapping("/{complaintId}/rating")
    public ResponseEntity<FeedbackStatsResponse> upsertRating(@AuthenticationPrincipal UserDetails ud,
                                                              @PathVariable Long complaintId,
                                                              @RequestBody UpsertFeedbackRequest req) {
        Long citizenId = Long.parseLong(ud.getUsername());
        FeedbackStatsResponse res = feedbackService.upsertRating(citizenId, complaintId, req.getRating());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{complaintId}/rating/me")
    public ResponseEntity<Integer> myRating(@AuthenticationPrincipal UserDetails ud,
                                            @PathVariable Long complaintId) {
        Long citizenId = Long.parseLong(ud.getUsername());
        return ResponseEntity.ok(feedbackService.getMyRating(citizenId, complaintId));
    }

    @GetMapping("/{complaintId}/rating/stats")
    public ResponseEntity<FeedbackStatsResponse> myStats(@AuthenticationPrincipal UserDetails ud,
                                                         @PathVariable Long complaintId) {
        Long citizenId = Long.parseLong(ud.getUsername());
        return ResponseEntity.ok(feedbackService.getStats(complaintId, citizenId));
    }
}
