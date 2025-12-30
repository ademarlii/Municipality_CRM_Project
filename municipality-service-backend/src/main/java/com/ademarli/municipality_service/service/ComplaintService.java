package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.complaint.*;
import com.ademarli.municipality_service.model.entity.*;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintCategoryRepository categoryRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    private final NotificationRepository notificationRepository;
    private final DepartmentMemberRepository departmentMemberRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            ComplaintCategoryRepository categoryRepository,
                            StatusHistoryRepository statusHistoryRepository,
                            NotificationRepository notificationRepository,
                            DepartmentMemberRepository departmentMemberRepository) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.notificationRepository = notificationRepository;
        this.departmentMemberRepository = departmentMemberRepository;
    }

    private static final Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS = buildTransitions();



    @Transactional
    public ComplaintDetailResponse createComplaint(Long userId, CreateComplaintRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        ComplaintCategory cat = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND"));

        if (!cat.isActive()) {
            throw new BusinessException("CATEGORY_NOT_ACTIVE");
        }

        Department dept = cat.getDefaultDepartment();
        if (dept == null) {
            throw new BusinessException("CATEGORY_HAS_NO_DEFAULT_DEPARTMENT");
        }
        if (!dept.isActive()) {
            throw new BusinessException("DEFAULT_DEPARTMENT_NOT_ACTIVE");
        }

        Complaint c = new Complaint();
        c.setCreatedBy(user);
        c.setCategory(cat);
        c.setDepartment(dept);

        c.setTitle(req.getTitle() != null ? req.getTitle().trim() : null);
        c.setDescription(req.getDescription() != null ? req.getDescription().trim() : null);

        c.setTrackingCode(generateTrackingCodeUnique());
        c.setStatus(ComplaintStatus.NEW);
        c.setUpdatedAt(OffsetDateTime.now());
        if (req.getLat() != null) c.setLat(req.getLat());
        if (req.getLon() != null) c.setLon(req.getLon());

        complaintRepository.save(c);

        StatusHistory sh = new StatusHistory();
        sh.setComplaint(c);
        sh.setFromStatus(null);
        sh.setToStatus(ComplaintStatus.NEW);
        sh.setChangedBy(user);
        sh.setNote("Şikayetiniz alınmıştır. En kısa sürede incelenecektir.");
        statusHistoryRepository.save(sh);

        notifyCitizenForStatusChange(c, ComplaintStatus.NEW, sh.getNote(), null);

        return toDetailResponse(c);
    }

    // ------------------------------------------------------------
    // Citizen list / owner check
    // ------------------------------------------------------------
    public List<ComplaintSummaryResponse> listMyComplaints(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        return complaintRepository.findByCreatedBy(user)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public ComplaintDetailResponse getMyComplaintOrThrow(Long complaintId, Long userId) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("COMPLAINT_NOT_FOUND"));

        if (c.getCreatedBy() == null || !Objects.equals(c.getCreatedBy().getId(), userId)) {
            throw new AccessDeniedException("NOT_OWNER");
        }
        return toDetailResponse(c);
    }

    // ------------------------------------------------------------
    // Change status (staff/admin)
    // ------------------------------------------------------------
    @Transactional
    public void changeStatus(Long actorUserId,
                             Long complaintId,
                             ComplaintStatus toStatus,
                             String note,
                             String publicAnswer) {

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new NotFoundException("ACTOR_NOT_FOUND"));

        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("COMPLAINT_NOT_FOUND"));

        assertCanHandleComplaint(actor, c);

        if (c.getStatus() == ComplaintStatus.CLOSED) {
            throw new BusinessException("COMPLAINT_ALREADY_CLOSED");
        }

        ComplaintStatus from = c.getStatus();
        assertTransition(from, toStatus);

        if (publicAnswer != null && toStatus != ComplaintStatus.RESOLVED) {
            throw new BusinessException("PUBLIC_ANSWER_ONLY_ALLOWED_ON_RESOLVED");
        }

        if (toStatus == ComplaintStatus.RESOLVED) {
            if (publicAnswer == null || publicAnswer.isBlank()) {
                throw new BusinessException("PUBLIC_ANSWER_REQUIRED_ON_RESOLVED");
            }
            c.setPublicAnswer(publicAnswer.trim());
            c.setResolvedAt(OffsetDateTime.now());
        }

        if (toStatus == ComplaintStatus.CLOSED) {
            c.setClosedAt(OffsetDateTime.now());
        }

        c.setStatus(toStatus);
        c.setUpdatedAt(OffsetDateTime.now());
        complaintRepository.save(c);

        StatusHistory sh = new StatusHistory();
        sh.setComplaint(c);
        sh.setFromStatus(from);
        sh.setToStatus(toStatus);
        sh.setChangedBy(actor);
        sh.setNote(note);
        statusHistoryRepository.save(sh);
        notifyCitizenForStatusChange(c, toStatus, note, publicAnswer);
    }

    // ------------------------------------------------------------
    // Public tracking
    // ------------------------------------------------------------
    public Optional<PublicTrackingResponse> trackByCode(String trackingCode) {
        return complaintRepository.findByTrackingCode(trackingCode).map(c -> {
            PublicTrackingResponse r = new PublicTrackingResponse();
            r.setTrackingCode(c.getTrackingCode());
            r.setStatus(c.getStatus());
            r.setDepartmentName(c.getDepartment() != null ? c.getDepartment().getName() : null);
            return r;
        });
    }

    // ------------------------------------------------------------
    // Public feed pageable
    // ------------------------------------------------------------
    public Page<PublicFeedItem> publicFeed(Pageable pageable) {
        Page<ComplaintRepository.PublicFeedRow> page = complaintRepository.publicFeedResolvedWithRatings(pageable);

        return page.map(r -> {
            PublicFeedItem p = new PublicFeedItem();
            p.setId(r.getId());
            p.setTrackingCode(maskTrackingCode(r.getTrackingCode()));
            p.setTitle(r.getTitle());
            p.setCategoryName(r.getCategoryName());
            p.setDepartmentName(r.getDepartmentName());
            p.setStatus(r.getStatus());
            p.setAnsweredAt(r.getAnsweredAt());
            p.setPublicAnswer(r.getPublicAnswer());

            p.setAvgRating(r.getAvgRating() == null ? 0d : r.getAvgRating());
            p.setRatingCount(r.getRatingCount() == null ? 0L : r.getRatingCount());
            return p;
        });
    }


    private String maskTrackingCode(String code) {
        if (code == null) return null;
        if (code.length() <= 6) return "****";
        String start = code.substring(0, Math.min(3, code.length())); // TRK
        String end = code.substring(Math.max(0, code.length() - 2));
        return start + "****" + end;
    }



    // ------------------------------------------------------------
    // Mappers
    // ------------------------------------------------------------
    private ComplaintSummaryResponse toSummary(Complaint c) {
        ComplaintSummaryResponse r = new ComplaintSummaryResponse();
        r.setId(c.getId());
        r.setTrackingCode(c.getTrackingCode());
        r.setTitle(c.getTitle());
        r.setStatus(c.getStatus());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }

    private ComplaintDetailResponse toDetailResponse(Complaint c) {
        ComplaintDetailResponse r = new ComplaintDetailResponse();
        r.setId(c.getId());
        r.setTrackingCode(c.getTrackingCode());
        r.setTitle(c.getTitle());
        r.setDescription(c.getDescription());
        r.setStatus(c.getStatus());
        r.setCategoryId(c.getCategory() != null ? c.getCategory().getId() : null);
        r.setDepartmentId(c.getDepartment() != null ? c.getDepartment().getId() : null);
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }








    private static Map<ComplaintStatus, Set<ComplaintStatus>> buildTransitions() {
        Map<ComplaintStatus, Set<ComplaintStatus>> m = new EnumMap<>(ComplaintStatus.class);

        m.put(ComplaintStatus.NEW, EnumSet.of(ComplaintStatus.IN_REVIEW));

        m.put(ComplaintStatus.IN_REVIEW, EnumSet.of(
                ComplaintStatus.RESOLVED,
                ComplaintStatus.CLOSED
        ));

        m.put(ComplaintStatus.RESOLVED, EnumSet.of(ComplaintStatus.CLOSED));

        m.put(ComplaintStatus.CLOSED, EnumSet.noneOf(ComplaintStatus.class));

        return m;
    }

    private void assertTransition(ComplaintStatus from, ComplaintStatus to) {
        Set<ComplaintStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BusinessException("INVALID_STATUS_TRANSITION: " + from + " -> " + to);
        }
    }


    private boolean isStaff(User u) {
        return u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r == Role.ADMIN || r == Role.AGENT);
    }

    private boolean isAdmin(User u) {
        return u.getRoles() != null && u.getRoles().contains(Role.ADMIN);
    }

    private void assertCanHandleComplaint(User actor, Complaint c) {
        if (!isStaff(actor)) {
            throw new AccessDeniedException("ONLY_STAFF_CAN_CHANGE_STATUS");
        }

        if (isAdmin(actor)) return;

        Long deptId = (c.getDepartment() != null) ? c.getDepartment().getId() : null;
        if (deptId == null) throw new BusinessException("COMPLAINT_HAS_NO_DEPARTMENT");

        boolean member = departmentMemberRepository.existsByDepartmentIdAndUserIdAndActiveTrue(deptId, actor.getId());
        if (!member) throw new AccessDeniedException("NOT_A_MEMBER_OF_THIS_DEPARTMENT");
    }

    private void createNotification(Long userId, Long complaintId, String title, String body, String link) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setComplaintId(complaintId);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        notificationRepository.save(n);
    }



    private String generateTrackingCodeUnique() {
        for (int i = 0; i < 5; i++) {
            String code = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!complaintRepository.existsByTrackingCode(code)) return code;
        }
        throw new IllegalStateException("TRACKING_CODE_GENERATION_FAILED");
    }



    private void notifyCitizenForStatusChange(Complaint c, ComplaintStatus toStatus, String note, String publicAnswer) {
        if (c.getCreatedBy() == null) return;

        Long citizenId = c.getCreatedBy().getId();
        Long complaintId = c.getId();
        String link = "/complaints/" + complaintId;

        switch (toStatus) {
            case NEW -> createNotification(
                    citizenId, complaintId,
                    "Şikayetiniz alındı",
                    "Şikayetiniz bize ulaşmıştır. En kısa sürede incelenecektir.",
                    link
            );

            case IN_REVIEW -> createNotification(
                    citizenId, complaintId,
                    "Şikayetiniz incelenmeye alındı",
                    "Şikayetiniz ilgili birim tarafından incelenmeye alınmıştır. En kısa sürede dönüş yapılacaktır.",
                    link
            );

            case RESOLVED -> createNotification(
                    citizenId, complaintId,
                    "Şikayetiniz çözüldü",
                    (publicAnswer != null && !publicAnswer.isBlank())
                            ? publicAnswer.trim()
                            : "Şikayetiniz çözüldü.",
                    link
            );

            case CLOSED -> createNotification(
                    citizenId, complaintId,
                    "Şikayetiniz kapatıldı",
                    "Şikayetiniz kapatılmıştır. Sebep: " + ((note != null && !note.isBlank()) ? note.trim() : "-"),
                    link
            );
        }
    }
}
