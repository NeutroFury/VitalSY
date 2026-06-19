/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // --- Paleta premium de verdes (VitalSY Health) ---
        text:       '#051F20', // Títulos, body copy, iconos activos
        primary:    '#235347', // Botones principales, enlaces, CTAs
        secondary:  '#163B32', // Texto secundario, bordes, badges
        surface:    '#8EB69B', // Fondos de tarjetas, inputs, contenedores
        background: '#DAF1DE', // Fondo general de la aplicación

        // Alias semánticos de salud
        'health-dark':    '#051F20',
        'health-accent':  '#235347',
        'health-muted':   '#163B32',
        'health-card':    '#8EB69B',
        'health-light':   '#DAF1DE',

        // Colores de contraste semántico (acciones y alertas médicas)
        'accent-primary': '#E76F51', // Terracota suave — CTAs principales
        'accent-alert':   '#D97706', // Ámbar intenso — Alertas médicas
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        heading: ['Plus Jakarta Sans', 'sans-serif'],
      },
      boxShadow: {
        'soft':   '0 2px 8px  rgba(5, 31, 32, 0.08)',
        'card':   '0 4px 12px rgba(5, 31, 32, 0.12)',
        'elevated': '0 8px 24px rgba(5, 31, 32, 0.16)',
      },
    },
  },
  plugins: [],
}
