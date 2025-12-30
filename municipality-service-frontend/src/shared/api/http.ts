import axios from "axios";
import { clearAuth, getToken } from "../auth/authStore";

export const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
});

http.interceptors.request.use((config) => {
    const token = getToken();
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

http.interceptors.response.use(
    (res) => res,
    (err) => {
        const status = err.response?.status;
        const url: string = err.config?.url || "";

        const isAuthEndpoint =
            url.includes("/api/auth/login") || url.includes("/api/auth/register");

        if (status === 401 && !isAuthEndpoint) {
            clearAuth();
            window.location.href = "/auth/login";
        }

        return Promise.reject(err);
    }
);
