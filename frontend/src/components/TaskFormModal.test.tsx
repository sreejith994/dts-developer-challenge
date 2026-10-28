import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { TaskFormModal, validate } from './TaskFormModal';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('validate', () => {
  const now = new Date('2026-06-01T12:00:00');

  it('requires a title and a future due date', () => {
    expect(validate('  ', '', '', now)).toEqual({ title: 'Title is required', dueDate: 'Due date is required' });
    expect(validate('Task', '', '2026-05-01T09:00', now)).toEqual({ dueDate: 'Due date must be in the future' });
  });

  it('accepts a valid task', () => {
    expect(validate('Task', 'Details', '2026-07-01T09:00', now)).toEqual({});
  });
});

describe('TaskFormModal', () => {
  it('shows validation errors without calling the API', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    render(<TaskFormModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Create task' }));

    expect(screen.getByText('Title is required')).toBeInTheDocument();
    expect(screen.getByText('Due date is required')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('has no status field because new tasks always start as to-do', () => {
    render(<TaskFormModal onClose={vi.fn()} onCreated={vi.fn()} />);

    expect(screen.queryByLabelText('Status')).not.toBeInTheDocument();
  });

  it('creates the task and notifies the parent', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, status: 201, json: async () => ({ id: 1 }) });
    vi.stubGlobal('fetch', fetchMock);
    const onCreated = vi.fn();
    render(<TaskFormModal onClose={vi.fn()} onCreated={onCreated} />);

    await userEvent.type(screen.getByLabelText('Title'), '  Review bundle  ');
    await userEvent.type(screen.getByLabelText('Due date'), '2099-01-15T10:30');
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }));

    expect(onCreated).toHaveBeenCalled();
    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body).toEqual({
      title: 'Review bundle',
      dueDate: new Date('2099-01-15T10:30').toISOString(),
    });
  });

  it('shows field errors returned by the server', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ detail: 'Request validation failed', errors: { title: 'Title must be unique' } }),
    }));
    render(<TaskFormModal onClose={vi.fn()} onCreated={vi.fn()} />);

    await userEvent.type(screen.getByLabelText('Title'), 'Review bundle');
    await userEvent.type(screen.getByLabelText('Due date'), '2099-01-15T10:30');
    await userEvent.click(screen.getByRole('button', { name: 'Create task' }));

    expect(await screen.findByText('Title must be unique')).toBeInTheDocument();
  });
});
