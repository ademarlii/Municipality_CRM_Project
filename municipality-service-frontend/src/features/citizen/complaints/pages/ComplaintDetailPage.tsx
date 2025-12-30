// ============================================
// 9. src/features/citizen/complaints/pages/ComplaintDetailPage.tsx
// ============================================
import { useEffect, useState } from "react";
import { Button, Card, CardContent, Rating, Stack, Typography, Box, Chip, Divider } from "@mui/material";
import { AccessTime, Category, Business, Check } from "@mui/icons-material";
import { useParams } from "react-router-dom";
import Loading from "../../../../shared/components/Loading";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import { getMyComplaint } from "../api";
import type { ComplaintDetail, FeedbackSummary } from "../types";
import { formatComplaintStatus, isResolved } from "../../../../shared/utils/complaintStatus";
import ComplaintRatingDialog from "../components/ComplaintRatingDialog";
import { getFeedbackSummary, getMyRating, upsertRating } from "../../../feedback/api";

export default function ComplaintDetailPage() {
    const { id } = useParams();
    const toast = useToast();
    const [item, setItem] = useState<ComplaintDetail | null>(null);
    const [loading, setLoading] = useState(true);
    const [summary, setSummary] = useState<FeedbackSummary | null>(null);
    const [myRating, setMyRatingState] = useState<number | null>(null);
    const [rateOpen, setRateOpen] = useState(false);

    const load = async () => {
        const complaintId = Number(id);
        if (!complaintId) return;

        setLoading(true);
        try {
            const res = await getMyComplaint(complaintId);
            setItem(res.data);

            if (isResolved(res.data.status)) {
                const [s, r] = await Promise.all([
                    getFeedbackSummary(complaintId).catch(() => null),
                    getMyRating(complaintId).catch(() => null),
                ]);
                setSummary(s?.data ?? { complaintId, avgRating: 0, ratingCount: 0 });
                setMyRatingState(r ? r.data : null);
            } else {
                setSummary(null);
                setMyRatingState(null);
            }
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Detay yüklenemedi."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, [id]);

    const submitRate = async (value: number) => {
        if (!item) return;
        try {
            const res = await upsertRating(item.id, value);
            setSummary(res.data);
            setMyRatingState(res.data.myRating ?? value);
            toast.success("Puan kaydedildi.");
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Puan kaydedilemedi."));
        }
    };

    if (loading) return <Loading />;

    if (!item) {
        return (
            <Card><CardContent><Typography data-testid="citizen-complaint-detail-notfound">Şikayet bulunamadı.</Typography></CardContent></Card>
        );
    }

    return (
        <Box sx={{ py: 4, maxWidth: 900, mx: "auto" }} data-testid="citizen-complaint-detail">
            <Stack spacing={3}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h4" fontWeight={900} data-testid="citizen-complaint-detail-title">
                        Şikayet Detayı
                    </Typography>
                    <Chip label={formatComplaintStatus(item.status)} color="primary" />
                </Stack>

                <Card elevation={0} sx={{ border: "2px solid #e5e7eb" }} data-testid={`citizen-complaint-detail-card-${item.id}`}>
                    <CardContent sx={{ p: 3 }}>
                        <Stack spacing={3}>
                            <Box>
                                <Typography variant="h5" fontWeight={900} gutterBottom data-testid="detail-title">
                                    {item.title}
                                </Typography>
                                <Stack direction="row" spacing={2} alignItems="center" sx={{ mt: 1 }}>
                                    <Chip icon={<Business />} label={item.departmentName || "Departman belirtilmedi"} size="small" />
                                    <Chip icon={<Category />} label={item.categoryName || "Kategori belirtilmedi"} size="small" />
                                </Stack>
                            </Box>

                            <Divider />

                            <Box>
                                <Typography variant="caption" sx={{ opacity: 0.7 }}>Takip Kodu</Typography>
                                <Typography fontWeight={900} sx={{ fontFamily: "monospace", letterSpacing: 1 }} data-testid="detail-tracking">
                                    {item.trackingCode}
                                </Typography>
                            </Box>

                            {item.description && (
                                <>
                                    <Divider />
                                    <Box>
                                        <Typography variant="caption" sx={{ opacity: 0.7 }}>Açıklama</Typography>
                                        <Typography data-testid="detail-description">{item.description}</Typography>
                                    </Box>
                                </>
                            )}

                            {isResolved(item.status) && summary && (
                                <>
                                    <Divider />
                                    <Card variant="outlined" sx={{ bgcolor: "success.50", border: "2px solid", borderColor: "success.main" }}>
                                        <CardContent>
                                            <Stack spacing={2}>
                                                <Stack direction="row" alignItems="center" spacing={1}>
                                                    <Check color="success" />
                                                    <Typography variant="subtitle1" fontWeight={900}>Değerlendirme</Typography>
                                                </Stack>

                                                <Stack direction="row" alignItems="center" justifyContent="space-between" data-testid="detail-rating-block">
                                                    <Stack direction="row" alignItems="center" spacing={1}>
                                                        <Rating value={summary.avgRating ?? 0} precision={0.1} readOnly />
                                                        <Typography variant="body2" sx={{ opacity: 0.85 }}>
                                                            {(summary.avgRating ?? 0).toFixed(1)} ({summary.ratingCount ?? 0} değerlendirme)
                                                        </Typography>
                                                    </Stack>

                                                    <Button
                                                        size="small"
                                                        variant="contained"
                                                        onClick={() => setRateOpen(true)}
                                                        data-testid="detail-rate-btn"
                                                    >
                                                        {myRating ? "Puanı Güncelle" : "Puan Ver"}
                                                    </Button>
                                                </Stack>
                                            </Stack>
                                        </CardContent>
                                    </Card>
                                </>
                            )}

                            {item.createdAt && (
                                <Stack direction="row" alignItems="center" spacing={1} sx={{ opacity: 0.7 }}>
                                    <AccessTime fontSize="small" />
                                    <Typography variant="caption" data-testid="detail-createdAt">
                                        {new Date(item.createdAt).toLocaleString("tr-TR")}
                                    </Typography>
                                </Stack>
                            )}
                        </Stack>
                    </CardContent>
                </Card>
            </Stack>

            {item && isResolved(item.status) && (
                <ComplaintRatingDialog
                    open={rateOpen}
                    onClose={() => setRateOpen(false)}
                    initialValue={myRating ?? 0}
                    onSubmit={submitRate}
                    complaintId={item.id}
                />
            )}
        </Box>
    );
}