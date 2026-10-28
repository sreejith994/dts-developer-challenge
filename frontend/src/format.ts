import type { Task, TaskStatus } from './api/types';

export const STATUS_LABELS: Record<TaskStatus, string> = {
  CREATED: 'To-do',
  INPROGRESS: 'In progress',
  BLOCKED: 'Blocked',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const dateFormat = new Intl.DateTimeFormat('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

const timeFormat = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' });

export function formatDate(iso: string): string {
  return dateFormat.format(new Date(iso));
}

export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  return `${dateFormat.format(date)}, ${timeFormat.format(date)}`;
}

export function isOverdue(task: Pick<Task, 'dueDate' | 'status'>, now = new Date()): boolean {
  return task.status !== 'COMPLETED' && task.status !== 'CANCELLED' && new Date(task.dueDate) < now;
}
