import { Stack, Typography } from "@mui/material";

export default function ForbiddenPage() {
    return (
        <Stack spacing={1}>
            <Typography variant="h4" fontWeight={950}>403 • Yetkisiz</Typography>
            <Typography sx={{ opacity: 0.8 }}>
                Bu sayfaya erişim yetkin yok.
            </Typography>
        </Stack>
    );
}
