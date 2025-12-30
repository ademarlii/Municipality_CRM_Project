// ============================================
// 8. src/features/public/page/TrackComplaintPage.tsx
// ============================================
import { useState } from "react";
import { Button, Card, CardContent, Stack, TextField, Typography, Alert, Box, Paper, InputAdornment, CircularProgress } from "@mui/material";
import { Search, CheckCircle } from "@mui/icons-material";
import { useFormik } from "formik";
import { trackComplaint } from "../api";
import { useToast } from "../../../shared/utils/toast";
import { formatComplaintStatus } from "../../../shared/utils/complaintStatus";
import { trackComplaintValidationSchema, trackComplaintInitialValues, type TrackComplaintFormValues } from "../validation/trackValidation";

export default function TrackComplaintPage() {
    const toast = useToast();
    const [result, setResult] = useState<{
        trackingCode: string;
        status: string;
        departmentName?: string;
    } | null>(null);

    const formik = useFormik<TrackComplaintFormValues>({
        initialValues: trackComplaintInitialValues,
        validationSchema: trackComplaintValidationSchema,
        onSubmit: async (values) => {
            setResult(null);
            try {
                const res = await trackComplaint(values.trackingCode.trim());
                setResult(res.data);
            } catch (err: any) {
                if (err?.response?.status === 404) {
                    toast.error("Bu takip koduna ait kayıt bulunamadı.");
                } else {
                    toast.error("Sorgu başarısız.");
                }
            }
        },
    });

    return (
        <Box sx={{ py: 4, maxWidth: 700, mx: "auto" }}>
            <Stack spacing={4}>
                <Box textAlign="center">
                    <Typography variant="h4" fontWeight={900} gutterBottom>
                        Şikayet Takibi
                    </Typography>
                    <Typography variant="body1" sx={{ opacity: 0.7 }}>
                        Takip kodunu girerek şikayetinin durumunu öğrenebilirsin
                    </Typography>
                </Box>

                <Card elevation={0} sx={{ border: "2px solid", borderColor: "primary.main" }}>
                    <CardContent sx={{ p: 3 }}>
                        <form onSubmit={formik.handleSubmit}>
                            <Stack spacing={3}>
                                <TextField
                                    fullWidth
                                    name="trackingCode"
                                    label="Takip Kodunu Gir"
                                    placeholder="Örn: TRK-0347D186"
                                    value={formik.values.trackingCode}
                                    onChange={formik.handleChange}
                                    onBlur={formik.handleBlur}
                                    error={formik.touched.trackingCode && Boolean(formik.errors.trackingCode)}
                                    helperText={formik.touched.trackingCode && formik.errors.trackingCode}
                                    InputProps={{
                                        startAdornment: (
                                            <InputAdornment position="start">
                                                <Search />
                                            </InputAdornment>
                                        ),
                                    }}
                                    inputProps={{ "data-testid": "public-track-code" }}
                                />

                                <Button
                                    type="submit"
                                    variant="contained"
                                    size="large"
                                    fullWidth
                                    disabled={formik.isSubmitting || !formik.isValid}
                                    data-testid="public-track-submit"
                                    sx={{ py: 1.5 }}
                                >
                                    {formik.isSubmitting ? <CircularProgress size={24} color="inherit" /> : "Sorgula"}
                                </Button>
                            </Stack>
                        </form>
                    </CardContent>
                </Card>

                {result && (
                    <Paper elevation={0} sx={{ p: 4, border: "2px solid", borderColor: "success.main", bgcolor: "success.50" }} data-testid="public-track-result">
                        <Stack spacing={2} alignItems="center">
                            <CheckCircle sx={{ fontSize: 60, color: "success.main" }} />
                            <Typography variant="h6" fontWeight={900}>Şikayet Bulundu</Typography>

                            <Stack spacing={1} sx={{ width: "100%", mt: 2 }}>
                                <Card>
                                    <CardContent>
                                        <Typography variant="caption" sx={{ opacity: 0.7 }}>Takip Kodu</Typography>
                                        <Typography fontWeight={900} sx={{ fontFamily: "monospace", letterSpacing: 1 }} data-testid="public-track-result-tracking">
                                            {result.trackingCode}
                                        </Typography>
                                    </CardContent>
                                </Card>

                                <Card>
                                    <CardContent>
                                        <Typography variant="caption" sx={{ opacity: 0.7 }}>Durum</Typography>
                                        <Typography fontWeight={900} data-testid="public-track-result-status">
                                            {formatComplaintStatus(result.status)}
                                        </Typography>
                                    </CardContent>
                                </Card>

                                <Card>
                                    <CardContent>
                                        <Typography variant="caption" sx={{ opacity: 0.7 }}>Departman</Typography>
                                        <Typography fontWeight={900} data-testid="public-track-result-dept">
                                            {result.departmentName || "Belirlenmedi"}
                                        </Typography>
                                    </CardContent>
                                </Card>
                            </Stack>
                        </Stack>
                    </Paper>
                )}

                <Alert severity="info" sx={{ borderRadius: 2 }}>
                    <Typography variant="body2">
                        <strong>İpucu:</strong> Takip kodunu şikayetini oluşturduktan sonra aldın. E-posta veya SMS ile de gönderilmiş olabilir.
                    </Typography>
                </Alert>
            </Stack>
        </Box>
    );
}
