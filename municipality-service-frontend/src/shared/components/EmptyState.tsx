//src/shared/components/EmptyState.tsx
import { Box, Typography } from "@mui/material";

export default function EmptyState({ title, desc }: { title: string; desc?: string }) {
    return (
        <Box sx={{ py: 5, textAlign: "center", bgcolor: "white", border: "1px solid #e5e7eb", borderRadius: 3 }}>
            <Typography variant="h6" fontWeight={900}>{title}</Typography>
            {desc && <Typography sx={{ opacity: 0.75, mt: 1 }}>{desc}</Typography>}
        </Box>
    );
}
