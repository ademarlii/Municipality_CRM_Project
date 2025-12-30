//src/shared/components/Loading.tsx
import { Box, CircularProgress, Typography } from "@mui/material";

export default function Loading({ label = "Yükleniyor..." }: { label?: string }) {
    return (
        <Box sx={{ display: "grid", placeItems: "center", py: 6, gap: 2 }}>
            <CircularProgress />
            <Typography sx={{ opacity: 0.7 }}>{label}</Typography>
        </Box>
    );
}
