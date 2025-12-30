import { http } from "../../../shared/api/http";
import type { PageResponse } from "../../../shared/types/common";
import type {
    AddDepartmentMemberRequest,
    DepartmentMemberResponse,
    DepartmentResponse,
    DepartmentUpsertRequest
} from "./types";

export function listDepartments(page = 0, size = 10) {
    return http.get<PageResponse<DepartmentResponse>>("/api/admin/departments", { params: { page, size } });
}

export function createDepartment(req: DepartmentUpsertRequest) {
    return http.post<DepartmentResponse>("/api/admin/departments", req);
}

export function updateDepartment(id: number, req: DepartmentUpsertRequest) {
    return http.put<DepartmentResponse>(`/api/admin/departments/${id}`, req);
}

export function deleteDepartment(id: number) {
    return http.delete<void>(`/api/admin/departments/${id}`);
}

export function listMembers(deptId: number) {
    return http.get<DepartmentMemberResponse[]>(`/api/admin/departments/${deptId}/members`);
}

export function addMember(deptId: number, req: AddDepartmentMemberRequest) {
    return http.post<void>(`/api/admin/departments/${deptId}/members`, req);
}

export function removeMember(deptId: number, userId: number) {
    return http.delete<void>(`/api/admin/departments/${deptId}/members/${userId}`);
}

export function changeMemberRole(deptId: number, userId: number, role: string) {
    return http.patch<void>(`/api/admin/departments/${deptId}/members/${userId}/role/${role}`);
}
