import { formatBytes, fileTypeLabel } from '../../hooks/useUpload.js';

/**
 * Displays metadata about the file the user has selected before uploading.
 *
 * Props:
 *   file       – File object
 *   onRemove() – callback to deselect the file
 *   disabled   – true while uploading
 */
export default function SelectedFileCard({ file, onRemove, disabled }) {
  if (!file) return null;

  const ext = fileTypeLabel(file.name);

  return (
    <div className="card flex items-center gap-4 px-5 py-4">
      {/* File type badge */}
      <div className="shrink-0 w-12 h-12 rounded-xl bg-brand-50 border border-brand-100
                      flex items-center justify-center">
        <span className="text-xs font-bold text-brand-600 tracking-wide">{ext}</span>
      </div>

      {/* File info */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-slate-800 truncate" title={file.name}>
          {file.name}
        </p>
        <p className="text-xs text-slate-400 mt-0.5">{formatBytes(file.size)}</p>
      </div>

      {/* Remove button */}
      {!disabled && (
        <button
          type="button"
          onClick={onRemove}
          aria-label="Remove selected file"
          className="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center
                     text-slate-400 hover:text-rose-500 hover:bg-rose-50 transition-colors"
        >
          <XIcon />
        </button>
      )}
    </div>
  );
}

function XIcon() {
  return (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
    </svg>
  );
}
