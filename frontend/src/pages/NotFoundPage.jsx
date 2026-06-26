import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center text-center px-4 space-y-4">
      <p className="text-6xl font-bold text-slate-200">404</p>
      <h1 className="text-xl font-semibold text-slate-700">Page not found</h1>
      <p className="text-slate-400 text-sm">The page you're looking for doesn't exist yet.</p>
      <Link
        to="/"
        className="mt-2 px-4 py-2 rounded-lg bg-brand-500 text-white text-sm font-medium hover:bg-brand-600 transition-colors"
      >
        Back to home
      </Link>
    </div>
  );
}
