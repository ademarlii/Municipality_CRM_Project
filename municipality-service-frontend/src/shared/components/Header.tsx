// ============================================
// src/shared/components/Header.tsx
// ============================================
import { AppBar, Avatar, Box, Button, Chip, Container, IconButton, Menu, MenuItem, Stack, Toolbar, Typography } from "@mui/material";
import { AccountCircle, AdminPanelSettings, Assignment, ExitToApp, Home, Login, PersonAdd } from "@mui/icons-material";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { useState } from "react";
import { getSession, logout } from "../auth/authStore";
import { hasRole } from "../auth/jwt";
import CitizenNotificationsDrawer from "../../features/citizen/complaints/pages/CitizenNotificationsDrawer";

export default function Header() {
    const nav = useNavigate();
    const { token, payload } = getSession();
    const isAdmin = hasRole(payload, "ADMIN");
    const isAgent = hasRole(payload, "AGENT");
    const isCitizen = hasRole(payload, "CITIZEN");

    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const menuOpen = Boolean(anchorEl);

    const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleLogout = () => {
        handleClose();
        logout();
        nav("/auth/login");
    };

    const userEmail = payload?.sub || payload?.email || "Kullanıcı";

    return (
        <AppBar
            position="sticky"
            elevation={0}
            sx={{
                bgcolor: "white",
                color: "text.primary",
                borderBottom: "2px solid #e5e7eb",
            }}
        >
            <Toolbar sx={{ py: 1 }}>
                <Container maxWidth="lg" sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                    {/* Logo */}
                    <Box
                        component={RouterLink}
                        to="/"
                        sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1.5,
                            textDecoration: "none",
                            color: "inherit",
                        }}
                    >
                        <Box
                            sx={{
                                width: 40,
                                height: 40,
                                borderRadius: 2,
                                bgcolor: "primary.main",
                                display: "flex",
                                alignItems: "center",
                                justifyContent: "center",
                            }}
                        >
                            <Home sx={{ color: "white", fontSize: 24 }} />
                        </Box>
                        <Stack spacing={-0.5}>
                            <Typography variant="subtitle1" fontWeight={900} sx={{ lineHeight: 1.2 }}>
                                Arnavutköy
                            </Typography>
                            <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                Belediyesi
                            </Typography>
                        </Stack>
                    </Box>

                    <Box sx={{ flex: 1 }} />

                    {/* Guest Menu */}
                    {!token && (
                        <Stack direction="row" spacing={1}>
                            <Button
                                component={RouterLink}
                                to="/auth/login"
                                variant="outlined"
                                startIcon={<Login />}
                                sx={{ borderRadius: 2 }}
                            >
                                Giriş
                            </Button>
                            <Button
                                component={RouterLink}
                                to="/auth/register"
                                variant="contained"
                                startIcon={<PersonAdd />}
                                sx={{ borderRadius: 2 }}
                            >
                                Kayıt Ol
                            </Button>
                        </Stack>
                    )}

                    {/* Logged In Menu */}
                    {token && (
                        <Stack direction="row" spacing={1} alignItems="center">
                            {/* Citizen Menu */}
                            {isCitizen && (
                                <>
                                    <Button
                                        component={RouterLink}
                                        to="/citizen/complaints"
                                        startIcon={<Assignment />}
                                        sx={{ borderRadius: 2 }}
                                    >
                                        Şikayetlerim
                                    </Button>
                                    <CitizenNotificationsDrawer />
                                </>
                            )}

                            {/* Agent Menu */}
                            {isAgent && (
                                <Chip
                                    label="Agent"
                                    color="warning"
                                    size="small"
                                    component={RouterLink}
                                    to="/agent/complaints"
                                    clickable
                                />
                            )}

                            {/* Admin Menu */}
                            {isAdmin && (
                                <Chip
                                    label="Admin"
                                    color="error"
                                    size="small"
                                    icon={<AdminPanelSettings />}
                                    component={RouterLink}
                                    to="/admin/categories"
                                    clickable
                                />
                            )}

                            {/* User Menu */}
                            <IconButton onClick={handleMenu} sx={{ ml: 1 }} id={"open-menu"}>
                                <Avatar sx={{ width: 36, height: 36, bgcolor: "primary.main" }}>
                                    <AccountCircle />
                                </Avatar>
                            </IconButton>

                            <Menu

                                anchorEl={anchorEl}
                                open={menuOpen}
                                onClose={handleClose}
                                anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
                                transformOrigin={{ vertical: "top", horizontal: "right" }}
                            >
                                <MenuItem disabled sx={{ opacity: 1 }}>
                                    <Stack>
                                        <Typography variant="body2" fontWeight={700}>
                                            {userEmail}
                                        </Typography>
                                        <Typography variant="caption" sx={{ opacity: 0.7 }}>
                                            {isAdmin ? "Admin" : isAgent ? "Agent" : "Vatandaş"}
                                        </Typography>
                                    </Stack>
                                </MenuItem>
                                <MenuItem onClick={handleLogout} className={"auth-logout"}>
                                    <ExitToApp sx={{ mr: 1 }} fontSize="small" />
                                    Çıkış Yap
                                </MenuItem>
                            </Menu>
                        </Stack>
                    )}
                </Container>
            </Toolbar>
        </AppBar>
    );
}