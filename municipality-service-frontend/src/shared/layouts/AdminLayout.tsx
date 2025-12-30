//src/shared/layouts/AdminLayout.tsx
import { Box, Button, Card, CardContent, Stack, Typography } from "@mui/material";
import { Link as RouterLink, Outlet, useLocation } from "react-router-dom";
import Page from "../components/Page";

function NavBtn({ to, label, }: { to: string; label: string}) {
    const loc = useLocation();
    const active = loc.pathname.startsWith(to);
    return (
        <Button
            component={RouterLink}
            to={to}
            variant={active ? "contained" : "text"}
            fullWidth
            sx={{ justifyContent: "flex-start" }}
            data-testid={`nav-${label}`}
        >
            {label}
        </Button>
    );
}

export default function AdminLayout() {
    return (
        <Page>
            <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
                <Card sx={{ width: { xs: "100%", md: 260 }, height: "fit-content" }}>
                    <CardContent>
                        <Typography fontWeight={900} sx={{ mb: 1 }}>Admin Panel</Typography>
                        <Stack spacing={1}>
                            <NavBtn to="/admin/categories" label="Kategoriler" />
                            <NavBtn to="/admin/departments" label="Departmanlar" />
                        </Stack>
                    </CardContent>
                </Card>

                <Box sx={{ flex: 1 }}>
                    <Outlet />
                </Box>
            </Stack>
        </Page>
    );
}
