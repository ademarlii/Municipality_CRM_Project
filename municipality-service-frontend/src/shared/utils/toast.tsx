import React, { createContext, useContext, useMemo, useState } from "react";
import { Alert, IconButton, Snackbar } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";

type ToastKind = "success" | "error" | "info" | "warning";
type ToastState = { open: boolean; kind: ToastKind; message: string };

type ToastApi = {
    show: (kind: ToastKind, message: string) => void;
    success: (message: string) => void;
    error: (message: string) => void;
    info: (message: string) => void;
    warning: (message: string) => void;
};

const ToastCtx = createContext<ToastApi | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
    const [state, setState] = useState<ToastState>({
        open: false,
        kind: "info",
        message: "",
    });

    const close = () => setState((s) => ({ ...s, open: false }));

    const api = useMemo<ToastApi>(() => {
        const show = (kind: ToastKind, message: string) =>
            setState({ open: true, kind, message });

        return {
            show,
            success: (m: string) => show("success", m),
            error: (m: string) => show("error", m),
            info: (m: string) => show("info", m),
            warning: (m: string) => show("warning", m),
        };
    }, []);

    return (
        <ToastCtx.Provider value={api}>
            {children}

            <Snackbar
                open={state.open}
                autoHideDuration={3500}
                onClose={close}
                anchorOrigin={{ vertical: "top", horizontal: "right" }}
                data-testid="toast"
                sx={{
                    top: "80px !important",
                    right: "16px !important",
                }}
            >
                <Alert
                    severity={state.kind}
                    variant="filled"
                    data-testid={`toast-${state.kind}`}
                    data-message={state.message}
                    action={
                        <IconButton
                            size="small"
                            onClick={close}
                            aria-label="close"
                            data-testid="toast-close"
                            sx={{ color: "inherit" }}
                        >
                            <CloseIcon fontSize="small" />
                        </IconButton>
                    }
                >
                    {state.message}
                </Alert>
            </Snackbar>
        </ToastCtx.Provider>
    );
}

export function useToast() {
    const ctx = useContext(ToastCtx);
    if (!ctx) {
        throw new Error("useToast must be used inside <ToastProvider> (wrap App with ToastProvider).");
    }
    return ctx;
}
