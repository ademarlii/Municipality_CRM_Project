//src/app/router.tsx
import { createBrowserRouter, Navigate } from "react-router-dom";
import PublicLayout from "../shared/layouts/PublicLayout";
import CitizenLayout from "../shared/layouts/CitizenLayout";
import AgentLayout from "../shared/layouts/AgentLayout";
import AdminLayout from "../shared/layouts/AdminLayout";
import RequireAuth from "../shared/auth/RequireAuth";

import NotFoundPage from "../features/misc/pages/NotFoundPage";
import ForbiddenPage from "../features/misc/pages/ForbiddenPage";

import LoginPage from "../features/auth/pages/LoginPage";
import RegisterPage from "../features/auth/pages/RegisterPage";

import PublicFeedPage from "../features/public/page/PublicFeedPage";
import TrackComplaintPage from "../features/public/page/TrackComplaintPage";

import MyComplaintsPage from "../features/citizen/complaints/pages/MyComplaintsPage";
import CreateComplaintPage from "../features/citizen/complaints/pages/CreateComplaintPage";
import ComplaintDetailPage from "../features/citizen/complaints/pages/ComplaintDetailPage";

import ComplaintListPage from "../features/agent/complaints/pages/ComplaintListPage";

import CategoryListPage from "../features/admin/categories/pages/CategoryListPage";
import DepartmentListPage from "../features/admin/departments/pages/DepartmentListPage";
import DepartmentMembersPage from "../features/admin/departments/pages/DepartmentMembersPage";

export const router = createBrowserRouter([
    { path: "/403", element: <ForbiddenPage /> },

    // ✅ PUBLIC: ana sayfa artık feed
    {
        element: <PublicLayout />,
        children: [
            { index: true, element: <PublicFeedPage /> }, // ✅ "/"
            { path: "auth/login", element: <LoginPage /> },
            { path: "auth/register", element: <RegisterPage /> },

            // (opsiyonel) eski linkler bozulmasın:
            { path: "public/feed", element: <Navigate to="/" replace /> },

            // ✅ Tracking artık public değil
            { path: "public/track", element: <Navigate to="/auth/login" replace /> },
        ],
    },

    // ✅ CITIZEN
    {
        path: "/citizen",
        element: (
            <RequireAuth roles={["CITIZEN"]}>
                <CitizenLayout />
            </RequireAuth>
        ),
        children: [
            { path: "complaints", element: <MyComplaintsPage /> },
            { path: "complaints/new", element: <CreateComplaintPage /> },
            { path: "complaints/:id", element: <ComplaintDetailPage /> },

            // ✅ Tracking login arkasında:
            { path: "track", element: <TrackComplaintPage /> },
        ],
    },

    // ✅ AGENT
    {
        path: "/agent",
        element: (
            <RequireAuth roles={["AGENT", "ADMIN"]}>
                <AgentLayout />
            </RequireAuth>
        ),
        children: [{ path: "complaints", element: <ComplaintListPage /> }],
    },

    // ✅ ADMIN
    {
        path: "/admin",
        element: (
            <RequireAuth roles={["ADMIN"]}>
                <AdminLayout />
            </RequireAuth>
        ),
        children: [
            { path: "categories", element: <CategoryListPage /> },
            { path: "departments", element: <DepartmentListPage /> },
            { path: "departments/:deptId/members", element: <DepartmentMembersPage /> },
        ],
    },

    { path: "*", element: <NotFoundPage /> },
]);
