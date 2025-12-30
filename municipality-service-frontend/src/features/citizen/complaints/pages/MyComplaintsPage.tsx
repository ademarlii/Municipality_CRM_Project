// ============================================
// 10. src/features/citizen/complaints/pages/MyComplaintsPage.tsx
// ============================================
import { useEffect, useState } from "react";
import { Button, Card, CardContent, Rating, Stack, Typography, Box, Chip, IconButton } from "@mui/material";
import { Add, Star, AccessTime } from "@mui/icons-material";
import { Link as RouterLink } from "react-router-dom";
import Loading from "../../../../shared/components/Loading";
import EmptyState from "../../../../shared/components/EmptyState";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import { listMyComplaints } from "../api";
import type { ComplaintSummary, FeedbackSummary } from "../types";
import { formatComplaintStatus, isResolved } from "../../../../shared/utils/complaintStatus";
import { getFeedbackSummary, getMyRating, upsertRating } from "../../../feedback/api";
import ComplaintRatingDialog from "../components/ComplaintRatingDialog";

export default function MyComplaintsPage() {
    const toast = useToast();
    const [items, setItems] = useState<ComplaintSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [summaries, setSummaries] = useState<Record<number, FeedbackSummary>>({});
    const [myRatings, setMyRatings] = useState<Record<number, number | null>>({});
    const [rateOpen, setRateOpen] = useState(false);
    const [rateId, setRateId] = useState<number | null>(null);

    const currentMyRating = rateId ? (myRatings[rateId] ?? 0) : 0;

    const loadRatingsForResolved = async (list: ComplaintSummary[]) => {
        const resolvedIds = list.filter(x => isResolved(x.status)).map(x => x.id);

        const sPairs = await Promise.all(
            resolvedIds.map(async (id) => {
                try {
                    const res = await getFeedbackSummary(id);
                    return [id, res.data] as const;
                } catch {
                    return [id, { complaintId: id, avgRating: 0, ratingCount: 0 }] as const;
                }
            })
        );

        setSummaries(prev => {
            const copy = { ...prev };
            for (const [id, s] of sPairs) copy[id] = s;
            return copy;
        });

        const rPairs = await Promise.all(
            resolvedIds.map(async (id) => {
                try {
                    const res = await getMyRating(id);
                    return [id, res.data] as const;
                } catch {
                    return [id, null] as const;
                }
            })
        );

        setMyRatings(prev => {
            const copy = { ...prev };
            for (const [id, r] of rPairs) copy[id] = r;
            return copy;
        });
    };

    const load = async () => {
        setLoading(true);
        try {
            const res = await listMyComplaints();
            const list = res.data || [];
            setItems(list);
            await loadRatingsForResolved(list);
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Şikayetler yüklenemedi."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const openRate = (complaintId: number) => {
        setRateId(complaintId);
        setRateOpen(true);
    };

    const submitRate = async (value: number) => {
        if (!rateId) return;
        try {
            const res = await upsertRating(rateId, value);
            setSummaries(prev => ({ ...prev, [rateId]: res.data }));
            setMyRatings(prev => ({ ...prev, [rateId]: res.data.myRating ?? value }));
            toast.success("Puan kaydedildi.");
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Puan kaydedilemedi."));
        }
    };

    return (
        <Box sx={{ py: 4, maxWidth: 900, mx: "auto" }} data-testid="citizen-my-complaints">
            <Stack spacing={3}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Box>
                        <Typography variant="h4" fontWeight={900} data-testid="citizen-my-complaints-title">
                            Şikayetlerim
                        </Typography>
                        <Typography variant="body2" sx={{ opacity: 0.7, mt: 0.5 }}>
                            Oluşturduğun şikayetleri buradan takip edebilirsin
                        </Typography>
                    </Box>
                    <Button
                        component={RouterLink}
                        to="/citizen/complaints/new"
                        variant="contained"
                        startIcon={<Add />}
                        data-testid="citizen-new-complaint"
                        sx={{ borderRadius: 2 }}
                    >
                        Yeni Şikayet
                    </Button>
                </Stack>

                {loading && <Loading />}

                {!loading && items.length === 0 && (
                    <EmptyState
                        title="Henüz şikayet yok"
                        desc="Yeni şikayet oluşturduğunda burada listelenecek."
                    />
                )}

                {!loading && items.length > 0 && (
                    <Stack spacing={2} data-testid="citizen-complaints-list">
                        {items.map((c) => {
                            const sum = summaries[c.id];
                            //const my = myRatings[c.id];
                            const resolved = isResolved(c.status);

                            return (
                                <Card
                                    key={c.id}
                                    elevation={0}
                                    sx={{
                                        border: "1px solid #e5e7eb",
                                        transition: "all 0.2s",
                                        "&:hover": { boxShadow: 3 },
                                    }}
                                    component={RouterLink as any}
                                    to={`/citizen/complaints/${c.id}`}
                                    data-testid={`citizen-complaint-card-${c.id}`}
                                >
                                    <CardContent>
                                        <Stack spacing={2}>
                                            <Stack direction="row" justifyContent="space-between" alignItems="center">
                                                <Typography variant="h6" fontWeight={900} data-testid={`citizen-complaint-title-${c.id}`}>
                                                    {c.title}
                                                </Typography>
                                                <Chip
                                                    label={formatComplaintStatus(c.status)}
                                                    size="small"
                                                    color={resolved ? "success" : "default"}
                                                    data-testid={`citizen-complaint-status-${c.id}`}
                                                />
                                            </Stack>

                                            <Typography
                                                variant="body2"
                                                sx={{ opacity: 0.7, fontFamily: "monospace" }}
                                            >
                                                Takip: <strong>{c.trackingCode}</strong>
                                            </Typography>

                                            {resolved && (
                                                <Card variant="outlined" sx={{ bgcolor: "success.50" }} data-testid={`citizen-rating-block-${c.id}`}>
                                                    <CardContent>
                                                        <Stack
                                                            direction="row"
                                                            alignItems="center"
                                                            justifyContent="space-between"
                                                        >
                                                            <Stack direction="row" alignItems="center" spacing={1}>
                                                                <Rating
                                                                    value={sum?.avgRating ?? 0}
                                                                    precision={0.1}
                                                                    readOnly
                                                                    size="small"
                                                                />
                                                                <Typography variant="body2" sx={{ opacity: 0.85 }}>
                                                                    {(sum?.avgRating ?? 0).toFixed(1)} ({sum?.ratingCount ?? 0})
                                                                </Typography>
                                                            </Stack>

                                                            <IconButton
                                                                size="small"
                                                                color="primary"
                                                                onClick={(e) => {
                                                                    e.preventDefault();
                                                                    e.stopPropagation();
                                                                    openRate(c.id);
                                                                }}
                                                                data-testid={`citizen-rate-btn-${c.id}`}
                                                            >
                                                                <Star />
                                                            </IconButton>
                                                        </Stack>
                                                    </CardContent>
                                                </Card>
                                            )}

                                            {c.createdAt && (
                                                <Stack direction="row" alignItems="center" spacing={0.5} sx={{ opacity: 0.6 }}>
                                                    <AccessTime fontSize="small" />
                                                    <Typography variant="caption" data-testid={`citizen-complaint-createdAt-${c.id}`}>
                                                        {new Date(c.createdAt).toLocaleString("tr-TR")}
                                                    </Typography>
                                                </Stack>
                                            )}
                                        </Stack>
                                    </CardContent>
                                </Card>
                            );
                        })}
                    </Stack>
                )}
            </Stack>

            {rateId != null && (
                <ComplaintRatingDialog
                    open={rateOpen}
                    onClose={() => setRateOpen(false)}
                    initialValue={currentMyRating ?? 0}
                    onSubmit={submitRate}
                    complaintId={rateId}
                />
            )}
        </Box>
    );
}