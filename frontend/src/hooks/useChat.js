import { useState, useCallback } from 'react';

/**
 * Manages the chat session state for one document.
 *
 * State machine:
 *   idle → loading → idle (message appended) | error
 *
 * Returns:
 *   messages   – array of { role: 'user'|'assistant', content, timestamp }
 *   loading    – bool: true while waiting for the AI response
 *   error      – string | null
 *   sendMessage(uploadId, question) – POST to /api/chat and append both messages
 *   clearError()                    – dismiss the current error banner
 */
export function useChat() {
  const [messages, setMessages] = useState([]);
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState(null);

  const sendMessage = useCallback(async (uploadId, question) => {
    if (!question.trim()) return;

    // Optimistically append the user message
    const userMessage = {
      role:      'user',
      content:   question.trim(),
      timestamp: new Date().toISOString(),
    };
    setMessages(prev => [...prev, userMessage]);
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/chat', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ uploadId, question: question.trim() }),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message ?? `Request failed (${response.status})`);
      }

      const assistantMessage = {
        role:      'assistant',
        content:   data.answer,
        provider:  data.provider,
        model:     data.model,
        timestamp: data.timestamp,
      };
      setMessages(prev => [...prev, assistantMessage]);

    } catch (err) {
      setError(err.message ?? 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return { messages, loading, error, sendMessage, clearError };
}
