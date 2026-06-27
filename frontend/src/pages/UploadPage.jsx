import { Link } from 'react-router-dom';
import { useUpload, formatBytes } from '../hooks/useUpload.js';
import UploadZone       from '../components/upload/UploadZone.jsx';
import SelectedFileCard from '../components/upload/SelectedFileCard.jsx';
import UploadProgress   from '../components/upload/UploadProgress.jsx';

/**
 * M02 Upload page – /upload
 *
 * Orchestrates the upload flow:
 *   idle → file selected → uploading → success | error
 *
 * All state lives in useUpload(); this component is purely presentational.
 */
export default function UploadPage() {
  const {
    selectedFile,
    status,
    progress,
    validationError,
    serverError,
    uploadResult,
    selectFile,
    clearFile,
    upload,
  } = useUpload();

  const isUploading = status === 'uploading';
  const isSuccess   = status === 'success';

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 flex items-start justify-center px-4 py-12">
        <div className="w-full max-w-xl space-y-6">

          {/* Page heading */}
          <div className="space-y-1">
            <h1 className="text-2xl font-bold tracking-tight text-slate-900">
              Upload your document
            </h1>
            <p className="text-sm text-slate-500">
              Select one file to get started. Supported: PDF, DOCX, TXT, PNG, JPG.
            </p>
          </div>

          {/* ── Success state ─────────────────────────────────────────────── */}
          {isSuccess && uploadResult ? (
            <SuccessPanel result={uploadResult} onUploadAnother={clearFile} />
          ) : (
            <>
              {/* ── Drop zone (hidden once a valid file is staged) ─────── */}
              {!selectedFile && (
                <UploadZone onFileSelected={selectFile} disabled={isUploading} />
              )}

              {/* ── Validation error ───────────────────────────────────── */}
              {validationError && (
                <ErrorBanner message={validationError} />
              )}

              {/* ── Staged file card ───────────────────────────────────── */}
              {selectedFile && (
                <SelectedFileCard
                  file={selectedFile}
                  onRemove={clearFile}
                  disabled={isUploading}
                />
              )}

              {/* ── Progress bar ───────────────────────────────────────── */}
              {isUploading && <UploadProgress progress={progress} />}

              {/* ── Server error ───────────────────────────────────────── */}
              {serverError && (
                <ErrorBanner message={serverError} />
              )}

              {/* ── Upload button ──────────────────────────────────────── */}
              {selectedFile && !isUploading && (
                <button
                  type="button"
                  onClick={upload}
                  className="w-full py-3 rounded-xl bg-brand-500 hover:bg-brand-600
                             text-white font-semibold text-sm transition-colors
                             focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
                >
                  Upload document
                </button>
              )}

              {/* ── Re-select hint after server error ─────────────────── */}
              {serverError && !isUploading && (
                <button
                  type="button"
                  onClick={clearFile}
                  className="w-full py-2.5 rounded-xl border border-surface-border
                             text-slate-600 text-sm font-medium hover:bg-slate-50 transition-colors"
                >
                  Choose a different file
                </button>
              )}
            </>
          )}
        </div>
      </main>

      <footer className="border-t border-surface-border py-5 text-center text-xs text-slate-400">
        AI Study Assistant · Module M02 · Document Upload
      </footer>
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
        <nav className="flex items-center gap-4 text-sm">
          <Link to="/"       className="text-slate-500 hover:text-slate-800 transition-colors">Home</Link>
          <Link to="/upload" className="text-brand-500 font-medium">Upload</Link>
        </nav>
      </div>
    </header>
  );
}

function SuccessPanel({ result, onUploadAnother }) {
  return (
    <div className="card px-6 py-8 text-center space-y-5">
      {/* Checkmark */}
      <div className="mx-auto w-14 h-14 rounded-full bg-emerald-50 border border-emerald-100
                      flex items-center justify-center">
        <CheckIcon />
      </div>

      <div className="space-y-1">
        <h2 className="text-lg font-semibold text-slate-800">Upload successful!</h2>
        <p className="text-sm text-slate-500">Your document has been received.</p>
      </div>

      {/* File details */}
      <div className="bg-slate-50 rounded-xl divide-y divide-surface-border text-left text-sm overflow-hidden">
        <DetailRow label="File name"   value={result.originalFileName} />
        <DetailRow label="File size"   value={formatBytes(result.fileSize)} />
        <DetailRow label="Upload ID"   value={result.uploadId} mono />
        <DetailRow label="Status"      value={result.status} success />
      </div>

      <button
        type="button"
        onClick={onUploadAnother}
        className="w-full py-2.5 rounded-xl border border-surface-border text-slate-600
                   text-sm font-medium hover:bg-slate-50 transition-colors"
      >
        Upload another document
      </button>
    </div>
  );
}

function DetailRow({ label, value, mono, success }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 gap-4">
      <span className="text-slate-500 shrink-0">{label}</span>
      <span className={[
        'truncate text-right',
        mono    && 'font-mono text-xs text-slate-600',
        success && 'font-medium text-emerald-600',
        !mono && !success && 'text-slate-800 font-medium',
      ].filter(Boolean).join(' ')}>
        {value}
      </span>
    </div>
  );
}

function ErrorBanner({ message }) {
  return (
    <div role="alert" className="flex items-start gap-3 rounded-xl border border-rose-200
                                  bg-rose-50 px-4 py-3.5">
      <AlertIcon />
      <p className="text-sm text-rose-700 leading-relaxed">{message}</p>
    </div>
  );
}

/* ── Icons ───────────────────────────────────────────────────────────────── */

function BookIcon() {
  return (
    <svg className="w-5 h-5 text-brand-500" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987
           8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292
           c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0
           00-6 2.292m0-14.25v14.25" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg className="w-7 h-7 text-emerald-500" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="m4.5 12.75 6 6 9-13.5" />
    </svg>
  );
}

function AlertIcon() {
  return (
    <svg className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0
           2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898
           0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
    </svg>
  );
}
