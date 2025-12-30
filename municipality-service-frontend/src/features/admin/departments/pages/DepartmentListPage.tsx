// ============================================
// 5. src/features/admin/departments/pages/DepartmentListPage.tsx
// ============================================
import { useEffect, useState } from "react";
import { Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Switch, TextField, Typography, Chip, IconButton, Box } from "@mui/material";
import { Add, Edit, Delete, Business, Group } from "@mui/icons-material";
import { Link as RouterLink } from "react-router-dom";
import { useFormik } from "formik";
import Loading from "../../../../shared/components/Loading";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import { createDepartment, deleteDepartment, listDepartments, updateDepartment } from "../api";
import type { DepartmentResponse } from "../types";
import { departmentValidationSchema, departmentInitialValues, type DepartmentFormValues } from "../validation/departmentValidation";

export default function DepartmentListPage() {
    const toast = useToast();
    const [items, setItems] = useState<DepartmentResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [open, setOpen] = useState(false);
    const [editing, setEditing] = useState<DepartmentResponse | null>(null);

    const formik = useFormik<DepartmentFormValues>({
        initialValues: departmentInitialValues,
        validationSchema: departmentValidationSchema,
        onSubmit: async (values) => {
            try {
                if (editing) {
                    await updateDepartment(editing.id, { name: values.name.trim(), active: values.active });
                    toast.success("Departman güncellendi.");
                } else {
                    await createDepartment({ name: values.name.trim(), active: values.active });
                    toast.success("Departman oluşturuldu.");
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
            const res = await listDepartments(0, 100);
            setItems(res.data.content || []);
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Departmanlar yüklenemedi."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const openCreate = () => {
        setEditing(null);
        formik.resetForm();
        setOpen(true);
    };

    const openEdit = (d: DepartmentResponse) => {
        setEditing(d);
        formik.setValues({ name: d.name, active: !!d.active });
        setOpen(true);
    };

    const remove = async (id: number) => {
        if (!confirm("Bu departmanı pasif etmek istediğinizden emin misiniz?")) return;
        try {
            await deleteDepartment(id);
            toast.success("Departman pasif edildi.");
            load();
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Silinemedi."));
        }
    };

    return (
        <Stack spacing={3}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                    <Typography variant="h4" fontWeight={900}>Departmanlar</Typography>
                    <Typography variant="body2" sx={{ opacity: 0.7, mt: 0.5 }}>Departmanları ve üyelerini yönetin</Typography>
                </Box>
                <Button variant="contained" startIcon={<Add />} onClick={openCreate} data-testid="admin-dept-new">
                    Yeni Departman
                </Button>
            </Stack>

            {loading && <Loading />}

            {!loading && (
                <Stack spacing={2}>
                    {items.map((d) => (
                        <Card
                            key={d.id}
                            elevation={0}
                            sx={{ border: "1px solid #e5e7eb" }}
                            data-testid={`dept-card-${d.id}`}
                        >
                            <CardContent>
                                <Stack direction="row" justifyContent="space-between" alignItems="center">
                                    <Stack direction="row" spacing={2} alignItems="center" flex={1}>
                                        <Business color="primary" sx={{ fontSize: 32 }} />
                                        <Stack>
                                            <Typography fontWeight={900} data-testid={`dept-name-${d.id}`}>
                                                {d.name}
                                            </Typography>

                                            <Chip
                                                data-testid={`dept-status-${d.id}`}
                                                label={d.active ? "Aktif" : "Pasif"}
                                                size="small"
                                                color={d.active ? "success" : "default"}
                                            />
                                        </Stack>
                                    </Stack>

                                    <Stack direction="row" spacing={1}>
                                        <Button
                                            component={RouterLink}
                                            to={`/admin/departments/${d.id}/members`}
                                            variant="contained"
                                            startIcon={<Group />}
                                            size="small"
                                            data-testid={`dept-members-${d.id}`}
                                        >
                                            Üyeler
                                        </Button>

                                        <IconButton
                                            color="primary"
                                            onClick={() => openEdit(d)}
                                            data-testid={`dept-edit-${d.id}`}
                                        >
                                            <Edit />
                                        </IconButton>

                                        <IconButton
                                            color="error"
                                            onClick={() => remove(d.id)}
                                            data-testid={`dept-delete-${d.id}`}
                                        >
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
                <DialogTitle>{editing ? "Departman Güncelle" : "Yeni Departman"}</DialogTitle>
                <form onSubmit={formik.handleSubmit}>
                    <DialogContent>
                        <Stack spacing={3} sx={{ pt: 1 }}>
                            <TextField
                                id={"department-name-input"}
                                name="name"
                                label="Departman Adı"
                                value={formik.values.name}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.name && Boolean(formik.errors.name)}
                                helperText={formik.touched.name && formik.errors.name}
                            />
                            <Stack direction="row" alignItems="center" spacing={1}>
                                <Switch name="active" checked={formik.values.active} onChange={formik.handleChange}  className={"department-active-checkbox"}/>
                                <Typography>Aktif</Typography>
                            </Stack>
                        </Stack>
                    </DialogContent>
                    <DialogActions>
                        <Button onClick={() => setOpen(false)}>İptal</Button>
                        <Button type="submit" variant="contained" disabled={formik.isSubmitting || !formik.isValid} className={"department-submit-button"}>
                            Kaydet
                        </Button>
                    </DialogActions>
                </form>
            </Dialog>
        </Stack>
    );
}