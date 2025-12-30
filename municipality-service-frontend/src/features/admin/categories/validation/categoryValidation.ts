import * as Yup from "yup";

export const categoryValidationSchema = Yup.object({
    name: Yup.string()
        .required("Kategori adı gerekli")
        .min(3, "En az 3 karakter olmalı")
        .max(100, "En fazla 100 karakter olabilir"),
    defaultDepartmentId: Yup.number()
        .required("Default departman gerekli")
        .min(1, "Lütfen bir departman seçin"),
    active: Yup.boolean(),
});

export type CategoryFormValues = {
    name: string;
    defaultDepartmentId: number | "";
    active: boolean;
};

export const categoryInitialValues: CategoryFormValues = {
    name: "",
    defaultDepartmentId: "",
    active: true,
};