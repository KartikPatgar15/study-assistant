import { useRef, useState, useCallback } from 'react';

const ACCEPTED = '.pdf,.docx,.txt,.png,.jpg,.jpeg';

/**
 * Drag-and-drop zone with a fallback browse button.
 *
 * Props:
 *   onFileSelected(file) – called with the File when the user picks one
 *   disabled             – true while an upload is in progress
 */
export default function UploadZone({ onFileSelected, disabled }) {
  const inputRef        = useRef(null);
  const [dragging, setDragging] = useState(false);

  const handleFiles = useCallback((files) => {
    if (!files || files.length === 0) return;
    if (files.length > 1) {
      // Parent validation will catch this but we only pass the first file
      // so the user gets a clear "one file" constraint message.
      onFileSelected(files[0]);
      return;
    }
    onFileSelected(files[0]);
  }, [onFileSelected]);

  // ── Drag handlers ─────────────────────────────────────────────────────────

  const onDragOver = useCallback((e) => {
    e.preventDefault();
    if (!disabled) setDragging(true);
  }, [disabled]);

  const onDragLeave = useCallback((e) => {
    e.preventDefault();
    setDragging(false);
  }, []);

  const onDrop = useCallback((e) => {
    e.preventDefault();
    setDragging(false);
    if (disabled) return;
    handleFiles(e.dataTransfer.files);
  }, [disabled, handleFiles]);

  // ── Input change ──────────────────────────────────────────────────────────

  const onInputChange = useCallback((e) => {
    handleFiles(e.target.files);
    // Reset input so the same file can be re-selected after removal
    e.target.value = '';
  }, [handleFiles]);

  return (
    <div
      role="button"
      tabIndex={disabled ? -1 : 0}
      aria-label="Upload area – drag and drop a file or press Enter to browse"
      onDragOver={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') inputRef.current?.click(); }}
      onClick={() => !disabled && inputRef.current?.click()}
      className={[
        'relative flex flex-col items-center justify-center gap-4',
        'rounded-2xl border-2 border-dashed px-8 py-14 text-center',
        'transition-colors duration-150 cursor-pointer select-none',
        dragging
          ? 'border-brand-500 bg-brand-50'
          : 'border-surface-border bg-surface-muted hover:border-brand-500 hover:bg-brand-50',
        disabled && 'pointer-events-none opacity-50',
      ].join(' ')}
    >
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED}
        className="sr-only"
        onChange={onInputChange}
        disabled={disabled}
        aria-hidden="true"
      />

      {/* Icon */}
      <div className={`w-14 h-14 rounded-2xl flex items-center justify-center transition-colors
        ${dragging ? 'bg-brand-500' : 'bg-white border border-surface-border'}`}>
        <UploadCloudIcon className={dragging ? 'text-white' : 'text-brand-500'} />
      </div>

      {/* Text */}
      <div className="space-y-1">
        <p className="font-semibold text-slate-700">
          {dragging ? 'Drop your file here' : 'Drag & drop your file here'}
        </p>
        <p className="text-sm text-slate-400">or</p>
        <button
          type="button"
          tabIndex={-1}  // parent div already handles focus/keyboard
          className="text-sm font-medium text-brand-500 hover:text-brand-600 underline underline-offset-2"
        >
          Browse files
        </button>
      </div>

      {/* Accepted types hint */}
      <p className="text-xs text-slate-400">
        PDF, DOCX, TXT, PNG, JPG · max 25 MB
      </p>
    </div>
  );
}

function UploadCloudIcon({ className }) {
  return (
    <svg className={`w-7 h-7 ${className}`} fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={1.7} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M12 16.5V9.75m0 0 3 3m-3-3-3 3M6.75 19.5a4.5 4.5 0 0 1-1.41-8.775
           5.25 5.25 0 0 1 10.233-2.33 3 3 0 0 1 3.758 3.848A3.752 3.752 0 0 1
           18 19.5H6.75z" />
    </svg>
  );
}
