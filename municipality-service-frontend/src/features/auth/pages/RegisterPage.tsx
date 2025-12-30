// ============================================
// 3. src/features/auth/pages/RegisterPage.tsx
// ============================================
import { useState } from "react";
import { Button, Card, CardContent, CircularProgress, Stack, TextField, Typography, InputAdornment, IconButton } from "@mui/material";
import { Email, Phone, Lock, Visibility, VisibilityOff } from "@mui/icons-material";
import { Link, useNavigate } from "react-router-dom";
import { useFormik } from "formik";
import { register } from "../api";
import { saveAuth, getSession } from "../../../shared/auth/authStore";
import { useToast } from "../../../shared/utils/toast";
import { hasRole } from "../../../shared/auth/jwt";
import { getErrorMessage } from "../../../shared/api/error";
import { registerValidationSchema, registerInitialValues, type RegisterFormValues } from "../validation/authValidation";

export default function RegisterPage() {
    const nav = useNavigate();
    const toast = useToast();
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const formik = useFormik<RegisterFormValues>({
        initialValues: registerInitialValues,
        validationSchema: registerValidationSchema,
        onSubmit: async (values) => {
            try {
                const res = await register({
                    email: values.email.trim(),
                    phone: values.phone.trim(),
                    password: values.password.trim(),
                });
                saveAuth(res.data);

                const { payload } = getSession();
                if (hasRole(payload, "ADMIN")) nav("/admin/categories");
                else if (hasRole(payload, "AGENT")) nav("/agent/complaints");
                else nav("/citizen/complaints");

                toast.success("Kayıt başarılı. Hoş geldin!");
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Kayıt oluşturulamadı."));
            }
        },
    });

    const phoneErr =
        formik.touched.phone && formik.errors.phone ? formik.errors.phone : "";
    const confirmErr =
        formik.touched.confirmPassword && formik.errors.confirmPassword
            ? formik.errors.confirmPassword
            : "";


    return (
        <Stack alignItems="center" sx={{ py: 6 }}>
            <Card sx={{ width: "100%", maxWidth: 480, boxShadow: 3 }}>
                <CardContent sx={{ p: 4 }}>
                    <Typography variant="h4" fontWeight={900} gutterBottom textAlign="center">
                        Kayıt Ol
                    </Typography>
                    <Typography sx={{ opacity: 0.7, mb: 4, textAlign: "center" }}>
                        Vatandaş hesabı ile şikayet oluşturabilir ve takip edebilirsin.
                    </Typography>

                    <form onSubmit={formik.handleSubmit}>
                        <Stack spacing={3}>
                            <TextField
                                fullWidth
                                name="email"
                                label="E-posta"
                                value={formik.values.email}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.email && Boolean(formik.errors.email)}
                                helperText={formik.touched.email && formik.errors.email}
                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <Email color="action" />
                                        </InputAdornment>
                                    ),
                                }}
                                inputProps={{ "data-testid": "auth-register-email" }}
                            />

                            <TextField
                                fullWidth
                                name="phone"
                                label="Telefon (5XXXXXXXXX)"
                                value={formik.values.phone}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.phone && Boolean(formik.errors.phone)}
                                helperText={
                                    phoneErr ? (
                                        <span data-testid="auth-register-phone-error">{phoneErr}</span>
                                    ) : (
                                        " "
                                    )
                                }

                                InputProps={{
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <Phone color="action" />
                                        </InputAdornment>
                                    ),
                                }}
                                inputProps={{ "data-testid": "auth-register-phone", maxLength: 10 }}

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
                                helperText={formik.touched.password && formik.errors.password}
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
                                inputProps={{ "data-testid": "auth-register-password" }}
                            />

                            <TextField
                                fullWidth
                                name="confirmPassword"
                                label="Şifre Tekrar"
                                type={showConfirmPassword ? "text" : "password"}
                                value={formik.values.confirmPassword}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.confirmPassword && Boolean(formik.errors.confirmPassword)}

                                helperText={
                                    confirmErr ? (
                                        <span data-testid="auth-register-confirm-error">{confirmErr}</span>
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
                                            <IconButton onClick={() => setShowConfirmPassword(!showConfirmPassword)} edge="end">
                                                {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                                            </IconButton>
                                        </InputAdornment>
                                    ),
                                }}
                                inputProps={{ "data-testid": "auth-register-confirm" }}
                            />

                            <Button
                                type="submit"
                                variant="contained"
                                size="large"
                                fullWidth
                                disabled={formik.isSubmitting || !formik.isValid}
                                data-testid="auth-register-submit"
                                sx={{ py: 1.5 }}
                            >
                                {formik.isSubmitting ? <CircularProgress size={24} color="inherit" /> : "Kayıt Ol"}
                            </Button>

                            <Typography variant="body2" textAlign="center">
                                Zaten hesabın var mı? <Link to="/auth/login" style={{ fontWeight: 700 }}>Giriş Yap</Link>
                            </Typography>
                        </Stack>
                    </form>
                </CardContent>
            </Card>
        </Stack>
    );
}