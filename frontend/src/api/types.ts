export const TASK_STATUSES = ['CREATED', 'INPROGRESS', 'BLOCKED', 'COMPLETED', 'CANCELLED'] as const;

export type TaskStatus = (typeof TASK_STATUSES)[number];

export interface Task {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  dueDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  dueDate: string;
}
