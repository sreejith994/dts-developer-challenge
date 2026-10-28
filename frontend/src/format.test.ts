import { describe, expect, it } from 'vitest';
import { isOverdue } from './format';

const now = new Date('2026-06-01T12:00:00Z');

describe('isOverdue', () => {
  it('is true for an open task past its due date', () => {
    expect(isOverdue({ status: 'INPROGRESS', dueDate: '2026-05-31T12:00:00Z' }, now)).toBe(true);
  });

  it('is false for a task due in the future', () => {
    expect(isOverdue({ status: 'CREATED', dueDate: '2026-06-02T12:00:00Z' }, now)).toBe(false);
  });

  it('is false for completed or cancelled tasks', () => {
    expect(isOverdue({ status: 'COMPLETED', dueDate: '2026-05-31T12:00:00Z' }, now)).toBe(false);
    expect(isOverdue({ status: 'CANCELLED', dueDate: '2026-05-31T12:00:00Z' }, now)).toBe(false);
  });
});
