import { useState, type FormEvent } from 'react';
import { ApiError, createTask } from '../api/tasks';
import { Modal } from './Modal';

interface TaskFormModalProps {
  onClose: () => void;
  onCreated: () => void;
}

type Field = 'title' | 'description' | 'dueDate';

export function validate(title: string, description: string, dueDate: string, now = new Date()) {
  const errors: Partial<Record<Field, string>> = {};
  if (!title.trim()) errors.title = 'Title is required';
  else if (title.trim().length > 255) errors.title = 'Title must be at most 255 characters';
  if (description.length > 2000) errors.description = 'Description must be at most 2000 characters';
  if (!dueDate) errors.dueDate = 'Due date is required';
  else if (new Date(dueDate) <= now) errors.dueDate = 'Due date must be in the future';
  return errors;
}

export function TaskFormModal({ onClose, onCreated }: TaskFormModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const clientErrors = validate(title, description, dueDate);
    setErrors(clientErrors);
    setFormError(null);
    if (Object.keys(clientErrors).length > 0) return;

    setSaving(true);
    try {
      await createTask({
        title: title.trim(),
        description: description.trim() || undefined,
        dueDate: new Date(dueDate).toISOString(),
      });
      onCreated();
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
        setErrors(error.fieldErrors);
      } else {
        setFormError(error instanceof Error ? error.message : 'Something went wrong');
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      title="New task"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="button" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" form="task-form" className="button button--primary" disabled={saving}>
            {saving ? 'Creating…' : 'Create task'}
          </button>
        </>
      }
    >
      <form id="task-form" className="form" onSubmit={handleSubmit} noValidate>
        {formError && (
          <p className="alert" role="alert">
            {formError}
          </p>
        )}

        <div className="field">
          <label htmlFor="title">Title</label>
          <input
            id="title"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            aria-invalid={!!errors.title}
            aria-describedby={errors.title ? 'title-error' : undefined}
            autoFocus
          />
          {errors.title && <p id="title-error" className="field-error">{errors.title}</p>}
        </div>

        <div className="field">
          <label htmlFor="description">
            Description <span className="optional">(optional)</span>
          </label>
          <textarea
            id="description"
            rows={4}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            aria-invalid={!!errors.description}
            aria-describedby={errors.description ? 'description-error' : undefined}
          />
          {errors.description && <p id="description-error" className="field-error">{errors.description}</p>}
        </div>

        <div className="field">
          <label htmlFor="dueDate">Due date</label>
          <input
            id="dueDate"
            type="datetime-local"
            value={dueDate}
            onChange={(event) => setDueDate(event.target.value)}
            aria-invalid={!!errors.dueDate}
            aria-describedby={errors.dueDate ? 'dueDate-error' : undefined}
          />
          {errors.dueDate && <p id="dueDate-error" className="field-error">{errors.dueDate}</p>}
        </div>

        <p className="form-note">New tasks start as <strong>To-do</strong>. You can change the status once the task is created.</p>
      </form>
    </Modal>
  );
}
