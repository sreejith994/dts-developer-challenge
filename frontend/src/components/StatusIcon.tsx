import { Ban, Circle, CircleCheck, CircleDashed, OctagonAlert, type LucideProps } from 'lucide-react';
import type { TaskStatus } from '../api/types';

const ICONS = {
  CREATED: Circle,
  INPROGRESS: CircleDashed,
  BLOCKED: OctagonAlert,
  COMPLETED: CircleCheck,
  CANCELLED: Ban,
} satisfies Record<TaskStatus, unknown>;

export function StatusIcon({ status, ...props }: { status: TaskStatus } & LucideProps) {
  const Icon = ICONS[status];
  return <Icon size={18} strokeWidth={1.75} aria-hidden="true" {...props} />;
}
