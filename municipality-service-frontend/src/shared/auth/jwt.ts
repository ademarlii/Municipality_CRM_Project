export type JwtPayload = {
    sub?: string;
    roles?: string[];
    exp?: number;
    userId?: number;
    email?: string;
};

export function decodeJwt(token: string): JwtPayload | null {
    try {
        const base64 = token.split(".")[1];
        const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"));
        return JSON.parse(json);
    } catch {
        return null;
    }
}

export function hasRole(payload: JwtPayload | null, role: string) {
    return !!payload?.roles?.includes(role);
}
