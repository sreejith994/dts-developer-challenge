import { useCallback, useState } from 'react';
import { LayoutGrid, Plus, Table2 } from 'lucide-react';
import { TaskDetailsModal } from './components/TaskDetailsModal';
import { TaskFormModal } from './components/TaskFormModal';
import { TaskTable } from './components/TaskTable';

export default function App() {
  const [creating, setCreating] = useState(false);
  const [openTaskId, setOpenTaskId] = useState<number | null>(null);
  const [version, setVersion] = useState(0);
  const refresh = useCallback(() => setVersion((v) => v + 1), []);
  const closeDetails = useCallback(() => setOpenTaskId(null), []);
  const closeCreate = useCallback(() => setCreating(false), []);

  return (
    <div className="app">
      <header className="topbar">
        <h1 className="topbar-title">
          <LayoutGrid size={22} strokeWidth={1.75} aria-hidden="true" />
          Task views
        </h1>
        <button type="button" className="button" onClick={() => setCreating(true)}>
          <Plus size={16} /> New task
        </button>
      </header>

      <nav className="tabs" aria-label="Task views">
        <span className="tab tab--active" aria-current="page">
          <Table2 size={18} strokeWidth={1.75} aria-hidden="true" /> Table
        </span>
      </nav>

      <main className="content">
        <section className="card">
          <header className="card-header">
            <span className="project-mark" aria-hidden="true" />
            <h2>Caseworker tasks</h2>
          </header>
          <TaskTable version={version} onOpenTask={setOpenTaskId} onChanged={refresh} />
        </section>
      </main>

      {creating && (
        <TaskFormModal
          onClose={closeCreate}
          onCreated={() => {
            setCreating(false);
            refresh();
          }}
        />
      )}
      {openTaskId !== null && <TaskDetailsModal taskId={openTaskId} onClose={closeDetails} onChanged={refresh} />}
    </div>
  );
}
