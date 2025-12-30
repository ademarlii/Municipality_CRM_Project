// ============================================
// 4. src/features/admin/categories/pages/CategoryListPage.tsx
// ============================================
import { useEffect, useState } from "react";
import { Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Switch, TextField, Typography, MenuItem, Chip, IconButton, Box } from "@mui/material";
import { Add, Edit, Delete, Category as CategoryIcon } from "@mui/icons-material";
import { useFormik } from "formik";
import Loading from "../../../../shared/components/Loading";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import { createCategory, deleteCategory, listCategories, updateCategory } from "../api";
import type { CategoryResponse } from "../types";
import { listDepartments } from "../../departments/api";
import type { DepartmentResponse } from "../../departments/types";
import { categoryValidationSchema, categoryInitialValues, type CategoryFormValues } from "../validation/categoryValidation";

export default function CategoryListPage() {
    const toast = useToast();

    const [items, setItems] = useState<CategoryResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [open, setOpen] = useState(false);
    const [editing, setEditing] = useState<CategoryResponse | null>(null);
    const [departments, setDepartments] = useState<DepartmentResponse[]>([]);
    const [deptLoading, setDeptLoading] = useState(false);

    const formik = useFormik<CategoryFormValues>({
        initialValues: categoryInitialValues,
        validationSchema: categoryValidationSchema,
        onSubmit: async (values) => {
            try {
                if (editing) {
                    await updateCategory(editing.id, {
                        name: values.name.trim(),
                        defaultDepartmentId: values.defaultDepartmentId as number,
                        active: values.active,
                    });
                    toast.success("Kategori güncellendi.");
                } else {
                    await createCategory({
                        name: values.name.trim(),
                        defaultDepartmentId: values.defaultDepartmentId as number,
                        active: values.active,
                    });
                    toast.success("Kategori oluşturuldu.");
                }
                setOpen(false);
                load();
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Kaydedilemedi."));
            }
        },
    });

    const load = async () => {
        setLoading(true);
        try {
            const res = await listCategories(0, 100);
            setItems(res.data.content || []);
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Kategoriler yüklenemedi."));
        } finally {
            setLoading(false);
        }
    };

    const loadDepartmentsForDropdown = async () => {
        setDeptLoading(true);
        try {
            const res = await listDepartments(0, 1000);
            const activeOnly = (res.data.content || []).filter((d) => d.active);
            setDepartments(activeOnly);
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Departmanlar yüklenemedi."));
        } finally {
            setDeptLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const openCreate = () => {
        setEditing(null);
        formik.resetForm();
        setOpen(true);
    };

    const openEdit = (c: CategoryResponse) => {
        setEditing(c);
        formik.setValues({
            name: c.name,
            defaultDepartmentId: c.defaultDepartmentId || "",
            active: !!c.active,
        });
        setOpen(true);
    };

    useEffect(() => {
        if (open) loadDepartmentsForDropdown();
    }, [open]);

    const remove = async (id: number) => {
        if (!confirm("Bu kategoriyi pasif etmek istediğinizden emin misiniz?")) return;
        try {
            await deleteCategory(id);
            toast.success("Kategori pasif edildi.");
            load();
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Silinemedi."));
        }
    };

    return (
        <Stack spacing={3}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                    <Typography variant="h4" fontWeight={900}>Kategoriler</Typography>
                    <Typography variant="body2" sx={{ opacity: 0.7, mt: 0.5 }}>Şikayet kategorilerini yönetin</Typography>
                </Box>
                <Button variant="contained" startIcon={<Add />} onClick={openCreate} data-testid="admin-category-new">
                    Yeni Kategori
                </Button>
            </Stack>

            {loading && <Loading />}

            {!loading && (
                <Stack spacing={2}>
                    {items.map((c) => (
                        <Card key={c.id} elevation={0} sx={{ border: "1px solid #e5e7eb" }}>
                            <CardContent>
                                <Stack direction="row" justifyContent="space-between" alignItems="center">
                                    <Stack direction="row" spacing={2} alignItems="center" flex={1}>
                                        <CategoryIcon color="primary" sx={{ fontSize: 32 }} />
                                        <Stack>
                                            <Typography fontWeight={900}>{c.name}</Typography>
                                            <Stack direction="row" spacing={1} alignItems="center">
                                                <Typography variant="body2" sx={{ opacity: 0.7 }}>
                                                    {c.defaultDepartmentName || "-"}
                                                </Typography>
                                                <Chip label={c.active ? "Aktif" : "Pasif"} size="small" color={c.active ? "success" : "default"} />
                                            </Stack>
                                        </Stack>
                                    </Stack>

                                    <Stack direction="row" spacing={1}>
                                        <IconButton color="primary" onClick={() => openEdit(c)} data-testid={`admin-category-edit-${c.id}`}>
                                            <Edit />
                                        </IconButton>
                                        <IconButton color="error" onClick={() => remove(c.id)} data-testid={`admin-category-delete-${c.id}`}>
                                            <Delete />
                                        </IconButton>
                                    </Stack>
                                </Stack>
                            </CardContent>
                        </Card>
                    ))}
                </Stack>
            )}

            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
                <DialogTitle>{editing ? "Kategori Güncelle" : "Yeni Kategori"}</DialogTitle>
                <form onSubmit={formik.handleSubmit}>
                    <DialogContent>
                        <Stack spacing={3} sx={{ pt: 1 }}>
                            <TextField
                                fullWidth
                                name="name"
                                label="Kategori Adı"
                                value={formik.values.name}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.name && Boolean(formik.errors.name)}
                                helperText={formik.touched.name && formik.errors.name}
                                inputProps={{ "data-testid": "admin-category-name" }}
                            />

                            <TextField
                                select
                                fullWidth
                                name="defaultDepartmentId"
                                label="Default Departman"
                                value={formik.values.defaultDepartmentId}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                disabled={deptLoading || departments.length === 0}
                                error={formik.touched.defaultDepartmentId && Boolean(formik.errors.defaultDepartmentId)}
                                helperText={
                                    formik.touched.defaultDepartmentId
                                        ? formik.errors.defaultDepartmentId
                                        : deptLoading
                                            ? "Departmanlar yükleniyor..."
                                            : departments.length === 0
                                                ? "Aktif departman bulunamadı."
                                                : "Kategori bu departmana yönlendirilecek."
                                }
                                SelectProps={{
                                    SelectDisplayProps: { className: "admin-category-dropdown-department" },
                                }}
                            >
                                {departments.map((d) => (
                                    <MenuItem key={d.id} value={d.id} className={"data-testid-"+d.id}>
                                        {d.name}
                                    </MenuItem>
                                ))}
                            </TextField>

                            <Stack direction="row" alignItems="center" spacing={1}>
                                <Switch
                                    name="active"
                                    checked={formik.values.active}
                                    onChange={formik.handleChange}
                                    className={"admin-category-status-switch"}
                                />
                                <Typography>Aktif</Typography>
                            </Stack>
                        </Stack>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpen(false)}>İptal</Button>
                        <Button type="submit" variant="contained" disabled={formik.isSubmitting || !formik.isValid} data-testid="admin-category-save">
                            Kaydet
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>
        </Stack>
    );
}