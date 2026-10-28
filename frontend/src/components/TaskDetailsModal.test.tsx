import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Task } from '../api/types';
import { TaskDetailsModal } from './TaskDetailsModal';

const TASK: Task = {
  id: 42,
  title: 'Review bundle',
  description: 'Check evidence',
  status: 'CREATED',
  dueDate: '2099-05-12T09:00:00Z',
  createdAt: '2026-01-01T09:00:00Z',
  updatedAt: '2026-01-01T09:00:00Z',
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('TaskDetailsModal', () => {
  it('shows the task id alongside its details', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => TASK });
    vi.stubGlobal('fetch', fetchMock);

    render(<TaskDetailsModal taskId={TASK.id} onClose={vi.fn()} onChanged={vi.fn()} />);

    expect(await screen.findByText('42')).toBeInTheDocument();
    expect(screen.getByText('Task ID')).toBeInTheDocument();
    expect(screen.getByText('Check evidence')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(`/api/tasks/${TASK.id}`, expect.anything());
  });
});
