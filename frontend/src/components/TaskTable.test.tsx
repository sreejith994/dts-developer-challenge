import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { PagedResponse, Task } from '../api/types';
import { TaskTable } from './TaskTable';

function task(id: number, title: string, overrides: Partial<Task> = {}): Task {
  return {
    id,
    title,
    description: null,
    status: 'CREATED',
    dueDate: '2099-05-12T09:00:00Z',
    createdAt: '2026-01-01T09:00:00Z',
    updatedAt: '2026-01-01T09:00:00Z',
    ...overrides,
  };
}

function page(content: Task[], pageNumber: number, totalElements: number): PagedResponse<Task> {
  return { content, page: pageNumber, size: 10, totalElements, totalPages: Math.ceil(totalElements / 10) };
}

function respondWith(...bodies: unknown[]) {
  const fetchMock = vi.fn();
  bodies.forEach((body) => fetchMock.mockResolvedValueOnce({ ok: true, status: 200, json: async () => body }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('TaskTable', () => {
  it('renders the first page of tasks with pagination', async () => {
    const fetchMock = respondWith(page([task(1, 'Review bundle'), task(2, 'Call applicant')], 0, 12));
    render(<TaskTable version={0} onOpenTask={vi.fn()} onChanged={vi.fn()} />);

    expect(await screen.findByText('Review bundle')).toBeInTheDocument();
    expect(screen.getByText('Call applicant')).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader')[0]).toHaveTextContent('ID');
    const firstRow = screen.getByText('Review bundle').closest('tr')!;
    expect(within(firstRow).getAllByRole('cell')[0]).toHaveTextContent('1');
    expect(screen.getByText('Showing 1–2 of 12')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Previous/ })).toBeDisabled();
    expect(fetchMock).toHaveBeenCalledWith('/api/tasks?page=0&size=10', expect.anything());
  });

  it('loads the next page', async () => {
    const fetchMock = respondWith(page([task(1, 'Review bundle')], 0, 11), page([task(11, 'Last task')], 1, 11));
    render(<TaskTable version={0} onOpenTask={vi.fn()} onChanged={vi.fn()} />);

    await userEvent.click(await screen.findByRole('button', { name: /Next/ }));

    expect(await screen.findByText('Last task')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith('/api/tasks?page=1&size=10', expect.anything());
    expect(screen.getByRole('button', { name: /Next/ })).toBeDisabled();
  });

  it('updates a task status from the table', async () => {
    const fetchMock = respondWith(page([task(1, 'Review bundle')], 0, 1), task(1, 'Review bundle', { status: 'COMPLETED' }));
    const onChanged = vi.fn();
    render(<TaskTable version={0} onOpenTask={vi.fn()} onChanged={onChanged} />);

    await userEvent.selectOptions(await screen.findByLabelText('Status of Review bundle'), 'COMPLETED');

    expect(fetchMock).toHaveBeenLastCalledWith('/api/tasks/1/status', expect.objectContaining({ method: 'PATCH' }));
    expect(onChanged).toHaveBeenCalled();
  });

  it('opens a task when its row is clicked', async () => {
    respondWith(page([task(1, 'Review bundle')], 0, 1));
    const onOpenTask = vi.fn();
    render(<TaskTable version={0} onOpenTask={onOpenTask} onChanged={vi.fn()} />);

    const row = (await screen.findByText('Review bundle')).closest('tr')!;
    await userEvent.click(within(row).getByRole('button', { name: /^Review bundle/ }));

    expect(onOpenTask).toHaveBeenCalledWith(1);
  });

  it('shows an empty state', async () => {
    respondWith(page([], 0, 0));
    render(<TaskTable version={0} onOpenTask={vi.fn()} onChanged={vi.fn()} />);

    expect(await screen.findByText(/No tasks yet/)).toBeInTheDocument();
  });

  it('shows an error when loading fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    render(<TaskTable version={0} onOpenTask={vi.fn()} onChanged={vi.fn()} />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the server');
  });
});
