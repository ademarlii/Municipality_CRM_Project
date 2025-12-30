import * as Yup from "yup";

export const trackComplaintValidationSchema = Yup.object({
    trackingCode: Yup.string()
        .required("Takip kodu gerekli")
        .min(8, "Takip kodu en az 8 karakter olmalı")
        .max(50, "Takip kodu en fazla 50 karakter olabilir"),
});

export type TrackComplaintFormValues = {
    trackingCode: string;
};

export const trackComplaintInitialValues: TrackComplaintFormValues = {
    trackingCode: "",
};