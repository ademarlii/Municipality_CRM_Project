export type CategoryResponse = {
    id: number;
    name: string;
    active: boolean;
    defaultDepartmentId?: number;
    defaultDepartmentName?: string;
};

export type CategoryUpsertRequest = {
    name: string;
    defaultDepartmentId: number;
    active?: boolean | null;
};
