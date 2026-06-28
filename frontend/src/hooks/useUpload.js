import { useState, useCallback } from 'react';

const MAX_SIZE_BYTES = 25 * 1024 * 1024;
const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'txt', 'png', 'jpg', 'jpeg'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg',
];

/**
 * All upload + processing + knowledge build state and behaviour in one place.
 *
 * Status machine:
 *   idle → uploading → processing → knowledge → success | error
 *
 * M03.5 adds:
 *   'knowledge' status – knowledge builder pipeline running on the server
 *   Extended stage labels covering the full two-phase pipeline.
 *
 * Returns:
 *   selectedFile     – File | null
 *   status           – 'idle'|'uploading'|'processing'|'knowledge'|'success'|'error'
 *   progress         – 0-100 (XHR upload progress)
 *   processingStage  – human-readable current stage label
 *   validationError  – string | null
 *   serverError      – string | null
 *   uploadResult     – full server response | null
 *   selectFile(file) – validate and stage a file
 *   clearFile()      – reset to idle
 *   upload()         – POST to /api/upload
 */
export function useUpload() {
  const [selectedFile,    setSelectedFile]    = useState(null);
  const [status,          setStatus]          = useState('idle');
  const [progress,        setProgress]        = useState(0);
  const [processingStage, setProcessingStage] = useState('');
  const [validationError, setValidationError] = useState(null);
  const [serverError,     setServerError]     = useState(null);
  const [uploadResult,    setUploadResult]    = useState(null);

  // ── Validation ──────────────────────────────────────────────────────────────

  const validateFile = useCallback((file) => {
    if (!file) return 'No file selected.';
    if (file.size === 0) return 'The selected file is empty.';
    if (file.size > MAX_SIZE_BYTES) {
      return `File is too large (${formatBytes(file.size)}). Maximum size is 25 MB.`;
    }
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      return `File type ".${ext}" is not supported. Allowed types: ${ALLOWED_EXTENSIONS.join(', ')}.`;
    }
    if (file.type && !ALLOWED_MIME_TYPES.includes(file.type)) {
      return `File content type "${file.type}" is not supported.`;
    }
    return null;
  }, []);

  // ── File selection ──────────────────────────────────────────────────────────

  const selectFile = useCallback((file) => {
    setServerError(null);
    setUploadResult(null);
    setProgress(0);
    setProcessingStage('');
    setStatus('idle');
    const error = validateFile(file);
    if (error) {
      setValidationError(error);
      setSelectedFile(null);
    } else {
      setValidationError(null);
      setSelectedFile(file);
    }
  }, [validateFile]);

  // ── Clear ───────────────────────────────────────────────────────────────────

  const clearFile = useCallback(() => {
    setSelectedFile(null);
    setValidationError(null);
    setServerError(null);
    setUploadResult(null);
    setProgress(0);
    setProcessingStage('');
    setStatus('idle');
  }, []);

  // ── Upload ──────────────────────────────────────────────────────────────────

  const upload = useCallback(() => {
    if (!selectedFile) return;

    const isPdf = selectedFile.name.toLowerCase().endsWith('.pdf');

    setStatus('uploading');
    setProgress(0);
    setProcessingStage('');
    setServerError(null);

    const formData = new FormData();
    formData.append('file', selectedFile);

    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
      if (!e.lengthComputable) return;

      const pct = Math.round((e.loaded / e.total) * 100);
      setProgress(pct);

      // Once bytes are fully sent, switch to M03 processing stage labels.
      // The server is running extraction + knowledge build at this point.
      // Stage timings are approximate – the response resolves all of them.
      if (pct === 100 && isPdf) {
        // ── M03 stages ────────────────────────────────
        setStatus('processing');
        setProcessingStage('Processing document…');
        const t1 = setTimeout(() => setProcessingStage('Extracting text…'),    800);
        const t2 = setTimeout(() => setProcessingStage('Extracting images…'),  2000);
        const t3 = setTimeout(() => setProcessingStage('Generating report…'),  3500);
        // ── M03.5 stages ──────────────────────────────
        const t4 = setTimeout(() => {
          setStatus('knowledge');
          setProcessingStage('Creating chunks…');
        }, 5000);
        const t5 = setTimeout(() => setProcessingStage('Associating images…'),   6200);
        const t6 = setTimeout(() => setProcessingStage('Building knowledge base…'), 7500);

        // Store timeout IDs on the xhr so the load handler can cancel them
        xhr._stageTimers = [t1, t2, t3, t4, t5, t6];
      }
    });

    xhr.addEventListener('load', () => {
      // Cancel any remaining stage timers – the response is already here
      if (xhr._stageTimers) xhr._stageTimers.forEach(clearTimeout);

      try {
        const body = JSON.parse(xhr.responseText);
        if (xhr.status >= 200 && xhr.status < 300) {
          setProcessingStage('Knowledge ready');
          setUploadResult(body);
          setProgress(100);
          // Brief pause so the user sees "Knowledge ready" before success renders
          setTimeout(() => setStatus('success'), 600);
        } else {
          setServerError(body.message ?? `Server error (${xhr.status}).`);
          setStatus('error');
          setProcessingStage('');
        }
      } catch {
        setServerError('Unexpected response from server.');
        setStatus('error');
        setProcessingStage('');
      }
    });

    xhr.addEventListener('error', () => {
      if (xhr._stageTimers) xhr._stageTimers.forEach(clearTimeout);
      setServerError('Could not reach the server. Please check your connection.');
      setStatus('error');
      setProcessingStage('');
    });

    xhr.addEventListener('timeout', () => {
      if (xhr._stageTimers) xhr._stageTimers.forEach(clearTimeout);
      setServerError('The request timed out. Please try again.');
      setStatus('error');
      setProcessingStage('');
    });

    xhr.timeout = 180_000; // 3 minutes
    xhr.open('POST', '/api/upload');
    xhr.send(formData);
  }, [selectedFile]);

  return {
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
  };
}

// ── Helpers ───────────────────────────────────────────────────────────────────

export function formatBytes(bytes) {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
}

export function fileTypeLabel(filename) {
  return filename?.split('.').pop()?.toUpperCase() ?? 'FILE';
}
