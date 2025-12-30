import * as Yup from "yup";

export const loginValidationSchema = Yup.object({
    emailOrPhone: Yup.string()
        .required("E-posta veya telefon gerekli")
        .min(3, "En az 3 karakter olmalı"),
    password: Yup.string()
        .required("Şifre gerekli")
        .min(6, "Şifre en az 6 karakter olmalı"),
});

export type LoginFormValues = {
    emailOrPhone: string;
    password: string;
};

export const loginInitialValues: LoginFormValues = {
    emailOrPhone: "",
    password: "",
};

export const registerValidationSchema = Yup.object({
    email: Yup.string()
        .required("E-posta gerekli")
        .email("Geçerli bir e-posta adresi girin"),
    phone: Yup.string()
        .required("Telefon numarası gerekli")
        .matches(/^[0-9]{10}$/, "Telefon numarası 10 haneli olmalı (5XXXXXXXXX)"),
    password: Yup.string()
        .required("Şifre gerekli")
        .min(6, "Şifre en az 6 karakter olmalı")
        .matches(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/,
            "Şifre en az 1 büyük harf, 1 küçük harf ve 1 rakam içermeli"
        ),
    confirmPassword: Yup.string()
        .required("Şifre tekrarı gerekli")
        .oneOf([Yup.ref("password")], "Şifreler eşleşmiyor"),
});

export type RegisterFormValues = {
    email: string;
    phone: string;
    password: string;
    confirmPassword: string;
};

export const registerInitialValues: RegisterFormValues = {
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
};