import { Navigate } from "react-router-dom";
import { getSession } from "./authStore";
import { hasRole } from "./jwt";
import type {JSX} from "react";

export default function RequireAuth({
                                        children,
                                        roles,
                                    }: {
    children: JSX.Element;
    roles?: string[];
}) {
    const { token, payload } = getSession();
    if (!token) return <Navigate to="/auth/login" replace />;

    if (roles?.length) {
        const ok = roles.some((r) => hasRole(payload, r));
        if (!ok) return <Navigate to="/403" replace />;
    }
    return children;
}
