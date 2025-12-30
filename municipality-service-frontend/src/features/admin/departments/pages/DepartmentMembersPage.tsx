// ============================================
// 6. src/features/admin/departments/pages/DepartmentMembersPage.tsx
// ============================================
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Button, Card, CardContent, MenuItem, Stack, TextField, Typography, Chip, IconButton, Alert, Box } from "@mui/material";
import { PersonAdd, SwapHoriz, Delete } from "@mui/icons-material";
import { useFormik } from "formik";
import Loading from "../../../../shared/components/Loading";
import { useToast } from "../../../../shared/utils/toast";
import { getErrorMessage } from "../../../../shared/api/error";
import { addMember, changeMemberRole, listMembers, removeMember } from "../api";
import type { DepartmentMemberResponse } from "../types";
import { memberValidationSchema, memberInitialValues, type MemberFormValues } from "../validation/departmentValidation";

const ROLES = ["MEMBER", "MANAGER"];

export default function DepartmentMembersPage() {
    const { deptId } = useParams();
    const id = Number(deptId);
    const toast = useToast();

    const [items, setItems] = useState<DepartmentMemberResponse[]>([]);
    const [loading, setLoading] = useState(true);

    const formik = useFormik<MemberFormValues>({
        initialValues: memberInitialValues,
        validationSchema: memberValidationSchema,
        onSubmit: async (values) => {
            try {
                await addMember(id, { userId: values.userId as number, memberRole: values.memberRole });
                toast.success("Üye eklendi.");
                formik.resetForm();
                load();
            } catch (err: any) {
                toast.error(getErrorMessage(err, "Üye eklenemedi."));
            }
        },
    });

    const load = async () => {
        setLoading(true);
        try {
            const res = await listMembers(id);
            setItems(res.data || []);
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Üyeler yüklenemedi."));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { if (id) load(); }, [id]);

    const remove = async (uid: number) => {
        if (!confirm("Bu üyeyi departmandan çıkarmak istediğinizden emin misiniz?")) return;
        try {
            await removeMember(id, uid);
            toast.success("Üye pasif edildi.");
            load();
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Üye silinemedi."));
        }
    };

    const changeRole = async (uid: number, role: string) => {
        try {
            await changeMemberRole(id, uid, role);
            toast.success("Rol güncellendi.");
            load();
        } catch (err: any) {
            toast.error(getErrorMessage(err, "Rol güncellenemedi."));
        }
    };

    return (
        <Stack spacing={3}>
            <Box>
                <Typography variant="h4" fontWeight={900}>Departman Üyeleri</Typography>
                <Typography variant="body2" sx={{ opacity: 0.7, mt: 0.5 }}>Departman: #{id}</Typography>
            </Box>

            <Card elevation={0} sx={{ border: "2px solid", borderColor: "primary.main", bgcolor: "primary.50" }}>
                <CardContent>
                    <form onSubmit={formik.handleSubmit}>
                        <Stack spacing={2}>
                            <Stack direction="row" spacing={1} alignItems="center">
                                <PersonAdd color="primary" />
                                <Typography fontWeight={900}>Yeni Üye Ekle</Typography>
                            </Stack>

                            <Alert severity="info">
                                Sadece AGENT rolüne sahip kullanıcılar departmana eklenebilir.
                            </Alert>

                            <TextField
                                fullWidth
                                name="userId"
                                label="Kullanıcı ID"
                                type="number"
                                value={formik.values.userId}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.userId && Boolean(formik.errors.userId)}
                                helperText={formik.touched.userId && formik.errors.userId}
                                inputProps={{ "data-testid": "admin-member-userId" }}
                            />

                            <TextField
                                select
                                fullWidth
                                name="memberRole"
                                label="Rol"
                                value={formik.values.memberRole}
                                onChange={formik.handleChange}
                                onBlur={formik.handleBlur}
                                error={formik.touched.memberRole && Boolean(formik.errors.memberRole)}
                                helperText={formik.touched.memberRole && formik.errors.memberRole}
                                inputProps={{ "data-testid": "admin-member-role" }}
                            >
                                {ROLES.map((r) => (
                                    <MenuItem key={r} value={r}>{r}</MenuItem>
                                ))}
                            </TextField>

                            <Button type="submit" variant="contained" disabled={formik.isSubmitting || !formik.isValid} data-testid="admin-member-add" startIcon={<PersonAdd />}>
                                Üye Ekle
                            </Button>
                        </Stack>
                    </form>
                </CardContent>
            </Card>

            {loading && <Loading />}

            {!loading && (
                <Stack spacing={2}>
                    {items.map((m) => (
                        <Card key={m.userId} elevation={0} sx={{ border: "1px solid #e5e7eb" }}>
                            <CardContent>
                                <Stack direction="row" justifyContent="space-between" alignItems="center">
                                    <Stack>
                                        <Typography fontWeight={900}>{m.email}</Typography>
                                        <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                                            <Typography variant="body2" sx={{ opacity: 0.7 }}>{m.phone}</Typography>
                                            <Chip label={m.memberRole} size="small" color="primary" />
                                            <Chip label={m.active ? "Aktif" : "Pasif"} size="small" color={m.active ? "success" : "default"} />
                                        </Stack>
                                    </Stack>

                                    <Stack direction="row" spacing={1}>
                                        <Button
                                            variant="outlined"
                                            size="small"
                                            startIcon={<SwapHoriz />}
                                            onClick={() => changeRole(m.userId, m.memberRole === "MEMBER" ? "MANAGER" : "MEMBER")}
                                        >
                                            {m.memberRole === "MEMBER" ? "Manager Yap" : "Member Yap"}
                                        </Button>
                                        <IconButton color="error" onClick={() => remove(m.userId)}>
                                            <Delete />
                                        </IconButton>
                                    </Stack>
                                </Stack>
                            </CardContent>
                        </Card>
                    ))}
                </Stack>
            )}
        </Stack>
    );
}