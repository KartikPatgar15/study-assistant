import { Link } from 'react-router-dom';
import { useUpload } from '../hooks/useUpload.js';
import UploadZone            from '../components/upload/UploadZone.jsx';
import SelectedFileCard      from '../components/upload/SelectedFileCard.jsx';
import UploadProgress        from '../components/upload/UploadProgress.jsx';
import ProcessingStages      from '../components/processing/ProcessingStages.jsx';
import KnowledgeStages       from '../components/knowledge/KnowledgeStages.jsx';
import ProcessingResultPanel from '../components/processing/ProcessingResultPanel.jsx';

/**
 * Upload page – /upload
 *
 * M03.5 extends the pipeline with a 'knowledge' status phase. The page now
 * cycles through four distinct states visible to the user:
 *
 *   uploading   → bytes transferring      (UploadProgress)
 *   processing  → M03 PDF extraction      (ProcessingStages)
 *   knowledge   → M03.5 knowledge builder (KnowledgeStages)
 *   success     → full result panel       (ProcessingResultPanel)
 *
 * All state lives in useUpload(); this component remains purely presentational.
 */
export default function UploadPage() {
  const {
    selectedFile,
    status,
    progress,
    processingStage,
    validationError,
    serverError,
    uploadResult,
    selectFile,
    clearFile,
    upload,
  } = useUpload();

  const isUploading  = status === 'uploading';
  const isProcessing = status === 'processing';
  const isKnowledge  = status === 'knowledge';
  const isSuccess    = status === 'success';
  const isBusy       = isUploading || isProcessing || isKnowledge;

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

          {/* ── Success ──────────────────────────────────────────────────── */}
          {isSuccess && uploadResult ? (
            <ProcessingResultPanel result={uploadResult} onUploadAnother={clearFile} />
          ) : (
            <>
              {/* ── Drop zone ──────────────────────────────────────────── */}
              {!selectedFile && !isBusy && (
                <UploadZone onFileSelected={selectFile} disabled={isBusy} />
              )}

              {/* ── Validation error ───────────────────────────────────── */}
              {validationError && <ErrorBanner message={validationError} />}

              {/* ── Staged file card ───────────────────────────────────── */}
              {selectedFile && !isProcessing && !isKnowledge && (
                <SelectedFileCard
                  file={selectedFile}
                  onRemove={clearFile}
                  disabled={isBusy}
                />
              )}

              {/* ── Upload progress bar ────────────────────────────────── */}
              {isUploading && <UploadProgress progress={progress} />}

              {/* ── M03 processing stages ──────────────────────────────── */}
              {isProcessing && <ProcessingStages stage={processingStage} />}

              {/* ── M03.5 knowledge stages ─────────────────────────────── */}
              {isKnowledge && <KnowledgeStages stage={processingStage} />}

              {/* ── Server error ───────────────────────────────────────── */}
              {serverError && <ErrorBanner message={serverError} />}

              {/* ── Upload button ──────────────────────────────────────── */}
              {selectedFile && !isBusy && (
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

              {/* ── Re-select after server error ───────────────────────── */}
              {serverError && !isBusy && (
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
        AI Study Assistant · Module M03.5 · Knowledge Builder
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
