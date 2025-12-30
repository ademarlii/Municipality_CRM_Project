import { Navigate } from "react-router-dom";
import { getSession } from "../../../shared/auth/authStore";
import { hasRole } from "../../../shared/auth/jwt";

export default function HomeRedirect() {
    const { token, payload } = getSession();
    if (!token) return <Navigate to="/public/feed" replace />;

    if (hasRole(payload, "ADMIN")) return <Navigate to="/admin/categories" replace />;
    if (hasRole(payload, "AGENT")) return <Navigate to="/agent/complaints/update-status" replace />;
    return <Navigate to="/citizen/complaints" replace />;
}
