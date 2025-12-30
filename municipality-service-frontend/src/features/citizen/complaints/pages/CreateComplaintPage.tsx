// ============================================
// 1. src/features/citizen/complaints/pages/CreateComplaintPage.tsx
// ============================================
import { useEffect, useState } from "react";
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Fade,
    MenuItem,
    Paper,
    Stack,
    Step,
    StepLabel,
    Stepper,
    TextField,
    Typography,
} from "@mui/material";
import { Business, Category as CategoryIcon, CheckCircle, Description } from "@mui/icons-material";
import { useFormik } from "formik";
import { useNavigate } from "react-router-dom";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import { createComplaint, getPublicCategoriesByDepartment, getPublicDepartments } from "../api";
import type { CategoryItem, DepartmentItem } from "../types";
import {
    createComplaintValidationSchema,
    createComplaintInitialValues,
    type CreateComplaintFormValues,
} from "../validation/complaintValidation";

export default function CreateComplaintPage() {
    const nav = useNavigate();
    const toast = useToast();

    const [departments, setDepartments] = useState<DepartmentItem[]>([]);
    const [categories, setCategories] = useState<CategoryItem[]>([]);
    const [loadingDepts, setLoadingDepts] = useState(true);
    const [loadingCats, setLoadingCats] = useState(false);
    const [activeStep, setActiveStep] = useState(0);
    const [submitted, setSubmitted] = useState(false);
    const [trackingCode, setTrackingCode] = useState("");

    const formik = useFormik<CreateComplaintFormValues>({
        initialValues: createComplaintInitialValues,
        validationSchema: createComplaintValidationSchema,
        onSubmit: async (values) => {
            try {
                const res = await createComplaint({
                    categoryId: values.categoryId as number,
                    title: values.title.trim(),
                    description: values.description.trim() || undefined,
                });
                setTrackingCode(res.data.trackingCode);
                setSubmitted(true);
                toast.success("Şikayetiniz başarıyla oluşturuldu!");
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Şikayet oluşturulamadı."));
            }
        },
    });

    useEffect(() => {
        let mounted = true;
        (async () => {
            setLoadingDepts(true);
            try {
                const list = await getPublicDepartments();
                if (mounted) setDepartments(list);
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Departmanlar yüklenemedi."));
            } finally {
                if (mounted) setLoadingDepts(false);
            }
        })();
        return () => {
            mounted = false;
        };
    }, []);

    useEffect(() => {
        if (!formik.values.departmentId) {
            setCategories([]);
            formik.setFieldValue("categoryId", "");
            return;
        }
        let mounted = true;
        (async () => {
            setLoadingCats(true);
            setCategories([]);
            formik.setFieldValue("categoryId", "");
            try {
                const list = await getPublicCategoriesByDepartment(formik.values.departmentId);
                if (mounted) setCategories(list);
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Kategoriler yüklenemedi."));
            } finally {
                if (mounted) setLoadingCats(false);
            }
        })();
        return () => {
            mounted = false;
        };
    }, [formik.values.departmentId]);

    useEffect(() => {
        if (formik.values.departmentId && !formik.values.categoryId) setActiveStep(1);
        else if (formik.values.categoryId && !formik.values.title) setActiveStep(2);
        else if (formik.values.title) setActiveStep(3);
        else setActiveStep(0);
    }, [formik.values]);

    const steps = ["Departman Seç", "Kategori Seç", "Detayları Gir", "Gönder"];

    if (submitted) {
        return (
            <Box sx={{ py: 4, maxWidth: 700, mx: "auto" }}>
                <Fade in={submitted}>
                    <Card elevation={0} sx={{ border: "2px solid", borderColor: "success.main", bgcolor: "success.50" }}>
                        <CardContent sx={{ textAlign: "center", py: 6 }}>
                            <CheckCircle sx={{ fontSize: 80, color: "success.main", mb: 2 }} />
                            <Typography variant="h4" fontWeight={900} gutterBottom>
                                Şikayetiniz Alındı!
                            </Typography>
                            <Typography variant="body1" sx={{ mb: 3, opacity: 0.8 }}>
                                Şikayetiniz başarıyla kaydedildi. En kısa sürede değerlendirilecektir.
                            </Typography>
                            <Paper
                                elevation={0}
                                sx={{ p: 3, bgcolor: "white", borderRadius: 3, maxWidth: 400, mx: "auto", mb: 3 }}
                            >
                                <Typography variant="caption" sx={{ display: "block", mb: 1, opacity: 0.7 }}>
                                    Takip Kodunuz
                                </Typography>
                                <Typography
                                    variant="h5"
                                    fontWeight={900}
                                    color="primary"
                                    sx={{ fontFamily: "monospace", letterSpacing: 2 }}
                                    data-testid="tracking-code-display"
                                >
                                    {trackingCode}
                                </Typography>
                            </Paper>
                            <Stack direction="row" spacing={2} justifyContent="center">
                                <Button
                                    variant="contained"
                                    onClick={() => {
                                        setSubmitted(false);
                                        formik.resetForm();
                                    }}
                                    data-testid="create-another-btn"
                                >
                                    Yeni Şikayet Oluştur
                                </Button>
                                <Button variant="outlined" onClick={() => nav("/citizen/complaints")} data-testid="view-complaints-btn">
                                    Şikayetlerimi Görüntüle
                                </Button>
                            </Stack>
                        </CardContent>
                    </Card>
                </Fade>
            </Box>
        );
    }

    return (
        <Box sx={{ py: 4, maxWidth: 800, mx: "auto" }}>
            <Stack spacing={4}>
                <Box>
                    <Typography variant="h4" fontWeight={900} gutterBottom>
                        Yeni Şikayet Oluştur
                    </Typography>
                    <Typography variant="body1" sx={{ opacity: 0.7 }}>
                        Lütfen şikayetinizi detaylı bir şekilde açıklayın. Size en kısa sürede geri dönüş yapacağız.
                    </Typography>
                </Box>

                <Paper elevation={0} sx={{ p: 3, borderRadius: 3, bgcolor: "grey.50" }}>
                    <Stepper activeStep={activeStep} alternativeLabel>
                        {steps.map((label) => (
                            <Step key={label}>
                                <StepLabel>{label}</StepLabel>
                            </Step>
                        ))}
                    </Stepper>
                </Paper>

                <form onSubmit={formik.handleSubmit}>
                    <Stack spacing={3}>
                        <Card elevation={0} sx={{ border: "1px solid #e5e7eb" }}>
                            <CardContent>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <Business color="primary" />
                                        <Typography variant="h6" fontWeight={700}>
                                            Departman Seçimi
                                        </Typography>
                                        <Chip label="Zorunlu" size="small" color="error" />
                                    </Stack>

                                    <TextField
                                        select
                                        fullWidth
                                        name="departmentId"
                                        label="Hangi departmana şikayet etmek istiyorsunuz?"
                                        value={formik.values.departmentId}
                                        onChange={formik.handleChange}
                                        onBlur={formik.handleBlur}
                                        error={formik.touched.departmentId && Boolean(formik.errors.departmentId)}
                                        helperText={formik.touched.departmentId && formik.errors.departmentId}
                                        disabled={loadingDepts}
                                        SelectProps={{
                                            SelectDisplayProps: { className: "citizen-create-departmentId" },
                                        }}
                                    >
                                        <MenuItem value="">
                                            <em>Departman seçiniz</em>
                                        </MenuItem>
                                        {departments.map((dept) => (
                                            <MenuItem key={dept.id} value={dept.id}>
                                                {dept.name}
                                            </MenuItem>
                                        ))}
                                    </TextField>
                                </Stack>
                            </CardContent>
                        </Card>

                        <Card elevation={0} sx={{ border: "1px solid #e5e7eb", opacity: formik.values.departmentId ? 1 : 0.5 }}>
                            <CardContent>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <CategoryIcon color="primary" />
                                        <Typography variant="h6" fontWeight={700}>
                                            Kategori Seçimi
                                        </Typography>
                                        <Chip label="Zorunlu" size="small" color="error" />
                                    </Stack>

                                    <TextField
                                        select
                                        fullWidth
                                        name="categoryId"
                                        label="Şikayetinizin kategorisini seçin"
                                        value={formik.values.categoryId}
                                        onChange={formik.handleChange}
                                        onBlur={formik.handleBlur}
                                        disabled={!formik.values.departmentId || loadingCats}
                                        error={formik.touched.categoryId && Boolean(formik.errors.categoryId)}
                                        helperText={
                                            formik.touched.categoryId
                                                ? formik.errors.categoryId
                                                : !formik.values.departmentId
                                                    ? "Önce departman seçin"
                                                    : loadingCats
                                                        ? "Kategoriler yükleniyor..."
                                                        : ""
                                        }
                                        SelectProps={{
                                            SelectDisplayProps: { className: "citizen-create-categoryId" },
                                        }}
                                    >
                                        <MenuItem value="">
                                            <em>Kategori seçiniz</em>
                                        </MenuItem>
                                        {categories.map((cat) => (
                                            <MenuItem key={cat.id} value={cat.id}>
                                                {cat.name}
                                            </MenuItem>
                                        ))}
                                    </TextField>
                                </Stack>
                            </CardContent>
                        </Card>

                        <Card elevation={0} sx={{ border: "1px solid #e5e7eb", opacity: formik.values.categoryId ? 1 : 0.5 }}>
                            <CardContent>
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <Description color="primary" />
                                        <Typography variant="h6" fontWeight={700}>
                                            Şikayet Detayları
                                        </Typography>
                                    </Stack>

                                    <TextField
                                        fullWidth
                                        name="title"
                                        label="Şikayet Başlığı"
                                        placeholder="Örn: Parkta aydınlatma sorunu var"
                                        value={formik.values.title}
                                        onChange={formik.handleChange}
                                        onBlur={formik.handleBlur}
                                        disabled={!formik.values.categoryId}
                                        error={formik.touched.title && Boolean(formik.errors.title)}
                                        helperText={formik.touched.title ? formik.errors.title : `${formik.values.title.length}/200 karakter`}
                                        inputProps={{ maxLength: 200, "data-testid": "citizen-create-title" }}
                                    />

                                    <TextField
                                        fullWidth
                                        multiline
                                        rows={6}
                                        name="description"
                                        label="Detaylı Açıklama"
                                        placeholder="Şikayetinizi detaylı bir şekilde açıklayın..."
                                        value={formik.values.description}
                                        onChange={formik.handleChange}
                                        onBlur={formik.handleBlur}
                                        disabled={!formik.values.categoryId}
                                        error={formik.touched.description && Boolean(formik.errors.description)}
                                        helperText={
                                            formik.touched.description
                                                ? formik.errors.description
                                                : `${formik.values.description.length}/2000 karakter`
                                        }
                                        inputProps={{ maxLength: 2000, "data-testid": "citizen-create-description" }}
                                    />

                                    {formik.values.title && (
                                        <Alert severity="info" sx={{ borderRadius: 2 }}>
                                            <Typography variant="body2">
                                                <strong>İpucu:</strong> Şikayetinizi detaylı açıkladığınızda daha hızlı çözüme ulaşabilirsiniz.
                                            </Typography>
                                        </Alert>
                                    )}
                                </Stack>
                            </CardContent>
                        </Card>

                        <Button
                            type="submit"
                            variant="contained"
                            size="large"
                            disabled={formik.isSubmitting || !formik.isValid}
                            fullWidth
                            sx={{ py: 1.5, fontSize: "1.1rem", fontWeight: 700 }}
                            data-testid="citizen-create-submit"
                        >
                            {formik.isSubmitting ? (
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <CircularProgress size={20} color="inherit" />
                                    <span>Gönderiliyor...</span>
                                </Stack>
                            ) : (
                                "Şikayeti Gönder"
                            )}
                        </Button>
                    </Stack>
                </form>

                <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                    <Paper elevation={0} sx={{ p: 2, flex: 1, bgcolor: "primary.50", borderRadius: 2 }}>
                        <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                            📞 Acil Durumlar
                        </Typography>
                        <Typography variant="body2" sx={{ opacity: 0.8 }}>
                            Acil durumlar için 153 numaralı hattı arayabilirsiniz.
                        </Typography>
                    </Paper>
                    <Paper elevation={0} sx={{ p: 2, flex: 1, bgcolor: "warning.50", borderRadius: 2 }}>
                        <Typography variant="subtitle2" fontWeight={700} gutterBottom>
                            ⏱️ Cevap Süresi
                        </Typography>
                        <Typography variant="body2" sx={{ opacity: 0.8 }}>
                            Şikayetleriniz ortalama 2-3 iş günü içinde cevaplanır.
                        </Typography>
                    </Paper>
                </Stack>
            </Stack>
        </Box>
    );
}
