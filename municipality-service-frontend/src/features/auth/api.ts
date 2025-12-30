import { http } from "../../shared/api/http";
import type { AuthResponse } from "../../shared/auth/authStore";

export type RegisterRequest = { email: string; phone: string; password: string };
export type LoginRequest = { emailOrPhone: string; password: string };

export function login(req: LoginRequest) {
    return http.post<AuthResponse>("/api/auth/login", req);
}

export function register(req: RegisterRequest) {
    return http.post<AuthResponse>("/api/auth/register", req);
}
