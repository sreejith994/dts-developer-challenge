import type { CreateTaskRequest, PagedResponse, Task, TaskStatus } from './types';

const BASE_URL = '/api/tasks';

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
    });
  } catch {
    throw new ApiError(0, 'Could not reach the server. Check the backend is running.');
  }

  if (!response.ok) {
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = await response.json();
    return new ApiError(
      response.status,
      problem.detail ?? problem.title ?? `Request failed (${response.status})`,
      problem.errors ?? {},
    );
  } catch {
    return new ApiError(response.status, `Request failed (${response.status})`);
  }
}

export function getTasksPage(page: number, size: number): Promise<PagedResponse<Task>> {
  return request(`?page=${page}&size=${size}`);
}

export function getTask(id: number): Promise<Task> {
  return request(`/${id}`);
}

export async function createTask(task: CreateTaskRequest): Promise<number> {
  const { id } = await request<{ id: number }>('', { method: 'POST', body: JSON.stringify(task) });
  return id;
}

export function updateTaskStatus(id: number, status: TaskStatus): Promise<Task> {
  return request(`/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
}

export function deleteTask(id: number): Promise<void> {
  return request(`/${id}`, { method: 'DELETE' });
}
