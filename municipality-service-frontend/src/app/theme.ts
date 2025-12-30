//src/app/theme.ts
import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
    palette: {
        primary: { main: "#0B4F6C" },
        secondary: { main: "#2A9D8F" },
        background: { default: "#F6F8FB" },
    },
    shape: { borderRadius: 14 },
    typography: {
        fontFamily: ["Inter", "system-ui", "Segoe UI", "Roboto"].join(","),
    },
    components: {
        MuiCard: { styleOverrides: { root: { borderRadius: 16 } } },
        MuiButton: { styleOverrides: { root: { borderRadius: 12, textTransform: "none", fontWeight: 700 } } },
    },
});
