/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'neon-green': '#ccff00',
        'cyber-black': '#050505'
      },
      boxShadow: {
        'neon': '0 0 10px rgba(204, 255, 0, 0.5), 0 0 20px rgba(204, 255, 0, 0.3)',
      }
    },
  },
  plugins: [],
}
