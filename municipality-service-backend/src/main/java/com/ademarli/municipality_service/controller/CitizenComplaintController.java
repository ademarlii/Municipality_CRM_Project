package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.complaint.ComplaintDetailResponse;
import com.ademarli.municipality_service.model.dto.complaint.ComplaintSummaryResponse;
import com.ademarli.municipality_service.model.dto.complaint.CreateComplaintRequest;
import com.ademarli.municipality_service.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/citizen/complaints")
public class CitizenComplaintController {

    private final ComplaintService complaintService;

    public CitizenComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    public ResponseEntity<ComplaintDetailResponse> create(@AuthenticationPrincipal UserDetails ud, @Valid @RequestBody CreateComplaintRequest req) {
        Long userId = Long.parseLong(ud.getUsername());
        ComplaintDetailResponse r = complaintService.createComplaint(userId, req);
        return ResponseEntity.ok(r);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ComplaintSummaryResponse>> myComplaints(@AuthenticationPrincipal UserDetails ud) {
        Long userId = Long.parseLong(ud.getUsername());
        List<ComplaintSummaryResponse> list = complaintService.listMyComplaints(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDetailResponse> getOne(@AuthenticationPrincipal UserDetails ud,
                                                          @PathVariable Long id) {
        Long userId = Long.parseLong(ud.getUsername());
        return ResponseEntity.ok(complaintService.getMyComplaintOrThrow(id, userId));
    }
}
