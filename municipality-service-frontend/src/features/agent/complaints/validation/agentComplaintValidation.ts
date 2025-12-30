import * as Yup from "yup";

export const changeStatusValidationSchema = Yup.object({
    toStatus: Yup.string()
        .required("Durum seçimi gerekli")
        .oneOf(["NEW", "IN_REVIEW", "RESOLVED", "CLOSED"], "Geçerli bir durum seçin"),
    note: Yup.string()
        .max(1000, "Not en fazla 1000 karakter olabilir"),
    publicAnswer: Yup.string()
        .when("toStatus", {
            is: "RESOLVED",
            then: (schema) => schema
                .required("Çözüldü durumu için vatandaşa cevap zorunlu")
                .min(10, "Cevap en az 10 karakter olmalı"),
            otherwise: (schema) => schema,
        })
        .max(2000, "Cevap en fazla 2000 karakter olabilir"),
});

export type ChangeStatusFormValues = {
    toStatus: string;
    note: string;
    publicAnswer: string;
};

export const changeStatusInitialValues: ChangeStatusFormValues = {
    toStatus: "IN_REVIEW",
    note: "",
    publicAnswer: "",
};