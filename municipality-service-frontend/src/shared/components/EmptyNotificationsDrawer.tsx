//src/shared/components/EmptyNotificationsDrawer.tsx
import { Badge, Box, Drawer, IconButton, Typography } from "@mui/material";
import NotificationsOutlinedIcon from "@mui/icons-material/NotificationsOutlined";
import { useState } from "react";

export default function EmptyNotificationsDrawer({ title = "Bildirimler" }: { title?: string }) {
    const [open, setOpen] = useState(false);

    return (
        <>
            <IconButton onClick={() => setOpen(true)} sx={{ ml: 1 }}>
                <Badge color="primary" variant="dot">
                    <NotificationsOutlinedIcon />
                </Badge>
            </IconButton>

            <Drawer anchor="right" open={open} onClose={() => setOpen(false)}>
                <Box sx={{ width: 360, p: 2 }}>
                    <Typography fontWeight={900} variant="h6">{title}</Typography>
                    <Typography sx={{ mt: 2 }} color="text.secondary">
                        Bildirim yok
                    </Typography>
                </Box>
            </Drawer>
        </>
    );
}
