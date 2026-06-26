/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      colors: {
        // Brand palette – used consistently throughout the app.
        // Defined here so future modules can reference them by name.
        brand: {
          50:  '#eef2ff',
          100: '#e0e7ff',
          500: '#6366f1',  // primary
          600: '#4f46e5',  // primary-dark
          700: '#4338ca',
        },
        surface: {
          DEFAULT: '#ffffff',
          muted:   '#f8fafc',
          border:  '#e2e8f0',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
    },
  },
  plugins: [],
};
