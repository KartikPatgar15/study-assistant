/**
 * Displays the current document processing stage with an animated indicator.
 *
 * Shown after bytes finish uploading while the server runs the PDF pipeline.
 *
 * Props:
 *   stage – string describing the current stage (e.g. "Extracting text…")
 */
export default function ProcessingStages({ stage }) {
  const stages = [
    'Processing document…',
    'Extracting text…',
    'Extracting images…',
    'Generating report…',
    'Completed',
  ];

  return (
    <div className="card px-5 py-5 space-y-4">
      {/* Current stage label */}
      <div className="flex items-center gap-3">
        {stage === 'Completed' ? <CheckCircleIcon /> : <SpinnerIcon />}
        <span className="text-sm font-semibold text-slate-700">{stage || 'Processing…'}</span>
      </div>

      {/* Stage pipeline */}
      <ol className="space-y-2">
        {stages.map((s) => {
          const currentIdx  = stages.indexOf(stage);
          const thisIdx     = stages.indexOf(s);
          const isDone      = thisIdx < currentIdx || stage === 'Completed';
          const isCurrent   = s === stage && stage !== 'Completed';

          return (
            <li key={s} className="flex items-center gap-3">
              {/* State dot */}
              <span className={[
                'w-2 h-2 rounded-full shrink-0 transition-colors duration-300',
                isDone    ? 'bg-emerald-500'               : '',
                isCurrent ? 'bg-brand-500 animate-pulse'   : '',
                !isDone && !isCurrent ? 'bg-slate-200'     : '',
              ].join(' ')} />
              <span className={[
                'text-sm transition-colors duration-300',
                isDone    ? 'text-emerald-600 font-medium' : '',
                isCurrent ? 'text-brand-600 font-medium'  : '',
                !isDone && !isCurrent ? 'text-slate-400'  : '',
              ].join(' ')}>
                {s}
              </span>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

function SpinnerIcon() {
  return (
    <svg className="w-5 h-5 animate-spin text-brand-500 shrink-0" fill="none"
      viewBox="0 0 24 24" aria-hidden="true">
      <circle className="opacity-25" cx="12" cy="12" r="10"
        stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  );
}

function CheckCircleIcon() {
  return (
    <svg className="w-5 h-5 text-emerald-500 shrink-0" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" />
    </svg>
  );
}
