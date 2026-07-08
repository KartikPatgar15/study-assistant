import { Link, useParams, useLocation } from 'react-router-dom';
import { useChat } from '../hooks/useChat.js';
import ChatWindow from '../components/chat/ChatWindow.jsx';
import ChatInput  from '../components/chat/ChatInput.jsx';

/**
 * Chat page – /chat/:uploadId
 *
 * Receives the uploadId from the URL parameter.
 * The document name can optionally be passed via location state
 * (set by ProcessingResultPanel's navigation link) for display purposes.
 *
 * Layout:
 *   sticky header
 *   ─────────────
 *   scrollable ChatWindow (flex-1)
 *   ─────────────
 *   sticky ChatInput
 */
export default function ChatPage() {
  const { uploadId }               = useParams();
  const location                   = useLocation();
  const documentName               = location.state?.documentName ?? 'Your document';
  const { messages, loading, error, sendMessage, clearError } = useChat();

  const handleSend = (question) => {
    sendMessage(uploadId, question);
  };

  return (
    <div className="min-h-screen flex flex-col bg-surface-muted">
      <Header documentName={documentName} uploadId={uploadId} />

      {/* Error banner */}
      {error && (
        <div role="alert" className="mx-4 mt-3 flex items-start gap-3 rounded-xl
                                      border border-rose-200 bg-rose-50 px-4 py-3">
          <AlertIcon />
          <p className="flex-1 text-sm text-rose-700">{error}</p>
          <button onClick={clearError} aria-label="Dismiss error"
            className="text-rose-400 hover:text-rose-600 shrink-0">
            <XIcon />
          </button>
        </div>
      )}

      {/* Chat body */}
      <div className="flex-1 flex flex-col max-w-3xl w-full mx-auto">
        <ChatWindow messages={messages} loading={loading} />
        <ChatInput  onSend={handleSend} disabled={loading} />
      </div>

      <footer className="py-3 text-center text-xs text-slate-400 border-t border-surface-border bg-white">
        AI Study Assistant · Module M04A · AI Q&amp;A
      </footer>
    </div>
  );
}

/* ── Sub-components ──────────────────────────────────────────────────────── */

function Header({ documentName, uploadId }) {
  return (
    <header className="border-b border-surface-border bg-white/90 backdrop-blur-sm sticky top-0 z-10">
      <div className="max-w-3xl mx-auto px-4 h-14 flex items-center gap-3">
        {/* Back to upload */}
        <Link to="/upload"
          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          aria-label="Back to upload">
          <ChevronLeftIcon />
        </Link>

        {/* Document context */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-slate-800 truncate">{documentName}</p>
          <p className="text-xs text-slate-400 font-mono truncate">ID: {uploadId}</p>
        </div>

        {/* Nav */}
        <nav className="flex items-center gap-3 text-sm shrink-0">
          <Link to="/" className="text-slate-400 hover:text-slate-700 transition-colors">Home</Link>
        </nav>
      </div>
    </header>
  );
}

/* ── Icons ───────────────────────────────────────────────────────────────── */

function ChevronLeftIcon() {
  return (
    <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5 8.25 12l7.5-7.5" />
    </svg>
  );
}

function AlertIcon() {
  return (
    <svg className="w-4 h-4 text-rose-500 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={1.8} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0
           2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898
           0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
    </svg>
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
