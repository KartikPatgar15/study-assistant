import { Routes, Route } from 'react-router-dom';
import HomePage    from './pages/HomePage.jsx';
import UploadPage  from './pages/UploadPage.jsx';
import ChatPage    from './pages/ChatPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

/**
 * Application root.
 *
 * M04A adds the /chat/:uploadId route.
 * All M01–M03.5 routes are preserved unchanged.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/"                element={<HomePage />} />
      <Route path="/upload"          element={<UploadPage />} />
      <Route path="/chat/:uploadId"  element={<ChatPage />} />
      <Route path="*"                element={<NotFoundPage />} />
    </Routes>
  );
}
