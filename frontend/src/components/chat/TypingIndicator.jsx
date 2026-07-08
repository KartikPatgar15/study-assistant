/**
 * Animated typing indicator shown while the AI response is loading.
 */
export default function TypingIndicator() {
  return (
    <div className="flex justify-start">
      <div className="bg-white border border-surface-border rounded-2xl rounded-tl-sm
                      px-4 py-3 shadow-sm flex items-center gap-1.5">
        <span className="w-2 h-2 rounded-full bg-slate-300 animate-bounce [animation-delay:-0.3s]" />
        <span className="w-2 h-2 rounded-full bg-slate-300 animate-bounce [animation-delay:-0.15s]" />
        <span className="w-2 h-2 rounded-full bg-slate-300 animate-bounce" />
      </div>
    </div>
  );
}
