// ============================================
// 2. src/features/auth/pages/LoginPage.tsx
// ============================================
import { useEffect } from "react";
import { Button, Card, CardContent, CircularProgress, Stack, TextField, Typography, Alert, InputAdornment, IconButton } from "@mui/material";
import { Email, Lock, Visibility, VisibilityOff } from "@mui/icons-material";
import { Link, useNavigate } from "react-router-dom";
import { useFormik } from "formik";
import { login } from "../api";
import { saveAuth, getSession, clearAuth } from "../../../shared/auth/authStore";
import { useToast } from "../../../shared/utils/toast";
import { hasRole } from "../../../shared/auth/jwt";
import { getErrorMessage } from "../../../shared/api/error";
import { loginValidationSchema, loginInitialValues, type LoginFormValues } from "../validation/authValidation";
import { useState } from "react";

export default function LoginPage() {
    const nav = useNavigate();
    const toast = useToast();
    const [showPassword, setShowPassword] = useState(false);

    useEffect(() => { clearAuth(); }, []);

    const formik = useFormik<LoginFormValues>({
        initialValues: loginInitialValues,
        validationSchema: loginValidationSchema,
        onSubmit: async (values) => {
            try {
                const res = await login({ emailOrPhone: values.emailOrPhone.trim(), password: values.password.trim() });
                const token = res?.data?.accessToken || res?.data?.token;
                if (!token) {
                    clearAuth();
                    toast.error("Giriş yapılamadı, sayfayı yenileyip tekrar dene");
                    return;
                }

                saveAuth(res.data);
                const { payload } = getSession();
                if (!payload) {
                    clearAuth();
                    toast.error("Giriş yapılamadı. Oturum okunamadı");
                    return;
                }

                if (hasRole(payload, "ADMIN")) nav("/admin");
                else if (hasRole(payload, "AGENT")) nav("/agent/complaints");
                else nav("/citizen/complaints");

                toast.success("Giriş başarılı. Hoş geldin!");
            } catch (err: any) {
                clearAuth();
                toast.error(getErrorMessage(err, "Giriş yapılamadı."));
            }
        },
    });

    const passwordErr =
        formik.touched.password && formik.errors.password ? formik.errors.password : "";

    return (
        <Stack alignItems="center" sx={{ py: 6 }}>
            <Card sx={{ width: "100%", maxWidth: 480, boxShadow: 3 }}>
                <CardContent sx={{ p: 4 }}>
                    <Typography variant="h4" fontWeight={900} gutterBottom textAlign="center">
                        Giriş Yap
                    </Typography>
                    <Typography sx={{ opacity: 0.7, mb: 4, textAlign: "center" }}>
                        Arnavutköy Belediyesi Şikayet & CRM Sistemi
                    </Typography>

                    <form onSubmit={formik.handleSubmit}>
                        <Stack spacing={3}>
                            <TextField
                                fullWidth
                                name="emailOrPhone"
                                label="E-posta veya Telefon"
                                value={formik.values.emailOrPhone}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.emailOrPhone && Boolean(formik.errors.emailOrPhone)}
                                helperText={formik.touched.emailOrPhone && formik.errors.emailOrPhone}
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <Email color="action" />
                                        </InputAdornment>
                                    ),
                                }}
                                inputProps={{ "data-testid": "auth-login-emailOrPhone" }}
                                autoComplete="username"
                            />

                            <TextField
                                fullWidth
                                name="password"
                                label="Şifre"
                                type={showPassword ? "text" : "password"}
                                value={formik.values.password}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.password && Boolean(formik.errors.password)}
                                helperText={
                                    passwordErr ? (
                                        <span data-testid="auth-register-phone-error">{passwordErr}</span>
                                    ) : (
                                        " "
                                    )

                                }
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <Lock color="action" />
                                        </InputAdornment>
                                    ),
                                    endAdornment: (
                                        <InputAdornment position="end">
                                            <IconButton onClick={() => setShowPassword(!showPassword)} edge="end">
                                                {showPassword ? <VisibilityOff /> : <Visibility />}
                                            </IconButton>
                                        </InputAdornment>
                                    ),
                                }}
                                inputProps={{ "data-testid": "auth-login-password" }}
                                autoComplete="current-password"
                            />

                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                fullWidth
                                disabled={formik.isSubmitting || !formik.isValid}
                                data-testid="auth-login-submit"
                                sx={{ py: 1.5 }}
                            >
                                {formik.isSubmitting ? <CircularProgress size={24} color="inherit" /> : "Giriş Yap"}
                            </Button>

                            <Stack direction="row" justifyContent="space-between" alignItems="center">
                                <Typography variant="body2">
                                    Hesabın yok mu? <Link to="/auth/register" style={{ fontWeight: 700 }}>Kayıt Ol</Link>
                                </Typography>
                            </Stack>

                            <Alert severity="info" sx={{ borderRadius: 2 }}>
                                <Typography variant="body2">
                                    <Link to="/" style={{ fontWeight: 700 }}>Ana Sayfa</Link> • <Link to="/citizen/track" style={{ fontWeight: 700 }}>Takip</Link>
                                </Typography>
                            </Alert>
                        </Stack>
                    </form>
                </CardContent>
            </Card>
        </Stack>
    );
}