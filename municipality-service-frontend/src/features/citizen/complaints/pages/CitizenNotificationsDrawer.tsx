// src/features/citizen/complaints/pages/CitizenNotificationsDrawer.tsx
import { useEffect, useMemo, useState } from "react";
import {
    Drawer,
    Box,
    Typography,
    List,
    ListItemButton,
    ListItemText,
    Divider,
    Button,
    IconButton,
    Badge,
} from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import { useNavigate } from "react-router-dom";
import {
    getMyNotifications,
    getUnreadCount,
    markAllNotificationsRead,
    markNotificationRead,
} from "../api.ts";
import type { NotificationItem } from "../types";

function normalizeLink(n: NotificationItem): string {
    if (n.link && n.link.startsWith("/citizen/")) return n.link;
    if (n.link && n.link.startsWith("/complaints/")) return `/citizen${n.link}`;
    if (n.complaintId) return `/citizen/complaints/${n.complaintId}`;
    return "/citizen";
}

export default function CitizenNotificationsDrawer() {
    const nav = useNavigate();

    const [open, setOpen] = useState(false);
    const [items, setItems] = useState<NotificationItem[]>([]);
    const [unread, setUnread] = useState<number>(0);
    const [loading, setLoading] = useState(false);

    const unreadInList = useMemo(() => items.filter((x) => !x.isRead).length, [items]);

    const refreshUnread = async () => {
        const res = await getUnreadCount();
        setUnread(typeof res.data === "number" ? res.data : 0);
    };

    const load = async () => {
        setLoading(true);
        try {
            const res = await getMyNotifications(0, 30);
            setItems(res.data?.content ?? []);
            await refreshUnread();
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        refreshUnread().catch(() => {});
    }, []);

    const handleOpen = async () => {
        setOpen(true);
        await load();
    };

    const handleClickItem = async (n: NotificationItem) => {
        const target = normalizeLink(n);

        setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, isRead: true } : x)));
        setUnread((prev) => Math.max(0, prev - (n.isRead ? 0 : 1)));

        try {
            if (!n.isRead) await markNotificationRead(n.id);
        } catch {
            await load();
        }

        setOpen(false);
        nav(target);
    };

    const handleMarkAll = async () => {
        setItems((prev) => prev.map((x) => ({ ...x, isRead: true })));
        setUnread(0);

        try {
            await markAllNotificationsRead();
        } catch {
            await load();
        }
    };

    return (
        <>
            <IconButton
                onClick={handleOpen}
                aria-label="notifications"
                data-testid="citizen-notif-open"
            >
                <Badge
                    color="error"
                    data-testid="citizen-notif-badge"
                    // 🔥 TS-safe: badgeContent içine kendi span’imizi veriyoruz
                    badgeContent={
                        unread > 0 ? (
                            <span data-testid="citizen-notif-badge-content">{unread}</span>
                        ) : null
                    }
                >
                    <NotificationsIcon />
                </Badge>
            </IconButton>

            <Drawer
                anchor="right"
                open={open}
                onClose={() => setOpen(false)}
                data-testid="citizen-notif-drawer"
            >
                <Box sx={{ width: 360, p: 2 }}>
                    <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 1 }}>
                        <Typography variant="h6">Bildirimler</Typography>

                        <Button
                            onClick={handleMarkAll}
                            disabled={items.length === 0 || unreadInList === 0}
                            data-testid="citizen-notif-markAll"
                        >
                            Tümünü okundu yap
                        </Button>
                    </Box>

                    <Divider sx={{ mb: 1 }} />

                    {loading && <Typography variant="body2">Yükleniyor...</Typography>}

                    {!loading && items.length === 0 && (
                        <Typography variant="body2" data-testid="citizen-notif-empty">
                            Henüz bildirimin yok.
                        </Typography>
                    )}

                    <List dense data-testid="citizen-notif-list">
                        {items.map((n) => (
                            <ListItemButton
                                key={n.id}
                                onClick={() => handleClickItem(n)}
                                data-testid={`citizen-notif-item-${n.id}`}
                                sx={{
                                    borderRadius: 2,
                                    mb: 0.5,
                                    opacity: n.isRead ? 0.65 : 1,
                                }}
                            >
                                <ListItemText
                                    primary={
                                        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                                            <Typography fontWeight={n.isRead ? 400 : 700} data-testid={`citizen-notif-title-${n.id}`}>
                                                {n.title}
                                            </Typography>
                                            {!n.isRead && <Typography variant="caption">•</Typography>}
                                        </Box>
                                    }
                                    secondary={
                                        <>
                                            {n.body ? (
                                                <Typography variant="body2" data-testid={`citizen-notif-body-${n.id}`}>
                                                    {n.body}
                                                </Typography>
                                            ) : null}

                                            {n.complaintId ? (
                                                <Typography
                                                    variant="caption"
                                                    sx={{ display: "block", mt: 0.5 }}
                                                    data-testid={`citizen-notif-complaintId-${n.id}`}
                                                >
                                                    Şikayet ID: {n.complaintId}
                                                </Typography>
                                            ) : null}
                                        </>
                                    }
                                />
                            </ListItemButton>
                        ))}
                    </List>
                </Box>
            </Drawer>
        </>
    );
}
