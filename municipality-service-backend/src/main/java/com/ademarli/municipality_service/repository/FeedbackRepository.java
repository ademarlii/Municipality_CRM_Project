package com.ademarli.municipality_service.repository;

import com.ademarli.municipality_service.model.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByComplaint_IdAndCitizen_Id(Long complaintId, Long citizenId);

    boolean existsByComplaint_IdAndCitizen_Id(Long complaintId, Long citizenId);

    interface FeedbackStatsView {
        Double getAvgRating();
        Long getRatingCount();
    }

    @Query("""
        select 
          avg(f.rating) as avgRating,
          count(f) as ratingCount
        from Feedback f
        where f.complaint.id = :complaintId
    """)
    FeedbackStatsView getStats(@Param("complaintId") Long complaintId);
}
