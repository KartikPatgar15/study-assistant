import { useEffect, useRef } from 'react';
import MessageBubble   from './MessageBubble.jsx';
import TypingIndicator from './TypingIndicator.jsx';

/**
 * Scrollable message list — the main body of the chat interface.
 *
 * Props:
 *   messages – array of message objects from useChat
 *   loading  – bool: show typing indicator when true
 */
export default function ChatWindow({ messages, loading }) {
  const bottomRef = useRef(null);

  // Auto-scroll to the latest message whenever messages or loading changes
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  return (
    <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
      {messages.length === 0 && !loading && (
        <EmptyState />
      )}

      {messages.map((msg, idx) => (
        <MessageBubble key={idx} message={msg} />
      ))}

      {loading && <TypingIndicator />}

      {/* Invisible anchor for auto-scroll */}
      <div ref={bottomRef} />
    </div>
  );
}

function EmptyState() {
  return (
    <div className="h-full flex flex-col items-center justify-center text-center
                    py-16 text-slate-400 space-y-3">
      <div className="w-14 h-14 rounded-2xl bg-brand-50 border border-brand-100
                      flex items-center justify-center">
        <ChatIcon />
      </div>
      <p className="text-sm font-medium text-slate-500">Ask anything about your document</p>
      <p className="text-xs">Questions are answered using only your uploaded study material.</p>
    </div>
  );
}

function ChatIcon() {
  return (
    <svg className="w-7 h-7 text-brand-500" fill="none" viewBox="0 0 24 24"
      stroke="currentColor" strokeWidth={1.7} aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round"
        d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227
           1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133
           a1.14 1.14 0 0 1 .865-.501 48.172 48.172 0 0 0 3.423-.379c1.584-.233
           2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228
           A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513
           C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
    </svg>
  );
}
