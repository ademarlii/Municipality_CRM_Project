// src/shared/components/Page.tsx
import { Box, Container } from "@mui/material";
import Footer from "./Footer";
import Header from "./Header";
import React from "react";

export default function Page({ children }: { children: React.ReactNode }) {
    return (
        <Box
            sx={{
                display: "flex",
                flexDirection: "column",
                minHeight: "100vh", // 👈 ÖNEMLİ: Minimum ekran yüksekliği
            }}
        >
            <Header />

            <Box sx={{ flex: 1 }}> {/* 👈 ÖNEMLİ: Kalan alanı kapla */}
                <Container maxWidth="lg" sx={{ py: 4 }}>
                    {children}
                </Container>
            </Box>

            <Footer />
        </Box>
    );
}