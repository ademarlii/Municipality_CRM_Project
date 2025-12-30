package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.complaint.PublicTrackingResponse;
import com.ademarli.municipality_service.service.ComplaintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/complaints")
public class PublicTrackingController {

    private final ComplaintService complaintService;

    public PublicTrackingController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/track/{trackingCode}")
    public ResponseEntity<PublicTrackingResponse> track(@PathVariable String trackingCode) {
        return complaintService.trackByCode(trackingCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
