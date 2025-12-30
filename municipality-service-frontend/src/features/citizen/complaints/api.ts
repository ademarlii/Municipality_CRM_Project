// src/features/citizen/complaints/api.ts
import { http } from "../../../shared/api/http";
import type {
    CategoryItem,
    ComplaintDetail,
    ComplaintSummary,
    CreateComplaintRequest,
    DepartmentItem,
    NotificationItem,
    PageResponse,
} from "./types";

export function createComplaint(req: CreateComplaintRequest) {
    return http.post<ComplaintDetail>("/api/citizen/complaints", req);
}

export function listMyComplaints() {
    return http.get<ComplaintSummary[]>("/api/citizen/complaints/my");
}

export function getMyComplaint(id: number) {
    return http.get<ComplaintDetail>(`/api/citizen/complaints/${id}`);
}

export async function getPublicDepartments(): Promise<DepartmentItem[]> {
    const res = await http.get("/api/public/departments");
    return Array.isArray(res.data) ? res.data : [];
}

export async function getPublicCategoriesByDepartment(
    deptId: number | ""
): Promise<CategoryItem[]> {
    const res = await http.get(`/api/public/departments/${deptId}/categories`);
    return Array.isArray(res.data) ? res.data : [];
}

// -------- Notifications --------
export function getMyNotifications(page = 0, size = 20) {
    return http.get<PageResponse<NotificationItem>>(
        `/api/citizen/notifications?page=${page}&size=${size}`
    );
}

export function getUnreadCount() {
    return http.get<number>(`/api/citizen/notifications/unread-count`);
}

export function markNotificationRead(id: number) {
    return http.patch<void>(`/api/citizen/notifications/${id}/read`);
}

export function markAllNotificationsRead() {
    return http.patch<number>(`/api/citizen/notifications/read-all`);
}
