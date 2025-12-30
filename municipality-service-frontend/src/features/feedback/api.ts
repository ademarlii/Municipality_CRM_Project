// src/features/feedback/feedbackApi.ts
import { http } from "../../shared/api/http";

export type FeedbackSummary = {
    complaintId: number;
    avgRating: number;
    ratingCount: number;
};

export type FeedbackStatsResponse = FeedbackSummary & {
    myRating?: number | null;
};

// ✅ Public "summary" (avg + count)
export function getFeedbackSummary(complaintId: number) {
    return http.get<FeedbackSummary>(
        `/api/public/complaints/${complaintId}/rating/stats`
    );
}

// ✅ Citizen: benim oyum
export function getMyRating(complaintId: number) {
    return http.get<number | null>(
        `/api/citizen/feedback/${complaintId}/rating/me`
    );
}

// ✅ Citizen: oy ver / güncelle (backend stats döndürür)
export function upsertRating(complaintId: number, rating: number) {
    return http.put<FeedbackStatsResponse>(
        `/api/citizen/feedback/${complaintId}/rating`,
        { rating }
    );
}

// (opsiyonel) Citizen: stats + myRating tek seferde
export function getMyFeedbackStats(complaintId: number) {
    return http.get<FeedbackStatsResponse>(
        `/api/citizen/feedback/${complaintId}/rating/stats`
    );
}
