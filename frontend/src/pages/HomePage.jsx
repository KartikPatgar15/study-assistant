import StatusBadge from '../components/StatusBadge.jsx';
import { useHealthCheck } from '../hooks/useHealthCheck.js';

/**
 * M01 landing page.
 *
 * Displays the project title, status, and a short overview of what's coming.
 * Future modules will replace the placeholder section with real functionality.
 */
export default function HomePage() {
  const { status } = useHealthCheck();

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 flex items-center justify-center px-4 py-20">
        <div className="w-full max-w-2xl space-y-10">
          <HeroSection status={status} />
          <StackSection />
          <ModuleStatusSection />
        </div>
      </main>

      <Footer />
    </div>
  );
}

/* ── Sub-components ──────────────────────────────────────────────────────── */

function Header() {
  return (
    <header className="border-b border-surface-border bg-white/80 backdrop-blur-sm sticky top-0 z-10">
      <div className="max-w-5xl mx-auto px-6 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <BookIcon />
          <span className="font-semibold text-slate-800 tracking-tight">StudyAssistant</span>
        </div>
        <span className="text-xs text-slate-400 font-mono">v0.1.0 · M01</span>
      </div>
    </header>
  );
}

function HeroSection({ status }) {
  return (
    <div className="text-center space-y-5">
      <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-brand-500 shadow-lg shadow-brand-500/25 mb-2">
        <SparkleIcon />
      </div>

      <div className="space-y-2">
        <h1 className="text-4xl font-bold tracking-tight text-slate-900">
          AI Study Assistant
        </h1>
        <p className="text-slate-500 text-lg">
          Project Foundation Ready
        </p>
      </div>

      <div className="flex justify-center">
        <StatusBadge status={status} />
      </div>
    </div>
  );
}

function StackSection() {
  const layers = [
    { label: 'Frontend',  value: 'React 18 · Vite · Tailwind CSS' },
    { label: 'Backend',   value: 'Java 21 · Spring Boot 3.x' },
    { label: 'Database',  value: 'PostgreSQL via Supabase' },
    { label: 'Storage',   value: 'Supabase Storage' },
    { label: 'AI',        value: 'Provider-independent (coming soon)' },
  ];

  return (
    <div className="card divide-y divide-surface-border overflow-hidden">
      {layers.map(({ label, value }) => (
        <div key={label} className="flex items-center justify-between px-5 py-3.5">
          <span className="text-sm font-medium text-slate-500 w-24 shrink-0">{label}</span>
          <span className="text-sm text-slate-800 font-mono text-right">{value}</span>
        </div>
      ))}
    </div>
  );
}

function ModuleStatusSection() {
  const modules = [
    { id: 'M01', name: 'Project Foundation',  done: true  },
    { id: 'M02', name: 'Document Upload',     done: false },
    { id: 'M03', name: 'AI Q&A',              done: false },
    { id: 'M04', name: 'Export & Admin',      done: false },
  ];

  return (
    <div className="card px-5 py-4 space-y-3">
      <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest">Module roadmap</p>
      <ul className="space-y-2.5">
        {modules.map(({ id, name, done }) => (
          <li key={id} className="flex items-center gap-3">
            <span
              className={`w-5 h-5 rounded-full flex items-center justify-center text-xs shrink-0 ${
                done
                  ? 'bg-emerald-500 text-white'
                  : 'border-2 border-slate-200 text-transparent'
              }`}
            >
              ✓
            </span>
            <span className="font-mono text-xs text-slate-400 w-8">{id}</span>
            <span className={`text-sm ${done ? 'text-slate-800 font-medium' : 'text-slate-400'}`}>
              {name}
            </span>
            {done && <span className="ml-auto badge-green">Complete</span>}
          </li>
        ))}
      </ul>
    </div>
  );
}

function Footer() {
  return (
    <footer className="border-t border-surface-border py-6 text-center text-xs text-slate-400">
      AI Study Assistant · Module M01 · Project Foundation
    </footer>
  );
}

/* ── Icons (inline SVG, no external dependency) ──────────────────────────── */

function BookIcon() {
  return (
    <svg className="w-5 h-5 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
    </svg>
  );
}

function SparkleIcon() {
  return (
    <svg className="w-8 h-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.6} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09z" />
    </svg>
  );
}
