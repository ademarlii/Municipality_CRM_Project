import axios from "axios";

function joinFieldErrors(fieldErrors?: Record<string, string>) {
    if (!fieldErrors) return null;
    const parts = Object.entries(fieldErrors).map(([k, v]) => `${k}: ${v}`);
    return parts.length ? parts.join(" • ") : null;
}

export function getErrorMessage(err: any, fallback = "İşlem başarısız.") {
    if (axios.isAxiosError(err)) {
        const data: any = err.response?.data;

        if (typeof data === "string") return data || fallback;

        const fieldMsg = joinFieldErrors(data?.fieldErrors);
        return (
            fieldMsg ||
            data?.message ||
            data?.code ||
            err.response?.statusText ||
            fallback
        );
    }
    return err?.message || fallback;
}
