export type ComplaintStatus = "NEW" | "IN_REVIEW" | "RESOLVED" | "CLOSED" | string;

const MAP: Record<string, string> = {
    NEW: "Yeni",
    IN_REVIEW: "İnceleniyor",
    RESOLVED: "Çözüldü",
    CLOSED: "Kapandı",
};

export function formatComplaintStatus(status?: string | null): string {
    if (!status) return "-";
    return MAP[status] ?? status; // bilinmeyen olursa olduğu gibi göster
}

export function isResolved(status?: string | null): boolean {
    return status === "RESOLVED";
}
