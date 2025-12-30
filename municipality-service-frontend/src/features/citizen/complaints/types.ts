// src/features/citizen/complaints/types.ts
import type { ComplaintStatus } from "../../../shared/utils/complaintStatus";

export type ComplaintSummary = {
    id: number;
    trackingCode: string;
    title: string;
    status: ComplaintStatus;
    createdAt?: string;
};

//label={item.departmentName || "Departman belirtilmedi"} size="small" />
//                                     <Chip icon={<Category />} label={item.categoryName ||
export type ComplaintDetail = {
    id: number;
    trackingCode: string;
    title: string;
    description?: string;
    status: ComplaintStatus;
    categoryId?: number | null;
    departmentId?: number | null;
    departmentName:string;
    categoryName:string;
    createdAt?: string;
};

export type CreateComplaintRequest = {
    categoryId: number;
    title: string;
    description?: string;
    lat?: number;
    lon?: number;
};

export type DepartmentItem = {
    id: number;
    name: string;
};

export type CategoryItem = {
    id: number;
    name: string;
};

export type NotificationItem = {
    id: number;
    complaintId?: number | null;
    title: string;
    body?: string | null;
    link?: string | null;
    isRead: boolean;
    createdAt: string;
};

export type PageResponse<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
};

export type FeedbackSummary = {
    complaintId: number;
    avgRating: number;
    ratingCount: number;
};

export type MyRating = {
    complaintId: number;
    rating: number | null; // hiç oy vermediyse null
};