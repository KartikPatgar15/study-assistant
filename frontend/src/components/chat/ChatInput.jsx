import { useState, useRef } from 'react';

/**
 * Question input area at the bottom of the chat window.
 *
 * Props:
 *   onSend(question)  – called when the user submits a non-empty question
 *   disabled          – true while a response is loading
 */
export default function ChatInput({ onSend, disabled }) {
  const [value, setValue] = useState('');
  const textareaRef       = useRef(null);

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setValue('');
    textareaRef.current?.focus();
  };

  const handleKeyDown = (e) => {
    // Send on Enter (without Shift)
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="border-t border-surface-border bg-white px-4 py-3">
      <div className="flex items-end gap-3 max-w-3xl mx-auto">
        <textarea
          ref={textareaRef}
          value={value}
          onChange={e => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder="Ask a question about your document…"
          rows={1}
          className="flex-1 resize-none rounded-xl border border-surface-border bg-surface-muted
                     px-4 py-2.5 text-sm text-slate-800 placeholder-slate-400
                     focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent
                     disabled:opacity-50 transition-shadow"
          style={{ maxHeight: '120px', overflowY: 'auto' }}
        />
        <button
          type="button"
          onClick={handleSend}
          disabled={disabled || !value.trim()}
          aria-label="Send question"
          className="shrink-0 w-10 h-10 rounded-xl bg-brand-500 hover:bg-brand-600
                     disabled:opacity-40 disabled:cursor-not-allowed
                     flex items-center justify-center text-white transition-colors
                     focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2"
        >
          <SendIcon />
        </button>
      </div>
      <p className="text-center text-xs text-slate-400 mt-2">
        Press Enter to send · Shift+Enter for new line
      </p>
    </div>
  );
}

function SendIcon() {
  return (
    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={2} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M6 12 3.269 3.125A59.769 59.769 0 0 1 21.485 12
           7.372 7.372 0 0 1 3.27 20.875L5.999 12zm0 0h7.5" />
    </svg>
  );
}
