import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

/**
 * Application root.
 *
 * Routes are defined here so future modules can register their own pages
 * (e.g. /upload, /session/:id, /admin) without touching the module boundary.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
