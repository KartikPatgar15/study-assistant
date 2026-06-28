/**
 * Displays knowledge build statistics inside the result panel.
 *
 * Rendered as a collapsible section within ProcessingResultPanel so the
 * existing layout is not disturbed. Only shown when the server returns
 * a non-null `processing.knowledge` object (PDF uploads only).
 *
 * Props:
 *   knowledge – the processing.knowledge object from the server response
 */
export default function KnowledgeResultPanel({ knowledge }) {
  if (!knowledge) return null;

  const rows = [
    { label: 'Total chunks',       value: knowledge.totalChunks,                highlight: true  },
    { label: 'Pages covered',      value: knowledge.pagesCovered,               highlight: true  },
    { label: 'Images linked',      value: knowledge.imagesLinked,               highlight: false },
    { label: 'Avg chunk size',     value: `${knowledge.averageChunkSize} words`, highlight: false },
    { label: 'Largest chunk',      value: `${knowledge.largestChunk} words`,     highlight: false },
    { label: 'Smallest chunk',     value: `${knowledge.smallestChunk} words`,    highlight: false },
    { label: 'Largest page span',  value: `${knowledge.largestPageSpan} pages`,  highlight: false },
    { label: 'Build duration',     value: `${knowledge.buildDurationMs} ms`,     highlight: false },
  ];

  return (
    <div className="space-y-1">
      {/* Section label */}
      <div className="flex items-center gap-2 px-1">
        <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
          Knowledge base
        </p>
        <span className="badge-indigo">Ready for AI</span>
      </div>

      {/* Stats grid */}
      <div className="bg-slate-50 rounded-xl divide-y divide-surface-border overflow-hidden text-sm">
        {rows.map(({ label, value, highlight }) => (
          <div key={label} className="flex items-center justify-between px-4 py-3 gap-4">
            <span className="text-slate-500 shrink-0">{label}</span>
            <span className={[
              'text-right',
              highlight
                ? 'font-bold text-brand-600 text-base'
                : 'font-medium text-slate-800',
            ].join(' ')}>
              {value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
