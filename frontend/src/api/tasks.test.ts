import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createTask, deleteTask, getTasksPage } from './tasks';

function mockFetch(response: Partial<Response> | Error) {
  const fn = response instanceof Error ? vi.fn().mockRejectedValue(response) : vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fn);
  return fn;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('tasks api', () => {
  it('requests a page with page and size params', async () => {
    const page = { content: [], page: 1, size: 10, totalElements: 0, totalPages: 0 };
    const fetchMock = mockFetch({ ok: true, status: 200, json: async () => page });

    await expect(getTasksPage(1, 10)).resolves.toEqual(page);
    expect(fetchMock).toHaveBeenCalledWith('/api/tasks?page=1&size=10', expect.anything());
  });

  it('posts a new task as JSON and returns its id', async () => {
    const fetchMock = mockFetch({ ok: true, status: 201, json: async () => ({ id: 1 }) });

    await expect(createTask({ title: 'Review', dueDate: '2030-01-01T09:00:00.000Z' })).resolves.toBe(1);
    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe('POST');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(init.body)).toEqual({ title: 'Review', dueDate: '2030-01-01T09:00:00.000Z' });
  });

  it('handles 204 No Content on delete', async () => {
    mockFetch({ ok: true, status: 204 });

    await expect(deleteTask(1)).resolves.toBeUndefined();
  });

  it('turns problem details into an ApiError with field errors', async () => {
    mockFetch({
      ok: false,
      status: 400,
      json: async () => ({ title: 'Validation error', detail: 'Request validation failed', errors: { title: 'Title is required' } }),
    });

    const error = await createTask({ title: '', dueDate: '' }).catch((e) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.message).toBe('Request validation failed');
    expect(error.fieldErrors).toEqual({ title: 'Title is required' });
  });

  it('reports when the server cannot be reached', async () => {
    mockFetch(new TypeError('Failed to fetch'));

    await expect(getTasksPage(0, 10)).rejects.toThrow('Could not reach the server');
  });
});
