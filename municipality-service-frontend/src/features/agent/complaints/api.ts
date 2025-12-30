// src/features/agent/complaints/api.ts
import { http } from "../../../shared/api/http";

export type StatusKey = "NEW" | "IN_REVIEW" | "RESOLVED" | "CLOSED";

export type StaffComplaintListItem = {
    id: number;
    trackingCode: string;
    title: string;
    status: StatusKey;
    createdAt?: string;
    updatedAt?: string;
    categoryName?: string;
    departmentName?: string;
};

export type PageResponse<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number; // page index
    size: number;
};

export type ChangeStatusRequest = {
    toStatus: StatusKey;
    note?: string;
    publicAnswer?: string;
};

const API_BASE = "/api/agent/complaints";





export function listMyDepartmentComplaints(params?: {
    q?: string;
    status?: StatusKey;
    page?: number;
    size?: number;
}) {
    return http.get<PageResponse<StaffComplaintListItem>>(`${API_BASE}`, { params });
}


export function getComplaintDetail(id: number) {
    return http.get(`${API_BASE}/${id}`);
}

export function changeStatus(complaintId: number, req: ChangeStatusRequest) {
    return http.post<void>(`${API_BASE}/${complaintId}/status`, req);
}
