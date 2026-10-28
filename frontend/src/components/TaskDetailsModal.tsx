import { useEffect, useState } from 'react';
import { Trash2 } from 'lucide-react';
import { deleteTask, getTask, updateTaskStatus } from '../api/tasks';
import type { Task, TaskStatus } from '../api/types';
import { formatDateTime, isOverdue } from '../format';
import { Modal } from './Modal';
import { StatusSelect } from './StatusSelect';

interface TaskDetailsModalProps {
  taskId: number;
  onClose: () => void;
  onChanged: () => void;
}

export function TaskDetailsModal({ taskId, onClose, onChanged }: TaskDetailsModalProps) {
  const [task, setTask] = useState<Task | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getTask(taskId)
      .then((loaded) => !cancelled && setTask(loaded))
      .catch((err: Error) => !cancelled && setError(err.message));
    return () => {
      cancelled = true;
    };
  }, [taskId]);

  async function changeStatus(status: TaskStatus) {
    setBusy(true);
    setError(null);
    try {
      setTask(await updateTaskStatus(taskId, status));
      onChanged();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete() {
    setBusy(true);
    setError(null);
    try {
      await deleteTask(taskId);
      onChanged();
      onClose();
    } catch (err) {
      setError((err as Error).message);
      setBusy(false);
    }
  }

  const footer = task && (
    confirmingDelete ? (
      <>
        <span className="footer-note">Delete this task permanently?</span>
        <button type="button" className="button" onClick={() => setConfirmingDelete(false)} disabled={busy}>
          Keep
        </button>
        <button type="button" className="button button--danger" onClick={handleDelete} disabled={busy}>
          Delete task
        </button>
      </>
    ) : (
      <>
        <button type="button" className="button button--ghost-danger" onClick={() => setConfirmingDelete(true)}>
          <Trash2 size={16} /> Delete
        </button>
        <button type="button" className="button" onClick={onClose}>
          Close
        </button>
      </>
    )
  );

  return (
    <Modal title={task?.title ?? 'Task'} onClose={onClose} footer={footer}>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      {!task && !error && <p className="muted">Loading…</p>}
      {task && (
        <dl className="details">
          <dt>Task ID</dt>
          <dd className="task-id">{task.id}</dd>
          <dt>Status</dt>
          <dd>
            <StatusSelect status={task.status} onChange={changeStatus} disabled={busy} label="Change status" />
          </dd>
          <dt>Due</dt>
          <dd className={isOverdue(task) ? 'overdue' : undefined}>
            {formatDateTime(task.dueDate)}
            {isOverdue(task) && <span className="overdue-tag">Overdue</span>}
          </dd>
          <dt>Description</dt>
          <dd className={task.description ? 'description' : 'muted'}>{task.description || 'No description'}</dd>
          <dt>Created</dt>
          <dd className="muted">{formatDateTime(task.createdAt)}</dd>
          <dt>Updated</dt>
          <dd className="muted">{formatDateTime(task.updatedAt)}</dd>
        </dl>
      )}
    </Modal>
  );
}
