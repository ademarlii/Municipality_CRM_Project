//src
import { Outlet } from "react-router-dom";
import Page from "../components/Page";

export default function PublicLayout() {
    return (
        <Page>
            <Outlet />
        </Page>
    );
}
