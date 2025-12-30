//src/app/App.tsx
import { CssBaseline } from "@mui/material";
import { ThemeProvider } from "@mui/material/styles";
import { RouterProvider } from "react-router-dom";
import { theme } from "./theme";
import { router } from "./router";
import { ToastProvider } from "../shared/utils/toast";

export default function App() {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <ToastProvider>
                <RouterProvider router={router} />
            </ToastProvider>
        </ThemeProvider>
    );
}
