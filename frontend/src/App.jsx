import { Routes, Route } from 'react-router-dom';
import HomePage    from './pages/HomePage.jsx';
import UploadPage  from './pages/UploadPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

/**
 * Application root.
 *
 * M02 adds the /upload route. All M01 routes are preserved unchanged.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/"       element={<HomePage />} />
      <Route path="/upload" element={<UploadPage />} />
      <Route path="*"       element={<NotFoundPage />} />
    </Routes>
  );
}
