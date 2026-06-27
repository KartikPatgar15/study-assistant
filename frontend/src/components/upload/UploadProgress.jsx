/**
 * Upload progress bar shown while a file is being transmitted.
 *
 * Props:
 *   progress – number 0-100
 */
export default function UploadProgress({ progress }) {
  return (
    <div className="space-y-2" role="status" aria-live="polite">
      <div className="flex justify-between items-center">
        <span className="text-sm font-medium text-slate-600 flex items-center gap-2">
          <SpinnerIcon />
          Uploading…
        </span>
        <span className="text-sm font-mono text-slate-500">{progress}%</span>
      </div>

      {/* Track */}
      <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
        {/* Fill */}
        <div
          className="h-full rounded-full bg-brand-500 transition-all duration-200 ease-out"
          style={{ width: `${progress}%` }}
          aria-valuenow={progress}
          aria-valuemin={0}
          aria-valuemax={100}
          role="progressbar"
        />
      </div>
    </div>
  );
}

function SpinnerIcon() {
  return (
    <svg className="w-4 h-4 animate-spin text-brand-500" fill="none"
      viewBox="0 0 24 24" aria-hidden="true">
      <circle className="opacity-25" cx="12" cy="12" r="10"
        stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  );
}
