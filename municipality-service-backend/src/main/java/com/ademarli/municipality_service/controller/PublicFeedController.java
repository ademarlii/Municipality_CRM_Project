package com.ademarli.municipality_service.controller;

import com.ademarli.municipality_service.model.dto.complaint.PublicFeedItem;
import com.ademarli.municipality_service.service.ComplaintService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicFeedController {

    private final ComplaintService complaintService;

    public PublicFeedController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<PublicFeedItem>> feed(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(complaintService.publicFeed(pageable));
    }
}
