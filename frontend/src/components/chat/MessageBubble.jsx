/**
 * Renders a single chat message bubble.
 *
 * Props:
 *   message – { role: 'user'|'assistant', content, model?, provider?, timestamp }
 */
export default function MessageBubble({ message }) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] space-y-1`}>
        {/* Bubble */}
        <div className={[
          'px-4 py-3 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap',
          isUser
            ? 'bg-brand-500 text-white rounded-tr-sm'
            : 'bg-white border border-surface-border text-slate-800 rounded-tl-sm shadow-sm',
        ].join(' ')}>
          {message.content}
        </div>

        {/* Meta row */}
        <div className={`flex items-center gap-2 px-1 ${isUser ? 'justify-end' : 'justify-start'}`}>
          {!isUser && message.model && (
            <span className="text-xs text-slate-400 font-mono">
              {message.provider} · {message.model}
            </span>
          )}
          <span className="text-xs text-slate-400">
            {formatTime(message.timestamp)}
          </span>
        </div>
      </div>
    </div>
  );
}

function formatTime(iso) {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch {
    return '';
  }
}
