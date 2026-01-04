import { useEffect, useState } from "react";
import {
    Button,
    Card,
    CardContent,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    Stack,
    TextField,
    Typography,
    CircularProgress,
    Alert,
    Box,
    InputAdornment,
} from "@mui/material";
import { Search, FilterList, Edit } from "@mui/icons-material";
import { useFormik } from "formik";
import {
    listMyDepartmentComplaints,
    changeStatus,
    type StaffComplaintListItem,
    type StatusKey,
} from "../api";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import {
    changeStatusValidationSchema,
    changeStatusInitialValues,
    type ChangeStatusFormValues,
} from "../validation/agentComplaintValidation";

const STATUS_OPTIONS: Array<{ value: StatusKey; label: string }> = [
    { value: "NEW", label: "Yeni" },
    { value: "IN_REVIEW", label: "İnceleniyor" },
    { value: "RESOLVED", label: "Çözüldü" },
    { value: "CLOSED", label: "Kapandı" },
];

function statusLabel(s: StatusKey) {
    return STATUS_OPTIONS.find((x) => x.value === s)?.label ?? s;
}

function statusColor(s: StatusKey): "default" | "info" | "warning" | "success" {
    switch (s) {
        case "NEW":
            return "info";
        case "IN_REVIEW":
            return "warning";
        case "RESOLVED":
            return "success";
        case "CLOSED":
            return "default";
        default:
            return "default";
    }
}

function safeRows(resData: any): StaffComplaintListItem[] {
    if (resData?.content && Array.isArray(resData.content)) return resData.content;
    if (Array.isArray(resData)) return resData;
    return [];
}

export default function ComplaintListPage() {
    const toast = useToast();

    const [rows, setRows] = useState<StaffComplaintListItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [q, setQ] = useState("");
    const [status, setStatus] = useState<StatusKey | "ALL">("ALL");
    const [open, setOpen] = useState(false);
    const [selected, setSelected] = useState<StaffComplaintListItem | null>(null);

    const formik = useFormik<ChangeStatusFormValues>({
        initialValues: changeStatusInitialValues,
        validationSchema: changeStatusValidationSchema,
        onSubmit: async (values) => {
            if (!selected) return;
            try {
                await changeStatus(selected.id, {
                    toStatus: values.toStatus as StatusKey,
                    note: values.note.trim() || undefined,
                    publicAnswer:
                        values.toStatus === "RESOLVED"
                            ? values.publicAnswer.trim()
                            : undefined,
                });
                toast.success("Şikayet güncellendi.");
                closeEdit();
                await load();
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Güncelleme başarısız."));
            }
        },
    });

    const load = async () => {
        setLoading(true);
        try {
            const res = await listMyDepartmentComplaints({
                q: q.trim() || undefined,
                status: status === "ALL" ? undefined : status,
                page: 0,
                size: 100,
            });
            setRows(safeRows(res.data));
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Şikayetler alınamadı."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
    }, []);

    const openEdit = (c: StaffComplaintListItem) => {
        setSelected(c);
        formik.setValues({
            toStatus: c.status,
            note: "",
            publicAnswer: "",
        });
        setOpen(true);
    };

    const closeEdit = () => {
        setOpen(false);
        setSelected(null);
        formik.resetForm();
    };

    return (
        <Stack spacing={3}>
            <Box>
                <Typography variant="h4" fontWeight={900}>
                    Şikayetler
                </Typography>
                <Typography variant="body2" sx={{ opacity: 0.7, mt: 0.5 }}>
                    Departmanınıza atanan şikayetleri yönetin
                </Typography>
            </Box>

            {/* filtreler - çalışmıyor dedin, dokunmuyoruz; sadece testid eklemek istersen sen ekle */}
            <Card elevation={0} sx={{ border: "1px solid #e5e7eb" }}>
                <CardContent>
                    <Stack
                        direction={{ xs: "column", sm: "row" }}
                        spacing={2}
                        alignItems="center"
                    >
                        <TextField
                            label="Ara (Takip Kodu / Başlık)"
                            value={q}
                            onChange={(e) => setQ(e.target.value)}
                            sx={{ flex: 1 }}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <Search />
                                    </InputAdornment>
                                ),
                            }}
                        />

                        <TextField
                            select
                            label="Durum"
                            value={status}
                            onChange={(e) => setStatus(e.target.value as any)}
                            sx={{ width: 220 }}
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <FilterList />
                                    </InputAdornment>
                                ),
                            }}
                        >
                            <MenuItem value="ALL">Hepsi</MenuItem>
                            {STATUS_OPTIONS.map((s) => (
                                <MenuItem key={s.value} value={s.value}>
                                    {s.label}
                                </MenuItem>
                            ))}
                        </TextField>

                        <Button variant="contained" onClick={load} disabled={loading}>
                            {loading ? <CircularProgress size={22} /> : "Yenile"}
                        </Button>
                    </Stack>
                </CardContent>
            </Card>

            <Stack spacing={2}>
                {loading && (
                    <Card>
                        <CardContent sx={{ textAlign: "center", py: 4 }}>
                            <CircularProgress />
                        </CardContent>
                    </Card>
                )}

                {!loading && rows.length === 0 && (
                    <Card>
                        <CardContent>
                            <Typography sx={{ opacity: 0.75 }}>
                                Kriterlere uygun şikayet bulunamadı.
                            </Typography>
                        </CardContent>
                    </Card>
                )}

                {!loading &&
                    rows.map((c) => (
                        <Card
                            key={c.id}
                            data-testid={`agent-complaint-card-${c.id}`}
                            elevation={0}
                            sx={{ border: "1px solid #e5e7eb" }}
                        >
                            <CardContent>
                                <Stack
                                    direction="row"
                                    justifyContent="space-between"
                                    alignItems="center"
                                >
                                    <Stack flex={1}>
                                        <Typography
                                            data-testid={`agent-complaint-title-${c.id}`}
                                            fontWeight={900}
                                        >
                                            {c.title}
                                        </Typography>

                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            alignItems="center"
                                            sx={{ mt: 0.5 }}
                                        >
                                            <Typography
                                                data-testid={`agent-complaint-tracking-${c.id}`}
                                                variant="body2"
                                                sx={{ opacity: 0.7, fontFamily: "monospace" }}
                                            >
                                                {c.trackingCode}
                                            </Typography>

                                            <Chip
                                                data-testid={`agent-complaint-status-${c.id}`}
                                                label={statusLabel(c.status)}
                                                size="small"
                                                color={statusColor(c.status)}
                                            />
                                        </Stack>
                                    </Stack>

                                    <Button
                                        data-testid={`agent-complaint-edit-${c.id}`}
                                        variant="outlined"
                                        size="small"
                                        startIcon={<Edit />}
                                        onClick={() => openEdit(c)}
                                    >
                                        Düzenle
                                    </Button>
                                </Stack>
                            </CardContent>
                        </Card>
                    ))}
            </Stack>

            <Dialog
                open={open}
                onClose={closeEdit}
                fullWidth
                maxWidth="sm"
                data-testid="agent-edit-dialog"
            >
                <DialogTitle>Şikayeti Düzenle</DialogTitle>

                <form onSubmit={formik.handleSubmit}>
                    <DialogContent>
                        <Stack spacing={3} sx={{ pt: 1 }}>
                            <Alert severity="info" data-testid="agent-edit-info">
                                #{selected?.id} • {selected?.trackingCode}
                            </Alert>

                            <TextField
                                data-testid="agent-edit-status"
                                select
                                fullWidth
                                name="toStatus"
                                label="Durum"
                                value={formik.values.toStatus}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.toStatus && Boolean(formik.errors.toStatus)}
                                helperText={formik.touched.toStatus && formik.errors.toStatus}
                                inputProps={{ "data-testid": "agent-edit-status-input" }}
                            >
                                {STATUS_OPTIONS.map((s) => (
                                    <MenuItem key={s.value} value={s.value}>
                                        {s.label}
                                    </MenuItem>
                                ))}
                            </TextField>

                            <TextField
                                data-testid="agent-edit-note"
                                fullWidth
                                name="note"
                                label="Not (Opsiyonel)"
                                multiline
                                rows={3}
                                value={formik.values.note}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.note && Boolean(formik.errors.note)}
                                helperText={formik.touched.note && formik.errors.note}
                                inputProps={{ "data-testid": "agent-edit-note-input" }}
                            />

                            {formik.values.toStatus === "RESOLVED" && (
                                <TextField
                                    data-testid="agent-edit-publicAnswer"
                                    fullWidth
                                    name="publicAnswer"
                                    label="Vatandaşa Görünecek Cevap"
                                    multiline
                                    rows={4}
                                    value={formik.values.publicAnswer}
                                    onChange={formik.handleChange}
                                    onBlur={formik.handleBlur}
                                    error={
                                        formik.touched.publicAnswer &&
                                        Boolean(formik.errors.publicAnswer)
                                    }
                                    helperText={
                                        formik.touched.publicAnswer
                                            ? formik.errors.publicAnswer
                                            : "Çözüldü durumu için zorunludur"
                                    }
                                    required
                                    inputProps={{ "data-testid": "agent-edit-publicAnswer-input" }}
                                />
                            )}
                        </Stack>
                    </DialogContent>

                    <DialogActions>
                        <Button
                            onClick={closeEdit}
                            disabled={formik.isSubmitting}
                            data-testid="agent-edit-cancel"
                        >
                            Vazgeç
                        </Button>

                        <Button
                            type="submit"
                            variant="contained"
                            disabled={formik.isSubmitting || !formik.isValid}
                            data-testid="agent-edit-save"
                        >
                            {formik.isSubmitting ? "Kaydediliyor..." : "Kaydet"}
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>
        </Stack>
    );
}
