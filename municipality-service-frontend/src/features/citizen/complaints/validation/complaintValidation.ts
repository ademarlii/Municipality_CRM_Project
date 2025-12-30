import * as Yup from "yup";

export const createComplaintValidationSchema = Yup.object({
    departmentId: Yup.number()
        .required("Departman seçimi zorunludur")
        .min(1, "Lütfen bir departman seçin"),
    categoryId: Yup.number()
        .required("Kategori seçimi zorunludur")
        .min(1, "Lütfen bir kategori seçin"),
    title: Yup.string()
        .required("Başlık zorunludur")
        .min(10, "Başlık en az 10 karakter olmalıdır")
        .max(200, "Başlık en fazla 200 karakter olabilir"),
    description: Yup.string()
        .max(2000, "Açıklama en fazla 2000 karakter olabilir"),
});

export type CreateComplaintFormValues = {
    departmentId: number | "";
    categoryId: number | "";
    title: string;
    description: string;
};

export const createComplaintInitialValues: CreateComplaintFormValues = {
    departmentId: "",
    categoryId: "",
    title: "",
    description: "",
};