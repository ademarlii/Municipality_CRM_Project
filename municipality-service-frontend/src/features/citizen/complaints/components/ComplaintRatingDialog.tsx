
//src/features/citizen/complaints/components/ComplaintRatingDialog.tsx
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Rating, Stack, Typography } from "@mui/material";
import { useEffect, useState } from "react";

type Props = {
    open: boolean;
    onClose: () => void;
    initialValue: number; // 0..5
    onSubmit: (value: number) => Promise<void>;
    complaintId: number;
};

export default function ComplaintRatingDialog({ open, onClose, initialValue, onSubmit, complaintId }: Props) {
    const [value, setValue] = useState<number>(initialValue);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        setValue(initialValue);
    }, [initialValue, open]);

    const save = async () => {
        setSaving(true);
        try {
            await onSubmit(value);
            onClose();
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={saving ? undefined : onClose} maxWidth="xs" fullWidth data-testid={`rating-dialog-${complaintId}`}>
            <DialogTitle>Puan Ver</DialogTitle>
            <DialogContent>
                <Stack spacing={1} sx={{ mt: 1 }}>
                    <Typography variant="body2" sx={{ opacity: 0.8 }}>
                        Bu şikayet için puanını seç.
                    </Typography>

                    <Rating
                        value={value}
                        onChange={(_, v) => setValue(v ?? 0)}
                        size="large"
                        data-testid={`rating-input-${complaintId}`}
                    />

                    <Typography variant="caption" sx={{ opacity: 0.7 }}>
                        Seçilen: {value}
                    </Typography>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={saving} data-testid={`rating-cancel-${complaintId}`}>Vazgeç</Button>
                <Button onClick={save} variant="contained" disabled={saving || value < 1} data-testid={`rating-save-${complaintId}`}>
                    Kaydet
                </Button>
            </DialogActions>
        </Dialog>
    );
}
