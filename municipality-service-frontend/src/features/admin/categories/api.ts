import { http } from "../../../shared/api/http";
import type { PageResponse } from "../../../shared/types/common";
import type { CategoryResponse, CategoryUpsertRequest } from "./types";

export function listCategories(page = 0, size = 10) {
    return http.get<PageResponse<CategoryResponse>>("/api/admin/categories", { params: { page, size } });
}

export function createCategory(req: CategoryUpsertRequest) {
    return http.post<CategoryResponse>("/api/admin/categories", req);
}

export function updateCategory(id: number, req: CategoryUpsertRequest) {
    return http.put<CategoryResponse>(`/api/admin/categories/${id}`, req);
}

export function deleteCategory(id: number) {
    return http.delete<void>(`/api/admin/categories/${id}`);
}
