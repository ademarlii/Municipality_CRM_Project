// //src/api/client.ts
// import axios from "axios";
// import { getToken, clearToken } from "../auth/authStore.ts";
// import { toastError } from "../utils/toast.ts";
//
// const api = axios.create({
//     baseURL: import.meta.env.VITE_API_BASE_URL,
// });
//
// api.interceptors.request.use((config) => {
//     const token = getToken();
//     if (token) {
//         config.headers.Authorization = `Bearer ${token}`;
//     }
//     return config;
// });
//
// api.interceptors.response.use(
//     (res) => res,
//     (err) => {
//         if (err.response?.status === 401) {
//             clearToken();
//             toastError("Oturum süren doldu. Lütfen tekrar giriş yap.");
//             window.location.href = "/login";
//         }
//         return Promise.reject(err);
//     }
// );
//
// export default api;
//
//
