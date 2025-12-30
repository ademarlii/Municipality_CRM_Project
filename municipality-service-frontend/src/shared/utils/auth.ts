export type AuthResponse = {
    accessToken: string;
    tokenType: string;
    expiresInSeconds: number;
};

export function saveAuth(auth: AuthResponse) {
    localStorage.setItem("accessToken", auth.accessToken);
    localStorage.setItem("tokenType", auth.tokenType);
    localStorage.setItem("expiresInSeconds", String(auth.expiresInSeconds));
}
