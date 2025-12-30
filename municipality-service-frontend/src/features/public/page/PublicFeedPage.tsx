// ============================================
// 11. src/features/public/page/PublicFeedPage.tsx
// ============================================
import { useEffect, useState } from "react";
import { Card, CardContent, Pagination, Rating, Stack, Typography, Box, Chip } from "@mui/material";
import { AccessTime } from "@mui/icons-material";
import Loading from "../../../shared/components/Loading";
import EmptyState from "../../../shared/components/EmptyState";
import { useToast } from "../../../shared/utils/toast";
import { getErrorMessage } from "../../../shared/api/error";
import { formatComplaintStatus } from "../../../shared/utils/complaintStatus";
import { getPublicFeed, type PublicFeedItem } from "../api";
import { getSession } from "../../../shared/auth/authStore";
import { upsertRating } from "../../feedback/api";

export default function PublicFeedPage() {
    const toast = useToast();
    const { token } = getSession();

    const [items, setItems] = useState<PublicFeedItem[]>([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [savingId, setSavingId] = useState<number | null>(null);

    const load = async (p = 0) => {
        setLoading(true);
        try {
            const res = await getPublicFeed(p, 10);
            setItems(res.data.content || []);
            setTotalPages(res.data.totalPages || 0);
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Şikayetler yüklenmedi"));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(0); }, []);

    const handleRate = async (complaintId: number, value: number | null) => {
        if (!value) return;
        if (!token) {
            toast.error("Değerlendirme yapmak için giriş yapmalısın.");
            return;
        }

        const rating = Math.max(1, Math.min(5, Math.round(value)));
        setSavingId(complaintId);

        setItems((prev) =>
            prev.map((x) => {
                if (x.id !== complaintId) return x;
                const hadMy = typeof x.myRating === "number" && x.myRating > 0;
                const oldMy = hadMy ? (x.myRating as number) : 0;
                const oldCount = x.ratingCount ?? 0;
                const oldAvg = x.avgRating ?? 0;

                let newCount = oldCount;
                let newAvg = oldAvg;

                if (!hadMy) {
                    newCount = oldCount + 1;
                    newAvg = oldCount === 0 ? rating : (oldAvg * oldCount + rating) / newCount;
                } else {
                    newAvg = oldCount === 0 ? rating : (oldAvg * oldCount - oldMy + rating) / oldCount;
                }

                return { ...x, myRating: rating, avgRating: newAvg, ratingCount: newCount };
            })
        );

        try {
            const res = await upsertRating(complaintId, rating);
            const stats = res.data as any;
            setItems((prev) =>
                prev.map((x) =>
                    x.id === complaintId
                        ? {
                            ...x,
                            avgRating: stats.avgRating ?? x.avgRating,
                            ratingCount: stats.ratingCount ?? x.ratingCount,
                            myRating: stats.myRating ?? rating,
                        }
                        : x
                )
            );
            toast.success("Değerlendirmen kaydedildi.");
        } catch (err: any) {
            const status = err?.response?.status;
            if (status === 401 || status === 403) {
                toast.error("Değerlendirme yapmak için giriş yapmalısın.");
            } else {
                toast.error(getErrorMessage(err, "Değerlendirme kaydedilemedi."));
            }
            await load(page);
        } finally {
            setSavingId(null);
        }
    };

    return (
        <Box sx={{ py: 4, maxWidth: 900, mx: "auto" }} data-testid="home-feed">
            <Stack spacing={3}>
                <Box textAlign="center">
                    <Typography variant="h4" fontWeight={900} gutterBottom data-testid="home-title">
                        Çözülen Şikayetler
                    </Typography>
                    <Typography sx={{ opacity: 0.7 }} data-testid="home-subtitle">
                        Çözülen şikayetler kamuya açık şekilde listelenir
                    </Typography>
                </Box>

                {loading && <Loading />}

                {!loading && items.length === 0 && (
                    <EmptyState
                        title="Henüz kayıt yok"
                        desc="Çözülen şikayet oluştuğunda burada listelenecek."
                    />
                )}

                {!loading && items.length > 0 && (
                    <>
                        <Stack spacing={2} data-testid="feed-list">
                            {items.map((x) => (
                                <Card key={x.id} elevation={0} sx={{ border: "1px solid #e5e7eb" }} data-testid={`feed-card-${x.id}`}>
                                    <CardContent>
                                        <Stack spacing={2}>
                                            <Stack direction="row" justifyContent="space-between" alignItems="center">
                                                <Typography variant="h6" fontWeight={900} data-testid={`feed-title-${x.id}`}>
                                                    {x.title}
                                                </Typography>
                                                <Chip label={formatComplaintStatus(x.status)} size="small" color="success" data-testid={`feed-status-${x.id}`} />
                                            </Stack>

                                            <Card variant="outlined" sx={{ bgcolor: "primary.50" }} data-testid={`feed-rating-${x.id}`}>
                                                <CardContent>
                                                    <Stack direction="row" alignItems="center" justifyContent="space-between">
                                                        <Stack direction="row" alignItems="center" spacing={1}>
                                                            <Rating
                                                                value={x.myRating ?? x.avgRating ?? 0}
                                                                precision={1}
                                                                disabled={savingId === x.id}
                                                                onChange={(_, v) => handleRate(x.id, v)}
                                                                data-testid={`feed-rating-input-${x.id}`}
                                                            />
                                                            <Typography variant="body2" sx={{ opacity: 0.85 }} data-testid={`feed-rating-text-${x.id}`}>
                                                                {(x.avgRating ?? 0).toFixed(1)} ({x.ratingCount ?? 0})
                                                            </Typography>
                                                        </Stack>
                                                    </Stack>
                                                    {!token && (
                                                        <Typography variant="caption" sx={{ opacity: 0.7, display: "block", mt: 1 }} data-testid={`feed-rating-login-hint-${x.id}`}>
                                                            Değerlendirmek için giriş yap.
                                                        </Typography>
                                                    )}
                                                </CardContent>
                                            </Card>

                                            <Stack spacing={0.5}>
                                                <Typography variant="body2" sx={{ opacity: 0.7, fontFamily: "monospace" }}>
                                                    Takip: <strong>{x.trackingCode}</strong>
                                                </Typography>
                                                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                                                    {x.categoryName} • {x.departmentName}
                                                </Typography>
                                            </Stack>

                                            {x.publicAnswer && (
                                                <Card variant="outlined" sx={{ bgcolor: "success.50" }}>
                                                    <CardContent>
                                                        <Typography variant="caption" sx={{ opacity: 0.7 }}>Yanıt</Typography>
                                                        <Typography data-testid={`feed-answer-${x.id}`}>{x.publicAnswer}</Typography>
                                                    </CardContent>
                                                </Card>
                                            )}

                                            {x.answeredAt && (
                                                <Stack direction="row" alignItems="center" spacing={0.5} sx={{ opacity: 0.6 }}>
                                                    <AccessTime fontSize="small" />
                                                    <Typography variant="caption" data-testid={`feed-answeredAt-${x.id}`}>
                                                        {new Date(x.answeredAt).toLocaleString("tr-TR")}
                                                    </Typography>
                                                </Stack>
                                            )}
                                        </Stack>
                                    </CardContent>
                                </Card>
                            ))}
                        </Stack>

                        {totalPages > 1 && (
                            <Stack alignItems="center" sx={{ pt: 2 }}>
                                <Pagination
                                    count={totalPages}
                                    page={page + 1}
                                    onChange={(_, v) => {
                                        const p = v - 1;
                                        setPage(p);
                                        load(p);
                                    }}
                                    data-testid="feed-pagination"
                                />
                            </Stack>
                        )}
                    </>
                )}
            </Stack>
        </Box>
    );
}