// ============================================
// src/shared/components/Footer.tsx
// ============================================
import { Box, Container, Divider, Link, Stack, Typography } from "@mui/material";
import { Email, Phone, LocationOn } from "@mui/icons-material";

export default function Footer() {
    const currentYear = new Date().getFullYear();

    return (
        <Box
            component="footer"
            sx={{
                mt: 8,
                py: 4,
                bgcolor: "grey.900",
                color: "white",
            }}
        >
            <Container maxWidth="lg">
                <Stack spacing={4}>
                    {/* Main Content */}
                    <Stack
                        direction={{ xs: "column", md: "row" }}
                        spacing={4}
                        justifyContent="space-between"
                    >
                        {/* About */}
                        <Stack spacing={2} flex={1}>
                            <Typography variant="h6" fontWeight={900}>
                                Arnavutköy Belediyesi
                            </Typography>
                            <Typography variant="body2" sx={{ opacity: 0.8, maxWidth: 400 }}>
                                Vatandaşlarımızın şikayet ve taleplerini hızlı ve etkin bir şekilde çözüme kavuşturmak için buradayız.
                            </Typography>
                        </Stack>

                        {/* Quick Links */}
                        <Stack spacing={2}>
                            <Typography variant="subtitle2" fontWeight={700}>
                                Hızlı Erişim
                            </Typography>
                            <Stack spacing={1}>
                                <Link href="/" color="inherit" underline="hover" sx={{ opacity: 0.8 }}>
                                    Ana Sayfa
                                </Link>
                                <Link href="/auth/login" color="inherit" underline="hover" sx={{ opacity: 0.8 }}>
                                    Giriş Yap
                                </Link>
                                <Link href="/auth/register" color="inherit" underline="hover" sx={{ opacity: 0.8 }}>
                                    Kayıt Ol
                                </Link>
                                <Link href="/citizen/track" color="inherit" underline="hover" sx={{ opacity: 0.8 }}>
                                    Şikayet Takip
                                </Link>
                            </Stack>
                        </Stack>

                        {/* Contact */}
                        <Stack spacing={2}>
                            <Typography variant="subtitle2" fontWeight={700}>
                                İletişim
                            </Typography>
                            <Stack spacing={1}>
                                <Stack direction="row" spacing={1} alignItems="center" sx={{ opacity: 0.8 }}>
                                    <Phone fontSize="small" />
                                    <Typography variant="body2">153 (ALO Belediye)</Typography>
                                </Stack>
                                <Stack direction="row" spacing={1} alignItems="center" sx={{ opacity: 0.8 }}>
                                    <Email fontSize="small" />
                                    <Typography variant="body2">info@arnavutkoy.bel.tr</Typography>
                                </Stack>
                                <Stack direction="row" spacing={1} alignItems="flex-start" sx={{ opacity: 0.8 }}>
                                    <LocationOn fontSize="small" />
                                    <Typography variant="body2">
                                        Arnavutköy, İstanbul
                                    </Typography>
                                </Stack>
                            </Stack>
                        </Stack>
                    </Stack>

                    <Divider sx={{ borderColor: "rgba(255, 255, 255, 0.1)" }} />

                    {/* Bottom Bar */}
                    <Stack
                        direction={{ xs: "column", sm: "row" }}
                        justifyContent="space-between"
                        alignItems="center"
                        spacing={2}
                    >
                        <Typography variant="body2" sx={{ opacity: 0.7 }}>
                            © {currentYear} Arnavutköy Belediyesi. Tüm hakları saklıdır.
                        </Typography>
                        <Stack direction="row" spacing={2}>
                            <Link href="#" color="inherit" underline="hover" sx={{ opacity: 0.7 }}>
                                <Typography variant="body2">Gizlilik Politikası</Typography>
                            </Link>
                            <Link href="#" color="inherit" underline="hover" sx={{ opacity: 0.7 }}>
                                <Typography variant="body2">Kullanım Koşulları</Typography>
                            </Link>
                        </Stack>
                    </Stack>
                </Stack>
            </Container>
        </Box>
    );
}