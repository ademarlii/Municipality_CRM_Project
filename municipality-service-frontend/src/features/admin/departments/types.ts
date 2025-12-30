export type DepartmentResponse = { id: number; name: string; active: boolean };

export type DepartmentUpsertRequest = { name: string; active?: boolean | null };

export type DepartmentMemberResponse = {
    userId: number;
    email: string;
    phone: string;
    memberRole: string;
    active: boolean;
};

export type AddDepartmentMemberRequest = {
    userId: number;
    memberRole?: string | null;
};
