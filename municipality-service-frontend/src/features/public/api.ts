// src/features/public/api.ts
import { http } from "../../shared/api/http";
import type { PageResponse } from "../../shared/types/common";

export type PublicFeedItem = {
    id: number;
    trackingCode: string;
    title: string;
    status: string;
    categoryName?: string;
    departmentName?: string;
    publicAnswer?: string;
    answeredAt?: string;

    avgRating: number;
    ratingCount: number;
    myRating?: number | null;
};

export type PublicTrackingResponse = {
    trackingCode: string;
    status: string;
    departmentName?: string;
};

export function getPublicFeed(page = 0, size = 20) {
    return http.get<PageResponse<PublicFeedItem>>("/api/public/feed", {
        params: { page, size },
    });
}

export function trackComplaint(code: string) {
    return http.get<PublicTrackingResponse>(
        `/api/public/complaints/track/${encodeURIComponent(code)}`
    );
}
