import { decodeJwt, type JwtPayload } from "./jwt";

const TOKEN_KEY = "accessToken";

export type AuthResponse = {
    token?: string;
    accessToken?: string;
    tokenType?: string;
    expiresInSeconds?: number;
};

export function saveToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
}

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("tokenType");
    localStorage.removeItem("expiresInSeconds");
}

export function saveAuth(res: AuthResponse) {
    const token = res?.accessToken || res?.token;
    if (token) saveToken(token);
    if (res?.tokenType) localStorage.setItem("tokenType", res.tokenType);
    if (typeof res?.expiresInSeconds === "number") {
        localStorage.setItem("expiresInSeconds", String(res.expiresInSeconds));
    }
}

export function getSession(): { token: string | null; payload: JwtPayload | null } {
    const token = getToken();
    return { token, payload: token ? decodeJwt(token) : null };
}

export function isAuthenticated() {
    return !!getToken();
}

export function logout() {
    clearAuth();
    window.location.href = "/auth/login";
}
