import { TASK_STATUSES, type TaskStatus } from '../api/types';
import { STATUS_LABELS } from '../format';

interface StatusSelectProps {
  status: TaskStatus;
  onChange: (status: TaskStatus) => void;
  disabled?: boolean;
  label: string;
}

export function StatusSelect({ status, onChange, disabled, label }: StatusSelectProps) {
  return (
    <span className={`status-pill status-${status.toLowerCase()} status-pill--select`}>
      <span className="status-dot" aria-hidden="true" />
      <select
        aria-label={label}
        value={status}
        disabled={disabled}
        onClick={(event) => event.stopPropagation()}
        onChange={(event) => onChange(event.target.value as TaskStatus)}
      >
        {TASK_STATUSES.map((value) => (
          <option key={value} value={value}>
            {STATUS_LABELS[value]}
          </option>
        ))}
      </select>
    </span>
  );
}
