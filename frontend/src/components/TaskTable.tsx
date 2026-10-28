import { useEffect, useState } from 'react';
import { ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';
import { getTasksPage, updateTaskStatus } from '../api/tasks';
import type { PagedResponse, Task, TaskStatus } from '../api/types';
import { formatDate, isOverdue } from '../format';
import { StatusIcon } from './StatusIcon';
import { StatusSelect } from './StatusSelect';

export const PAGE_SIZE = 10;

interface TaskTableProps {
  version: number;
  onOpenTask: (id: number) => void;
  onChanged: () => void;
}

export function TaskTable({ version, onOpenTask, onChanged }: TaskTableProps) {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PagedResponse<Task> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    getTasksPage(page, PAGE_SIZE)
      .then((result) => {
        if (cancelled) return;
        if (result.content.length === 0 && page > 0) setPage(Math.max(0, result.totalPages - 1));
        else setData(result);
      })
      .catch((err: Error) => !cancelled && setError(err.message));
    return () => {
      cancelled = true;
    };
  }, [page, version]);

  async function changeStatus(task: Task, status: TaskStatus) {
    setUpdatingId(task.id);
    setError(null);
    try {
      await updateTaskStatus(task.id, status);
      onChanged();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setUpdatingId(null);
    }
  }

  const first = data && data.totalElements > 0 ? data.page * data.size + 1 : 0;
  const last = data ? data.page * data.size + data.content.length : 0;

  return (
    <>
      {error && (
        <p className="alert alert--inline" role="alert">
          {error}
        </p>
      )}

      <div className="table-scroll">
        <table className="task-table">
          <thead>
            <tr>
              <th scope="col" className="col-id">ID</th>
              <th scope="col" className="col-name">Task name</th>
              <th scope="col" className="col-status">Status</th>
              <th scope="col" className="col-due">
                <span className="th-sorted" title="Sorted by due date, earliest first">
                  Due date <ChevronDown size={14} aria-hidden="true" />
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((task) => (
              <tr key={task.id} className="task-row" onClick={() => onOpenTask(task.id)}>
                <td className="col-id">{task.id}</td>
                <td className="col-name">
                  <button type="button" className="task-name">
                    <StatusIcon status={task.status} className="task-icon" />
                    <span className="task-text">
                      <span className="task-title">{task.title}</span>
                      <span className={`task-due-inline${isOverdue(task) ? ' overdue' : ''}`}>
                        Due {formatDate(task.dueDate)}
                      </span>
                    </span>
                  </button>
                </td>
                <td className="col-status">
                  <StatusSelect
                    status={task.status}
                    onChange={(status) => changeStatus(task, status)}
                    disabled={updatingId === task.id}
                    label={`Status of ${task.title}`}
                  />
                </td>
                <td className={`col-due${isOverdue(task) ? ' overdue' : ''}`}>
                  {formatDate(task.dueDate)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalElements === 0 && (
        <p className="empty-state">No tasks yet. Create one to get started.</p>
      )}
      {!data && !error && <p className="empty-state">Loading tasks…</p>}

      {data && data.totalElements > 0 && (
        <nav className="pagination" aria-label="Pagination">
          <span className="muted">
            Showing {first}–{last} of {data.totalElements}
          </span>
          <div className="pagination-controls">
            <button
              type="button"
              className="button button--small"
              onClick={() => setPage((p) => p - 1)}
              disabled={data.page === 0}
            >
              <ChevronLeft size={16} /> Previous
            </button>
            <span className="page-indicator">
              Page {data.page + 1} of {data.totalPages}
            </span>
            <button
              type="button"
              className="button button--small"
              onClick={() => setPage((p) => p + 1)}
              disabled={data.page + 1 >= data.totalPages}
            >
              Next <ChevronRight size={16} />
            </button>
          </div>
        </nav>
      )}
    </>
  );
}
