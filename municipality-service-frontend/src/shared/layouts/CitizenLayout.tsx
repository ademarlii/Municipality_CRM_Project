//src/shared/layouts/AgentLayout.tsx
import { Outlet } from "react-router-dom";
import Page from "../components/Page";

export default function CitizenLayout() {
    return (
        <Page>
            <Outlet />
        </Page>
    );
}
