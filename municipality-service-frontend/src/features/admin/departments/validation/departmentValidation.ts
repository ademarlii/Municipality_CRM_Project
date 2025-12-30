import * as Yup from "yup";

export const departmentValidationSchema = Yup.object({
    name: Yup.string()
        .required("Departman adı gerekli")
        .min(3, "En az 3 karakter olmalı")
        .max(100, "En fazla 100 karakter olabilir"),
    active: Yup.boolean(),
});

export type DepartmentFormValues = {
    name: string;
    active: boolean;
};

export const departmentInitialValues: DepartmentFormValues = {
    name: "",
    active: true,
};

export const memberValidationSchema = Yup.object({
    userId: Yup.number()
        .required("Kullanıcı ID gerekli")
        .min(1, "Geçerli bir kullanıcı ID girin"),
    memberRole: Yup.string()
        .required("Rol seçimi gerekli")
        .oneOf(["MEMBER", "MANAGER"], "Geçerli bir rol seçin"),
});

export type MemberFormValues = {
    userId: number | "";
    memberRole: string;
};

export const memberInitialValues: MemberFormValues = {
    userId: "",
    memberRole: "MEMBER",
};