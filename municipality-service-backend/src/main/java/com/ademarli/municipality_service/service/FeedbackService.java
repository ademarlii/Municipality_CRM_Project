package com.ademarli.municipality_service.service;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.feedback.FeedbackStatsResponse;
import com.ademarli.municipality_service.model.entity.Complaint;
import com.ademarli.municipality_service.model.entity.Feedback;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.ComplaintStatus;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.ComplaintRepository;
import com.ademarli.municipality_service.repository.FeedbackRepository;
import com.ademarli.municipality_service.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ComplaintRepository complaintRepository,
                           UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
    }

    private boolean isCitizen(User u) {
        return u.getRoles() != null && u.getRoles().contains(Role.CITIZEN);
    }

    private void validateRating(Integer rating) {
        if (rating == null) throw new BusinessException("RATING_REQUIRED");
        if (rating < 1 || rating > 5) throw new BusinessException("RATING_MUST_BE_BETWEEN_1_AND_5");
    }

    private void validateComplaintIsRateable(Complaint c) {
        if (c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.CLOSED) {
            throw new BusinessException("ONLY_RESOLVED_OR_CLOSED_CAN_BE_RATED");
        }
    }

    @Transactional
    public FeedbackStatsResponse upsertRating(Long citizenUserId, Long complaintId, Integer rating) {
        User citizen = userRepository.findById(citizenUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND"));

        if (!isCitizen(citizen)) {
            throw new AccessDeniedException("ONLY_CITIZEN_CAN_RATE");
        }

        validateRating(rating);

        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new NotFoundException("COMPLAINT_NOT_FOUND"));

        validateComplaintIsRateable(c);

        // ✅ FIX: nested id method
        Feedback fb = feedbackRepository.findByComplaint_IdAndCitizen_Id(complaintId, citizenUserId)
                .orElseGet(() -> {
                    Feedback f = new Feedback();
                    f.setComplaint(c);
                    f.setCitizen(citizen);
                    return f;
                });

        fb.setRating(rating);
        feedbackRepository.save(fb);

        return buildStatsResponse(complaintId, citizenUserId);
    }

    @Transactional(readOnly = true)
    public Integer getMyRating(Long citizenUserId, Long complaintId) {
        Optional<Feedback> fb = feedbackRepository.findByComplaint_IdAndCitizen_Id(complaintId, citizenUserId);
        return fb.map(Feedback::getRating).orElse(null);
    }

    @Transactional(readOnly = true)
    public FeedbackStatsResponse getStats(Long complaintId, Long citizenUserIdOrNull) {
        return buildStatsResponse(complaintId, citizenUserIdOrNull);
    }

    private FeedbackStatsResponse buildStatsResponse(Long complaintId, Long citizenUserIdOrNull) {
        FeedbackRepository.FeedbackStatsView stats = feedbackRepository.getStats(complaintId);

        double avg = (stats != null && stats.getAvgRating() != null) ? stats.getAvgRating() : 0.0;
        long cnt = (stats != null && stats.getRatingCount() != null) ? stats.getRatingCount() : 0L;

        FeedbackStatsResponse res = new FeedbackStatsResponse();
        res.setComplaintId(complaintId);
        res.setAvgRating(avg);          // ✅ DTO ismini aşağıda düzelttim
        res.setRatingCount(cnt);

        if (citizenUserIdOrNull != null) {
            res.setMyRating(getMyRating(citizenUserIdOrNull, complaintId));
        } else {
            res.setMyRating(null);
        }
        return res;
    }
}
