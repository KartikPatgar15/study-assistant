/**
 * Visual indicator for the backend health status.
 */
export default function StatusBadge({ status }) {
  const variants = {
    loading: {
      className: 'badge bg-slate-100 text-slate-500',
      dot: 'bg-slate-400 animate-pulse',
      label: 'Connecting…',
    },
    up: {
      className: 'badge-green',
      dot: 'bg-emerald-500',
      label: 'Backend connected',
    },
    down: {
      className: 'badge bg-rose-50 text-rose-700',
      dot: 'bg-rose-500',
      label: 'Backend unavailable',
    },
  };

  const v = variants[status] ?? variants.loading;

  return (
    <span className={v.className}>
      <span className={`w-2 h-2 rounded-full ${v.dot}`} aria-hidden="true" />
      {v.label}
    </span>
  );
}
