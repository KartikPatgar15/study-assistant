import { useState, useCallback } from 'react';

const MAX_SIZE_BYTES = 25 * 1024 * 1024; // 25 MB
const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'txt', 'png', 'jpg', 'jpeg'];
const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
  'image/png',
  'image/jpeg',
];

/**
 * All upload state and behaviour in one place.
 *
 * Returns:
 *   selectedFile   – File | null
 *   status         – 'idle' | 'uploading' | 'success' | 'error'
 *   progress       – 0-100 (XHR upload progress)
 *   validationError – string | null  (client-side rejection reason)
 *   serverError    – string | null  (server rejection reason)
 *   uploadResult   – UploadResponse | null
 *   selectFile(file) – validate and stage a file
 *   clearFile()    – reset to idle
 *   upload()       – POST to /api/upload
 */
export function useUpload() {
  const [selectedFile,    setSelectedFile]    = useState(null);
  const [status,          setStatus]          = useState('idle');   // idle | uploading | success | error
  const [progress,        setProgress]        = useState(0);
  const [validationError, setValidationError] = useState(null);
  const [serverError,     setServerError]     = useState(null);
  const [uploadResult,    setUploadResult]    = useState(null);

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

    return null; // valid
  }, []);

  const selectFile = useCallback((file) => {
    // Reset previous state whenever a new file is chosen
    setServerError(null);
    setUploadResult(null);
    setProgress(0);
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

  const clearFile = useCallback(() => {
    setSelectedFile(null);
    setValidationError(null);
    setServerError(null);
    setUploadResult(null);
    setProgress(0);
    setStatus('idle');
  }, []);

  const upload = useCallback(() => {
    if (!selectedFile) return;

    setStatus('uploading');
    setProgress(0);
    setServerError(null);

    const formData = new FormData();
    formData.append('file', selectedFile);

    // Use XMLHttpRequest instead of fetch to get real upload progress events.
    const xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        setProgress(Math.round((e.loaded / e.total) * 100));
      }
    });

    xhr.addEventListener('load', () => {
      try {
        const body = JSON.parse(xhr.responseText);
        if (xhr.status >= 200 && xhr.status < 300) {
          setUploadResult(body);
          setStatus('success');
          setProgress(100);
        } else {
          setServerError(body.message ?? `Server error (${xhr.status}).`);
          setStatus('error');
        }
      } catch {
        setServerError('Unexpected response from server.');
        setStatus('error');
      }
    });

    xhr.addEventListener('error', () => {
      setServerError('Could not reach the server. Please check your connection.');
      setStatus('error');
    });

    xhr.addEventListener('timeout', () => {
      setServerError('The request timed out. Please try again.');
      setStatus('error');
    });

    xhr.timeout = 120_000; // 2 minutes
    xhr.open('POST', '/api/upload');
    xhr.send(formData);
  }, [selectedFile]);

  return {
    selectedFile,
    status,
    progress,
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
  const ext = filename?.split('.').pop()?.toUpperCase() ?? 'FILE';
  return ext;
}
