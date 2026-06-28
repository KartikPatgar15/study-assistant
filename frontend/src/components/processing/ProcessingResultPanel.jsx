import { formatBytes } from '../../hooks/useUpload.js';
import KnowledgeResultPanel from '../knowledge/KnowledgeResultPanel.jsx';

/**
 * Shown after a successful upload + processing + knowledge build cycle.
 *
 * M03.5 adds a KnowledgeResultPanel section beneath the existing document
 * intelligence section. All previous sections are preserved unchanged.
 *
 * Props:
 *   result            – full server UploadResponse JSON
 *   onUploadAnother() – callback to reset the page
 */
export default function ProcessingResultPanel({ result, onUploadAnother }) {
  const p           = result.processing;   // null for non-PDF files
  const hasProcData = p != null;
  const status      = p?.status ?? 'SUCCESS';
  const statusMeta  = resolveStatus(status);

  return (
    <div className="card px-6 py-8 space-y-6">

      {/* ── Header ─────────────────────────────────────────────────────── */}
      <div className="text-center space-y-3">
        <div className={`mx-auto w-14 h-14 rounded-full flex items-center justify-center
                         ${statusMeta.ringClass}`}>
          {statusMeta.icon}
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-800">{statusMeta.title}</h2>
          <p className="text-sm text-slate-500 mt-0.5">
            {p?.message ?? 'Your file has been stored successfully.'}
          </p>
        </div>
      </div>

      {/* ── File details ───────────────────────────────────────────────── */}
      <Section title="File">
        <DetailRow label="Name"      value={result.originalFileName} />
        <DetailRow label="Size"      value={formatBytes(result.fileSize)} />
        <DetailRow label="Upload ID" value={result.uploadId} mono />
      </Section>

      {/* ── Document intelligence (PDF only) ───────────────────────────── */}
      {hasProcData && (
        <Section title="Document intelligence">
          <DetailRow label="Pages"    value={p.totalPages}  highlight />
          <DetailRow label="Images"   value={p.totalImages} highlight />
          <DetailRow label="Duration" value={`${p.processingDurationMs} ms`} />
          <DetailRow label="Status"   value={status} statusColor={statusMeta.textClass} />
        </Section>
      )}

      {/* ── Knowledge base (M03.5 – PDF only, non-null knowledge field) ── */}
      {hasProcData && p.knowledge && (
        <KnowledgeResultPanel knowledge={p.knowledge} />
      )}

      {/* ── Partial success warning ─────────────────────────────────────── */}
      {status === 'PARTIAL_SUCCESS' && (
        <div className="flex items-start gap-3 rounded-xl border border-amber-200
                        bg-amber-50 px-4 py-3">
          <WarnIcon />
          <p className="text-xs text-amber-700 leading-relaxed">
            Document was processed with warnings. Some pages or images may not have
            been fully extracted. Full details are in the processing report.
          </p>
        </div>
      )}

      {/* ── Failed banner ──────────────────────────────────────────────── */}
      {status === 'FAILED' && (
        <div className="flex items-start gap-3 rounded-xl border border-rose-200
                        bg-rose-50 px-4 py-3">
          <AlertIcon />
          <p className="text-xs text-rose-700 leading-relaxed">
            The file was stored but could not be processed (it may be encrypted or
            corrupted). You can still try uploading a different document.
          </p>
        </div>
      )}

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

/* ── Sub-components ──────────────────────────────────────────────────────── */

function Section({ title, children }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest px-1">
        {title}
      </p>
      <div className="bg-slate-50 rounded-xl divide-y divide-surface-border overflow-hidden text-sm">
        {children}
      </div>
    </div>
  );
}

function DetailRow({ label, value, mono, highlight, statusColor }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 gap-4">
      <span className="text-slate-500 shrink-0">{label}</span>
      <span className={[
        'text-right truncate',
        mono        && 'font-mono text-xs text-slate-600',
        highlight   && 'font-bold text-brand-600 text-base',
        statusColor && `font-medium ${statusColor}`,
        !mono && !highlight && !statusColor && 'text-slate-800 font-medium',
      ].filter(Boolean).join(' ')}>
        {value}
      </span>
    </div>
  );
}

/* ── Helpers ─────────────────────────────────────────────────────────────── */

function resolveStatus(status) {
  switch (status) {
    case 'PARTIAL_SUCCESS':
      return {
        title:     'Processed with warnings',
        ringClass: 'bg-amber-50 border border-amber-200',
        textClass: 'text-amber-600',
        icon:      <WarnIcon large />,
      };
    case 'FAILED':
      return {
        title:     'Processing failed',
        ringClass: 'bg-rose-50 border border-rose-100',
        textClass: 'text-rose-600',
        icon:      <AlertIcon large />,
      };
    default:
      return {
        title:     'Upload & processing complete',
        ringClass: 'bg-emerald-50 border border-emerald-100',
        textClass: 'text-emerald-600',
        icon:      <CheckIcon />,
      };
  }
}

/* ── Icons ───────────────────────────────────────────────────────────────── */

function CheckIcon() {
  return (
    <svg className="w-7 h-7 text-emerald-500" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="m4.5 12.75 6 6 9-13.5" />
    </svg>
  );
}

function WarnIcon({ large }) {
  return (
    <svg className={`${large ? 'w-7 h-7' : 'w-4 h-4'} text-amber-500 shrink-0`}
      fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0
           2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898
           0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
    </svg>
  );
}

function AlertIcon({ large }) {
  return (
    <svg className={`${large ? 'w-7 h-7' : 'w-4 h-4'} text-rose-500 shrink-0`}
      fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0
           2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898
           0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
    </svg>
  );
}
